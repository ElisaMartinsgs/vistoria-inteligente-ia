package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.util.List;

/**
 * Regra 5 (segunda parte): quando exigido, a etiqueta/selo de validade precisa estar visível.
 */
public class RegraSeloValidade implements Regra {

    public static final String CODIGO = "SELO_VALIDADE";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        return TipoItem.deTexto(item.tipoItem())
                .filter(TipoItem::exigeValidade)
                .filter(t -> Boolean.FALSE.equals(item.seloValidadePresente()))
                .map(t -> List.of(Apontamento.reprova(CODIGO, "seloValidadePresente=false para " + t,
                        "O selo/etiqueta de validade do " + t.nomeAmigavel() + " não aparece na foto.")))
                .orElse(List.of());
    }
}
