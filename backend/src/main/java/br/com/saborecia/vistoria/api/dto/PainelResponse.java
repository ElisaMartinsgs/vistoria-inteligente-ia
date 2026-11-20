package br.com.saborecia.vistoria.api.dto;

import java.util.List;

/** Visão do compliance/RH: volume, taxa de conformidade e principais motivos de reprovação. */
public record PainelResponse(
        long totalItens,
        long conformes,
        long naoConformes,
        long emRevisao,
        double taxaConformidade,
        List<MotivoFrequente> principaisMotivos,
        List<PorUnidade> porUnidade
) {
    public record MotivoFrequente(String regra, long quantidade) {}

    public record PorUnidade(String unidade, long total, long conformes, double taxaConformidade) {}
}
