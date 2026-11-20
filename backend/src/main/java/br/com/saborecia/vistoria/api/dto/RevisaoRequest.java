package br.com.saborecia.vistoria.api.dto;

import br.com.saborecia.vistoria.dominio.StatusItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Decisão do supervisor sobre um item em REVISAO_MANUAL (ou contestação de um parecer). */
public record RevisaoRequest(
        @NotNull(message = "informe o status final (CONFORME ou NAO_CONFORME)") StatusItem status,
        @NotBlank(message = "informe o nome do supervisor") @Size(max = 120) String supervisor,
        @Size(max = 1000) String observacao
) {}
