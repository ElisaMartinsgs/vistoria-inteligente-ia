package br.com.saborecia.vistoria.servico;

import br.com.saborecia.vistoria.config.VistoriaProperties;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.OrigemExtracao;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.extracao.LlmResponseParser;
import br.com.saborecia.vistoria.dominio.extracao.PromptBuilder;
import br.com.saborecia.vistoria.dominio.extracao.RegexExtractor;
import br.com.saborecia.vistoria.dominio.extracao.RespostaIaInvalidaException;
import br.com.saborecia.vistoria.excecao.IaConfiguracaoException;
import br.com.saborecia.vistoria.excecao.IaIndisponivelException;
import br.com.saborecia.vistoria.infra.ia.LlmClient;
import br.com.saborecia.vistoria.persistencia.entidade.ChamadaIa;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Transforma o texto do OCR em dados estruturados ({@link ItemVistoria}).
 * <pre>
 *   provedor=regex → RegexExtractor (sem IA)
 *   provedor=llm   → LLM → parser JSON
 *                     └─ falhou (timeout, 429, 5xx, JSON inválido, chave errada) → RegexExtractor (REGEX_FALLBACK)
 * </pre>
 * O usuário nunca recebe erro 500 por causa da IA: no pior caso o item vai para revisão manual.
 */
@Service
public class ExtracaoService {

    private static final Logger log = LoggerFactory.getLogger(ExtracaoService.class);

    private final VistoriaProperties.Ia cfg;
    private final LlmClient llm;
    private final PromptBuilder prompts;
    private final LlmResponseParser parser;
    private final RegexExtractor regex;

    public ExtracaoService(VistoriaProperties props, LlmClient llm, PromptBuilder prompts,
                           LlmResponseParser parser, RegexExtractor regex) {
        this.cfg = props.ia();
        this.llm = llm;
        this.prompts = prompts;
        this.parser = parser;
        this.regex = regex;
    }

    /** @param chamada auditoria da chamada ao LLM (null quando não houve chamada) */
    public record Resultado(ItemVistoria item, OrigemExtracao origem, ChamadaIa chamada) {}

    public Resultado extrair(String textoOcr, byte[] imagemJpeg, TipoItem tipoEsperado, String local,
                             String unidade, LocalDate hoje) {
        if (!cfg.usaLlm()) {
            return new Resultado(regex.extrair(textoOcr, tipoEsperado), OrigemExtracao.REGEX, null);
        }

        String sistema = prompts.sistema();
        String usuario = prompts.usuario(textoOcr, tipoEsperado, local, unidade, hoje.toString());
        long inicio = System.currentTimeMillis();
        LlmClient.RespostaLlm resposta = null;
        try {
            resposta = llm.completar(sistema, usuario, imagemJpeg);
            ItemVistoria item = parser.parse(resposta.conteudo());
            ChamadaIa chamada = new ChamadaIa(llm.nomeProvedor(), resposta.modelo(), PromptBuilder.VERSAO, true, null,
                    resposta.tentativas(), resposta.latenciaMs(), resposta.tokensEntrada(), resposta.tokensSaida(),
                    resposta.conteudo());
            return new Resultado(item, OrigemExtracao.LLM, chamada);

        } catch (IaIndisponivelException | IaConfiguracaoException | RespostaIaInvalidaException e) {
            log.warn("Extração por LLM falhou ({}): {}. Usando fallback por Regex.", e.getClass().getSimpleName(), e.getMessage());
            int tentativas = e instanceof IaIndisponivelException ind ? ind.getTentativas()
                    : resposta != null ? resposta.tentativas() : 1;
            ChamadaIa chamada = new ChamadaIa(llm.nomeProvedor(), llm.modelo(), PromptBuilder.VERSAO, false,
                    e.getClass().getSimpleName() + ": " + e.getMessage(), tentativas,
                    System.currentTimeMillis() - inicio,
                    resposta != null ? resposta.tokensEntrada() : null,
                    resposta != null ? resposta.tokensSaida() : null,
                    resposta != null ? resposta.conteudo() : null);
            return new Resultado(regex.extrair(textoOcr, tipoEsperado), OrigemExtracao.REGEX_FALLBACK, chamada);
        }
    }
}
