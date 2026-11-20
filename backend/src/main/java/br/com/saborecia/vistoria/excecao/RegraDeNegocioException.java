package br.com.saborecia.vistoria.excecao;

/** Operação inválida para o estado atual (ex.: revisar um item que não está em revisão). HTTP 422. */
public class RegraDeNegocioException extends RuntimeException {
    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
