package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.OrigemExtracao;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Critério de aceite da IA: só aprovamos automaticamente quando a extração é confiável.
 * <ul>
 *   <li>LLM indisponível e resultado veio do fallback por Regex → revisão.</li>
 *   <li>Confiança autoavaliada abaixo do limite → revisão.</li>
 * </ul>
 */
public class RegraConfiancaIa implements Regra {

    public static final String CODIGO = "CONFIANCA_IA";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        List<Apontamento> out = new ArrayList<>();
        if (ctx.origem() == OrigemExtracao.REGEX_FALLBACK) {
            out.add(Apontamento.revisao(CODIGO, "Extração feita pelo fallback de Regex (LLM indisponível)",
                    "A análise automática ficou incompleta; um supervisor vai conferir este item."));
        } else if (item.confianca() != null && item.confianca() < ctx.confiancaMinimaIa()) {
            out.add(Apontamento.revisao(CODIGO,
                    format(java.util.Locale.ROOT, "Confiança da extração %.2f abaixo do mínimo %.2f", item.confianca(), ctx.confiancaMinimaIa()),
                    "A leitura automática não teve segurança suficiente; um supervisor vai conferir."));
        }
        return out;
    }
}
