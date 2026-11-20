package br.com.saborecia.vistoria.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Todas as configurações do projeto ficam em application.yml sob o prefixo "vistoria".
 */
@ConfigurationProperties(prefix = "vistoria")
public record VistoriaProperties(
        /** Data fixa para demonstração/testes (yyyy-MM-dd). Vazio = data de hoje. */
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataReferencia,
        Armazenamento armazenamento,
        Imagem imagem,
        Ocr ocr,
        Ia ia,
        Regras regras
) {
    public record Armazenamento(String diretorio) {}

    public record Imagem(int larguraPadrao, double nitidezMinima, double brilhoMinimo,
                         double brilhoMaximo, double contrasteMinimo, boolean corrigirPerspectiva) {}

    public record Ocr(String tessdata, String idioma, int psm, int timeoutSegundos, int maxParalelo) {}

    public record Ia(
            /** "llm" (OpenAI, Gemini, Groq, Ollama... qualquer API compatível com OpenAI) ou "regex" (sem IA). */
            String provedor,
            String baseUrl,
            String apiKey,
            String modelo,
            double temperatura,
            int timeoutSegundos,
            int tentativas,
            boolean forcarJson,
            /** Envia também a foto ao modelo (modo multimodal). */
            boolean enviarImagem,
            double confiancaMinima,
            String prompt) {

        public boolean usaLlm() {
            return !"regex".equalsIgnoreCase(provedor);
        }
    }

    public record Regras(int diasAlertaVencimento) {}
}
