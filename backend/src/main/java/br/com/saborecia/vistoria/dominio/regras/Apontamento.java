package br.com.saborecia.vistoria.dominio.regras;

/**
 * Resultado negativo (ou de atenção) de uma regra.
 *
 * @param codigoRegra     ex.: "VALIDADE"
 * @param severidade      REPROVA, REVISAO ou ALERTA
 * @param detalheTecnico  texto para o supervisor/compliance
 * @param mensagemSimples texto em linguagem simples para o técnico/gerente
 */
public record Apontamento(String codigoRegra, Severidade severidade, String detalheTecnico, String mensagemSimples) {

    public enum Severidade {
        /** Reprova o item (NÃO CONFORME). */
        REPROVA,
        /** Não dá para decidir com segurança: vai para revisão de um supervisor. */
        REVISAO,
        /** Não muda o status, mas aparece no relatório (ex.: vence em 15 dias). */
        ALERTA
    }

    public static Apontamento reprova(String regra, String tecnico, String simples) {
        return new Apontamento(regra, Severidade.REPROVA, tecnico, simples);
    }

    public static Apontamento revisao(String regra, String tecnico, String simples) {
        return new Apontamento(regra, Severidade.REVISAO, tecnico, simples);
    }

    public static Apontamento alerta(String regra, String tecnico, String simples) {
        return new Apontamento(regra, Severidade.ALERTA, tecnico, simples);
    }
}
