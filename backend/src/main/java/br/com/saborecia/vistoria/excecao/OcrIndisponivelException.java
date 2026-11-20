package br.com.saborecia.vistoria.excecao;

/** Tesseract não instalado, sem o idioma "por", ou estourou o tempo limite. */
public class OcrIndisponivelException extends RuntimeException {
    public OcrIndisponivelException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
