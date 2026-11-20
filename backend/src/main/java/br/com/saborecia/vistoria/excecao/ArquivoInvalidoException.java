package br.com.saborecia.vistoria.excecao;

/** Upload vazio, formato não suportado, arquivo corrompido. HTTP 400. */
public class ArquivoInvalidoException extends RuntimeException {
    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
