package br.com.saborecia.vistoria.dominio;

/**
 * Dados de um item de vistoria, exatamente como a IA (ou o extrator por Regex) devolve em JSON.
 * <p>
 * Todos os campos são texto/objetos "crus" e podem vir {@code null}: a IA foi instruída a
 * NÃO inventar informação. Quem interpreta e valida é o {@link br.com.saborecia.vistoria.dominio.regras.MotorDeRegras}.
 */
public record ItemVistoria(
        String tipoItem,             // EXTINTOR, SAIDA_EMERGENCIA, PISO_ANTIDERRAPANTE...
        String unidade,              // nome da filial, se aparecer no documento
        String enderecoUnidade,
        String tecnicoResponsavel,
        String dataVistoria,         // texto como aparece (dd/MM/yyyy, yyyy-MM-dd...)
        Boolean itemPresente,        // o item aparece na imagem?
        String condicaoVisual,       // OK, DANIFICADO, OBSTRUIDO, VENCIDO, NAO_IDENTIFICADO
        Boolean seloValidadePresente,
        String dataValidade,         // texto como aparece no selo/etiqueta
        String localizacao,          // COZINHA, SALAO, SAIDA, ESTOQUE...
        String classificacaoRisco,   // INCENDIO, ESCORREGAMENTO, ELETRICO...
        String normaAssociada,       // NR-23, NR-12...
        String grauUrgencia,
        Double confianca             // 0..1, autoavaliação da IA
) {

    public static ItemVistoria vazio() {
        return new ItemVistoria(null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    /**
     * Completa campos que a IA não encontrou com o que o técnico já informou no formulário
     * (unidade escolhida, nome do técnico, data de hoje). Nunca sobrescreve o que veio da imagem.
     */
    public ItemVistoria completarCom(String unidadeCadastro, String tecnico, String dataHoje, String local) {
        return new ItemVistoria(
                tipoItem,
                unidade,               // mantido como veio: comparado com o cadastro pela RegraUnidadeConfere
                enderecoUnidade,
                tecnicoResponsavel != null ? tecnicoResponsavel : tecnico,
                dataVistoria != null ? dataVistoria : dataHoje,
                itemPresente,
                condicaoVisual,
                seloValidadePresente,
                dataValidade,
                localizacao != null ? localizacao : local,
                classificacaoRisco,
                normaAssociada,
                grauUrgencia,
                confianca);
    }
}
