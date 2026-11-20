package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regra 4 (segunda parte): a norma técnica associada precisa estar ativa (NR vigente no cadastro).
 */
public class RegraNormaVigente implements Regra {

    public static final String CODIGO = "NORMA_VIGENTE";
    private static final Pattern NR = Pattern.compile("NR\\s*[-–]?\\s*0*(\\d{1,2})", Pattern.CASE_INSENSITIVE);

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        Optional<String> norma = normalizar(item.normaAssociada())
                .or(() -> TipoItem.deTexto(item.tipoItem()).map(TipoItem::normaPadrao));
        if (norma.isEmpty()) return List.of();
        if (item.normaAssociada() != null && normalizar(item.normaAssociada()).isEmpty()) {
            return List.of(Apontamento.revisao(CODIGO, "Norma em formato desconhecido: '" + item.normaAssociada() + "'",
                    "Não reconhecemos a norma informada; um supervisor vai conferir."));
        }
        if (!ctx.normasAtivas().contains(norma.get())) {
            return List.of(Apontamento.reprova(CODIGO, norma.get() + " não está entre as normas ativas " + ctx.normasAtivas(),
                    "A norma " + norma.get() + " associada a este item não está vigente. Atualize o checklist."));
        }
        return List.of();
    }

    /** "nr 23", "NR-23", "NR23", "NR-023" → "NR-23". */
    public static Optional<String> normalizar(String texto) {
        if (texto == null) return Optional.empty();
        Matcher m = NR.matcher(texto);
        return m.find() ? Optional.of("NR-" + Integer.parseInt(m.group(1))) : Optional.empty();
    }
}
