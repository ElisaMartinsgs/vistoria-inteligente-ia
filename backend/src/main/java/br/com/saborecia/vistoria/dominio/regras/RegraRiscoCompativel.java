package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.util.List;
import java.util.Optional;

/**
 * Regra 4 (primeira parte): classificação de risco compatível com o tipo de item
 * (ex.: extintor → risco de incêndio). Incompatível indica erro de interpretação da IA → revisão.
 */
public class RegraRiscoCompativel implements Regra {

    public static final String CODIGO = "RISCO_COMPATIVEL";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        Optional<TipoItem> tipo = TipoItem.deTexto(item.tipoItem());
        if (tipo.isEmpty() || item.classificacaoRisco() == null || item.classificacaoRisco().isBlank()) {
            return List.of(); // sem risco informado: o sistema usa o risco padrão do tipo
        }
        Optional<ClassificacaoRisco> risco = ClassificacaoRisco.deTexto(item.classificacaoRisco());
        if (risco.isPresent() && tipo.get().riscosCompativeis().contains(risco.get())) {
            return List.of();
        }
        return List.of(Apontamento.revisao(CODIGO,
                "Risco '" + item.classificacaoRisco() + "' incompatível com " + tipo.get()
                        + " (esperado um de " + tipo.get().riscosCompativeis() + ")",
                "A classificação de risco não bate com o tipo de item; um supervisor vai conferir."));
    }
}
