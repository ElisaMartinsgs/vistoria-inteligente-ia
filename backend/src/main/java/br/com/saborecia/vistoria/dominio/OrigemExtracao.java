package br.com.saborecia.vistoria.dominio;

public enum OrigemExtracao {
    /** JSON produzido por um LLM a partir do texto do OCR (e opcionalmente da imagem). */
    LLM,
    /** Modo sem IA: palavras-chave + Regex sobre o texto do OCR (escolhido na configuração). */
    REGEX,
    /** O LLM falhou (timeout, indisponível, JSON inválido) e o sistema caiu no Regex. */
    REGEX_FALLBACK,
    /** Nada foi extraído porque a imagem foi reprovada antes (ilegível). */
    NENHUMA
}
