package br.com.saborecia.vistoria.excecao;

/** Unidade, vistoria ou item inexistente. HTTP 404. */
public class RecursoNaoEncontradoException extends RuntimeException {
    public RecursoNaoEncontradoException(String recurso, Object id) {
        super(recurso + " não encontrado(a): " + id);
    }
}
