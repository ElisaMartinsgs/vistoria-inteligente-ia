package br.com.saborecia.vistoria.infra.ia;

/**
 * Porta de saída para o modelo de linguagem. Trocar de provedor (OpenAI, Gemini, Groq, Ollama, Claude...)
 * não afeta o restante do sistema.
 */
public interface LlmClient {

    /**
     * @param sistema     instruções fixas (papel, regras, formato do JSON)
     * @param usuario     contexto + texto do OCR
     * @param imagemJpeg  foto para modelos multimodais; null para enviar só texto
     * @throws br.com.saborecia.vistoria.excecao.IaIndisponivelException  timeout, 429, 5xx, rede
     * @throws br.com.saborecia.vistoria.excecao.IaConfiguracaoException 400/401/403/404, chave ausente
     */
    RespostaLlm completar(String sistema, String usuario, byte[] imagemJpeg);

    String nomeProvedor();

    String modelo();

    record RespostaLlm(String conteudo, String modelo, Integer tokensEntrada, Integer tokensSaida,
                       long latenciaMs, int tentativas) {}
}
