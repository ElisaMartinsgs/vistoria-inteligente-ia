package br.com.saborecia.vistoria.api.dto;

import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.Urgencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record VistoriaResponse(
        Long id,
        Long unidadeId,
        String unidade,
        String tecnico,
        LocalDate dataVistoria,
        StatusItem statusGeral,
        int totalItens,
        long conformes,
        long naoConformes,
        long emRevisao,
        List<ItemResumo> itens
) {
    public record ItemResumo(Long id, TipoItem tipoItem, TipoItem tipoEsperado, String localizacao,
                             StatusItem status, Urgencia urgencia, String resumo, LocalDateTime criadoEm) {}
}
