package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.util.ArrayList;
import java.util.List;

/**
 * Regra 2 (primeira parte): o item está presente e em boas condições visuais.
 */
public class RegraItemPresente implements Regra {

    public static final String CODIGO = "ITEM_PRESENTE";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        List<Apontamento> out = new ArrayList<>();
        String nome = TipoItem.deTexto(item.tipoItem())
                .or(() -> java.util.Optional.ofNullable(ctx.tipoEsperado()))
                .map(TipoItem::nomeAmigavel).orElse("item");

        if (Boolean.FALSE.equals(item.itemPresente())) {
            out.add(Apontamento.reprova(CODIGO, "itemPresente=false",
                    "O " + nome + " não foi encontrado na foto. Envie outra foto."));
            return out;
        }

        String condicao = ClassificacaoRisco.TextoUtil.normalizar(item.condicaoVisual());
        if (condicao.contains("DANIFICADO") || condicao.contains("AVARIADO") || condicao.contains("QUEBRADO")) {
            out.add(Apontamento.reprova(CODIGO, "condicaoVisual=" + item.condicaoVisual(),
                    "O " + nome + " está danificado e precisa ser substituído ou reparado."));
        } else if (condicao.contains("OBSTRUIDO") || condicao.contains("BLOQUEADO")) {
            out.add(Apontamento.reprova(CODIGO, "condicaoVisual=" + item.condicaoVisual(),
                    "O acesso ao " + nome + " está obstruído. Libere a passagem."));
        } else if (condicao.contains("NAO_IDENTIFICADO") || condicao.contains("NAO IDENTIFICADO")) {
            out.add(Apontamento.revisao(CODIGO, "condicaoVisual=" + item.condicaoVisual(),
                    "Não foi possível avaliar a condição do " + nome + "; um supervisor vai conferir."));
        }
        return out;
    }
}
