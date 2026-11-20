package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.util.List;
import java.util.Optional;

/**
 * Rejeição: "o objeto na imagem não é o item vistoriado".
 * Compara o tipo que a IA reconheceu com o item que o técnico disse estar fotografando.
 */
public class RegraItemCorresponde implements Regra {

    public static final String CODIGO = "ITEM_CORRESPONDE";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        if (ctx.tipoEsperado() == null) return List.of();
        Optional<TipoItem> lido = TipoItem.deTexto(item.tipoItem());
        if (lido.isEmpty() || lido.get() == ctx.tipoEsperado()) return List.of();
        return List.of(Apontamento.reprova(CODIGO,
                "Esperado " + ctx.tipoEsperado() + ", reconhecido " + lido.get(),
                "A foto parece ser de " + lido.get().nomeAmigavel() + ", mas o item vistoriado é "
                        + ctx.tipoEsperado().nomeAmigavel() + ". Envie a foto do item correto."));
    }
}
