package br.com.saborecia.vistoria.infra.ia;

import br.com.saborecia.vistoria.config.VistoriaProperties;
import br.com.saborecia.vistoria.dominio.extracao.RespostaIaInvalidaException;
import br.com.saborecia.vistoria.excecao.IaConfiguracaoException;
import br.com.saborecia.vistoria.excecao.IaIndisponivelException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Etapa 4 do pipeline — IA (LLM): "interpreta o texto e devolve um JSON estruturado".
 * <p>
 * Fala o protocolo /chat/completions da OpenAI, que também é aceito por:
 * Google Gemini (endpoint OpenAI-compatible), Groq, OpenRouter, Ollama (local, gratuito) e outros.
 * <p>
 * Tratamento de falhas:
 * <ul>
 *   <li>timeout de conexão e de leitura configuráveis;</li>
 *   <li>novas tentativas com espera exponencial para 429 (limite de uso), 5xx e erros de rede;</li>
 *   <li>sem nova tentativa para 400/401/403/404 (erro de configuração);</li>
 *   <li>resposta cortada (finish_reason=length) ou vazia → {@link RespostaIaInvalidaException}.</li>
 * </ul>
 */
@Component
public class OpenAiCompatibleClient implements LlmClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    private final VistoriaProperties.Ia cfg;
    private final ObjectMapper mapper;
    private final RestClient http;

    public OpenAiCompatibleClient(VistoriaProperties props, ObjectMapper mapper) {
        this.cfg = props.ia();
        this.mapper = mapper;
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(Duration.ofSeconds(5));
        f.setReadTimeout(Duration.ofSeconds(cfg.timeoutSegundos()));
        RestClient.Builder b = RestClient.builder().baseUrl(cfg.baseUrl()).requestFactory(f);
        if (cfg.apiKey() != null && !cfg.apiKey().isBlank()) {
            b.defaultHeader("Authorization", "Bearer " + cfg.apiKey());
        }
        this.http = b.build();
        if (cfg.usaLlm() && (cfg.apiKey() == null || cfg.apiKey().isBlank()) && !cfg.baseUrl().contains("localhost")
                && !cfg.baseUrl().contains("ollama")) {
            log.warn("vistoria.ia.api-key não configurada: as chamadas ao LLM vão falhar e cair no fallback por Regex. "
                    + "Defina LLM_API_KEY ou use IA_PROVEDOR=regex.");
        }
    }

    @Override
    public String nomeProvedor() {
        return cfg.baseUrl();
    }

    @Override
    public String modelo() {
        return cfg.modelo();
    }

    @Override
    public RespostaLlm completar(String sistema, String usuario, byte[] imagemJpeg) {
        Map<String, Object> corpo = montarCorpo(sistema, usuario, imagemJpeg);
        long inicio = System.currentTimeMillis();
        int maxTentativas = Math.max(1, cfg.tentativas());
        RuntimeException ultimoErro = null;

        for (int tentativa = 1; tentativa <= maxTentativas; tentativa++) {
            try {
                String bruto = http.post().uri("/chat/completions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(corpo)
                        .retrieve()
                        .body(String.class);
                return interpretar(bruto, System.currentTimeMillis() - inicio, tentativa);
            } catch (RestClientResponseException e) {
                int status = e.getStatusCode().value();
                if (status == 400 || status == 401 || status == 403 || status == 404) {
                    throw new IaConfiguracaoException("LLM respondeu HTTP " + status + ": " + resumir(e.getResponseBodyAsString()));
                }
                ultimoErro = e; // 429 ou 5xx: tenta de novo
                log.warn("LLM HTTP {} (tentativa {}/{})", status, tentativa, maxTentativas);
            } catch (ResourceAccessException e) {
                ultimoErro = e; // timeout, conexão recusada, DNS
                log.warn("LLM inacessível: {} (tentativa {}/{})", e.getMessage(), tentativa, maxTentativas);
            }
            if (tentativa < maxTentativas) esperar(tentativa);
        }
        throw new IaIndisponivelException("LLM indisponível após " + maxTentativas + " tentativa(s): "
                + (ultimoErro != null ? ultimoErro.getMessage() : "erro desconhecido"), maxTentativas, ultimoErro);
    }

    private Map<String, Object> montarCorpo(String sistema, String usuario, byte[] imagemJpeg) {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("model", cfg.modelo());
        corpo.put("temperature", cfg.temperatura());
        if (cfg.forcarJson()) corpo.put("response_format", Map.of("type", "json_object"));

        List<Map<String, Object>> msgs = new ArrayList<>();
        msgs.add(Map.of("role", "system", "content", sistema));
        if (imagemJpeg != null && cfg.enviarImagem()) {
            String dataUrl = "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(imagemJpeg);
            msgs.add(Map.of("role", "user", "content", List.of(
                    Map.of("type", "text", "text", usuario),
                    Map.of("type", "image_url", "image_url", Map.of("url", dataUrl)))));
        } else {
            msgs.add(Map.of("role", "user", "content", usuario));
        }
        corpo.put("messages", msgs);
        return corpo;
    }

    private RespostaLlm interpretar(String bruto, long latencia, int tentativas) {
        JsonNode raiz;
        try {
            raiz = mapper.readTree(bruto);
        } catch (Exception e) {
            throw new RespostaIaInvalidaException("Resposta HTTP do LLM não é JSON: " + resumir(bruto));
        }
        JsonNode escolha = raiz.path("choices").path(0);
        String conteudo = escolha.path("message").path("content").asText(null);
        if ("length".equals(escolha.path("finish_reason").asText())) {
            throw new RespostaIaInvalidaException("Resposta do LLM foi cortada (limite de tokens)");
        }
        if (conteudo == null || conteudo.isBlank()) {
            throw new RespostaIaInvalidaException("LLM devolveu conteúdo vazio");
        }
        JsonNode uso = raiz.path("usage");
        Integer in = uso.has("prompt_tokens") ? uso.get("prompt_tokens").asInt() : null;
        Integer out = uso.has("completion_tokens") ? uso.get("completion_tokens").asInt() : null;
        return new RespostaLlm(conteudo, raiz.path("model").asText(cfg.modelo()), in, out, latencia, tentativas);
    }

    private static void esperar(int tentativa) {
        try {
            Thread.sleep(1000L * (1L << (tentativa - 1))); // 1s, 2s, 4s...
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static String resumir(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
