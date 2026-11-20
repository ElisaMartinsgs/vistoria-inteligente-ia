package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.Urgencia;

import java.time.LocalDate;
import java.util.List;

/**
 * Parecer final do motor de regras para um item.
 */
public record ResultadoValidacao(
        StatusItem status,
        TipoItem tipoItem,
        ClassificacaoRisco risco,
        String norma,
        LocalDate dataValidade,
        Urgencia urgencia,
        List<String> regrasAplicadas,
        List<Apontamento> apontamentos,
        String resumo
) {
    public List<Apontamento> bloqueantes() {
        return apontamentos.stream().filter(a -> a.severidade() != Apontamento.Severidade.ALERTA).toList();
    }
}
