package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;

import java.util.List;

/**
 * Aprovação: "dados conferem com o cadastro do restaurante".
 * Se o documento/etiqueta cita outra unidade, o item vai para revisão.
 */
public class RegraUnidadeConfere implements Regra {

    public static final String CODIGO = "UNIDADE_CONFERE";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        if (item.unidade() == null || item.unidade().isBlank() || ctx.unidadeCadastro() == null) return List.of();
        String lida = ClassificacaoRisco.TextoUtil.normalizar(item.unidade()).replaceAll("[^A-Z0-9]", "");
        String cadastro = ClassificacaoRisco.TextoUtil.normalizar(ctx.unidadeCadastro()).replaceAll("[^A-Z0-9]", "");
        if (lida.isEmpty()) return List.of();
        if (lida.contains(cadastro) || cadastro.contains(lida)) return List.of();
        return List.of(Apontamento.revisao(CODIGO,
                "Unidade no documento '" + item.unidade() + "' difere do cadastro '" + ctx.unidadeCadastro() + "'",
                "O documento parece ser de outra unidade (" + item.unidade() + "); um supervisor vai conferir."));
    }
}
