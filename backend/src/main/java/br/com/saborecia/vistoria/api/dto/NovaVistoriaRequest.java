package br.com.saborecia.vistoria.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NovaVistoriaRequest(
        @NotNull(message = "informe a unidade") Long unidadeId,
        @NotBlank(message = "informe o técnico responsável") @Size(max = 120) String tecnico
) {}
