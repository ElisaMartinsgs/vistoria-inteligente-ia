package br.com.saborecia.vistoria.api.dto;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.OrigemExtracao;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.Urgencia;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Resposta completa de POST /api/vistorias/validar e GET /api/itens/{id}. */
public record ValidacaoItemResponse(
        Long itemId,
        Long vistoriaId,
        String unidade,
        StatusItem status,
        StatusItem statusAutomatico,
        Urgencia urgencia,
        String resumo,
        List<MotivoResponse> motivos,
        TipoItem tipoEsperado,
        TipoItem tipoItem,
        String localizacao,
        LocalDate dataValidade,
        ClassificacaoRisco classificacaoRisco,
        String norma,
        Pipeline pipeline,
        Revisao revisao,
        LocalDateTime criadoEm
) {
    public record MotivoResponse(String regra, String severidade, String mensagem, String detalheTecnico) {}

    /** O que cada etapa produziu: útil para depurar e para a apresentação. */
    public record Pipeline(
            Double nitidez, Double brilho, Double contraste,
            String textoOcr, Double confiancaOcr,
            OrigemExtracao origemExtracao, ItemVistoria dadosExtraidos, Double confiancaExtracao,
            List<String> regrasAplicadas,
            long tempoProcessamentoMs) {}

    public record Revisao(String supervisor, String observacao, LocalDateTime em) {}
}
