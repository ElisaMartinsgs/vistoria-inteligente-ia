package br.com.saborecia.vistoria.dominio.extracao;

/** A IA respondeu, mas a resposta não pode ser usada (JSON malformado, incompleto, vazio). */
public class RespostaIaInvalidaException extends RuntimeException {
    public RespostaIaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
