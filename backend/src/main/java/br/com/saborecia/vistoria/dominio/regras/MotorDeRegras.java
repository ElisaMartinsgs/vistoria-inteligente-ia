package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.DataParser;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.Urgencia;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Aplica todas as regras e decide o status final:
 * <pre>
 *   algum REPROVA  → NAO_CONFORME
 *   algum REVISAO  → REVISAO_MANUAL
 *   senão          → CONFORME (alertas são mantidos no relatório)
 * </pre>
 * Java puro, sem Spring: pode ser testado sem subir a aplicação.
 */
public class MotorDeRegras {

    private final RegraLegibilidade legibilidade = new RegraLegibilidade();
    private final List<Regra> regras;

    public MotorDeRegras(List<Regra> regras) {
        this.regras = List.copyOf(regras);
    }

    /** Conjunto de regras do MVP, na ordem em que aparecem no relatório. */
    public static MotorDeRegras padrao() {
        return new MotorDeRegras(List.of(
                new RegraCamposObrigatorios(),
                new RegraItemCorresponde(),
                new RegraItemPresente(),
                new RegraSeloValidade(),
                new RegraValidade(),
                new RegraRiscoCompativel(),
                new RegraNormaVigente(),
                new RegraUnidadeConfere(),
                new RegraConfiancaIa()));
    }

    /** Avalia apenas a legibilidade (usado antes de gastar OCR/IA com uma foto ruim). */
    public List<Apontamento> avaliarLegibilidade(ContextoValidacao ctx) {
        return legibilidade.avaliar(ItemVistoria.vazio(), ctx);
    }

    public ResultadoValidacao validar(ItemVistoria item, ContextoValidacao ctx) {
        return validar(item, ctx, List.of());
    }

    /**
     * @param extras apontamentos gerados fora das regras (ex.: "OCR indisponível"), entram na decisão.
     */
    public ResultadoValidacao validar(ItemVistoria item, ContextoValidacao ctx, List<Apontamento> extras) {
        List<String> aplicadas = new ArrayList<>();
        List<Apontamento> apontamentos = new ArrayList<>();

        // 1) Legibilidade primeiro: foto ilegível encerra a validação.
        aplicadas.add(legibilidade.codigo());
        apontamentos.addAll(legibilidade.avaliar(item, ctx));
        boolean ilegivel = apontamentos.stream().anyMatch(a -> a.severidade() == Apontamento.Severidade.REPROVA);

        if (!ilegivel) {
            for (Regra r : regras) {
                aplicadas.add(r.codigo());
                apontamentos.addAll(r.avaliar(item, ctx));
            }
            apontamentos.addAll(extras);
        }

        StatusItem status = decidir(apontamentos);
        Optional<TipoItem> tipo = TipoItem.deTexto(item.tipoItem());
        ClassificacaoRisco risco = ClassificacaoRisco.deTexto(item.classificacaoRisco())
                .filter(r -> tipo.map(t -> t.riscosCompativeis().contains(r)).orElse(true))
                .or(() -> tipo.map(TipoItem::riscoPadrao))
                .orElse(null);
        String norma = RegraNormaVigente.normalizar(item.normaAssociada())
                .or(() -> tipo.map(TipoItem::normaPadrao))
                .orElse(null);
        LocalDate validade = DataParser.parse(item.dataValidade()).orElse(null);
        boolean temAlerta = apontamentos.stream().anyMatch(a -> a.severidade() == Apontamento.Severidade.ALERTA);

        return new ResultadoValidacao(status, tipo.orElse(null), risco, norma, validade,
                Urgencia.calcular(status, risco, temAlerta), aplicadas, apontamentos,
                resumo(status, tipo.orElse(ctx.tipoEsperado()), norma, apontamentos));
    }

    static StatusItem decidir(List<Apontamento> apontamentos) {
        if (apontamentos.stream().anyMatch(a -> a.severidade() == Apontamento.Severidade.REPROVA)) {
            return StatusItem.NAO_CONFORME;
        }
        if (apontamentos.stream().anyMatch(a -> a.severidade() == Apontamento.Severidade.REVISAO)) {
            return StatusItem.REVISAO_MANUAL;
        }
        return StatusItem.CONFORME;
    }

    private static String resumo(StatusItem status, TipoItem tipo, String norma, List<Apontamento> aps) {
        String nome = tipo != null ? tipo.nomeAmigavel() : "item";
        return switch (status) {
            case CONFORME -> {
                String base = "Item conforme: " + nome + (norma != null ? " de acordo com a " + norma : "") + ".";
                String alertas = juntar(aps, Apontamento.Severidade.ALERTA);
                yield alertas.isEmpty() ? base : base + " Atenção: " + alertas;
            }
            case NAO_CONFORME -> juntar(aps, Apontamento.Severidade.REPROVA);
            case REVISAO_MANUAL -> "Enviado para revisão do supervisor. " + juntar(aps, Apontamento.Severidade.REVISAO);
        };
    }

    private static String juntar(List<Apontamento> aps, Apontamento.Severidade sev) {
        return aps.stream().filter(a -> a.severidade() == sev)
                .map(Apontamento::mensagemSimples).distinct().collect(Collectors.joining(" "));
    }
}
