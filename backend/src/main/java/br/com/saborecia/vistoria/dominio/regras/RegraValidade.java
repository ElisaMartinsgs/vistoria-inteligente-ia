package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.DataParser;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Regras 2 e 3: item dentro da validade, com data em formato correto e coerente (Regex + cálculo em Java).
 * <ul>
 *   <li>Vencido → NÃO CONFORME, com a quantidade de dias e a ação esperada.</li>
 *   <li>Vence em até N dias → ALERTA (continua CONFORME, mas o gerente é avisado para agendar a recarga).</li>
 *   <li>Data ilegível/impossível → REVISÃO.</li>
 *   <li>Data absurda (mais de 6 anos à frente) → REVISÃO (provável erro de leitura do OCR/IA).</li>
 * </ul>
 */
public class RegraValidade implements Regra {

    public static final String CODIGO = "VALIDADE";
    public static final int ANOS_MAXIMOS_A_FRENTE = 6;
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        Optional<TipoItem> tipoOpt = TipoItem.deTexto(item.tipoItem());
        if (tipoOpt.isEmpty() || !tipoOpt.get().podeTerValidade()) return List.of();
        TipoItem tipo = tipoOpt.get();
        String nome = tipo.nomeAmigavel();

        if (item.dataValidade() == null || item.dataValidade().isBlank()) {
            if (tipo.exigeValidade()) {
                return List.of(Apontamento.reprova(CODIGO, "dataValidade retornou null para " + tipo,
                        "Não encontramos a data de validade do " + nome + ". Fotografe o selo/etiqueta de perto."));
            }
            return List.of();
        }

        Optional<LocalDate> validade = DataParser.parse(item.dataValidade());
        if (validade.isEmpty()) {
            return List.of(Apontamento.revisao(CODIGO, "dataValidade em formato inválido: '" + item.dataValidade() + "'",
                    "A data de validade lida (" + item.dataValidade() + ") não é uma data válida; um supervisor vai conferir."));
        }

        LocalDate referencia = Optional.ofNullable(item.dataVistoria())
                .flatMap(DataParser::parse)
                .orElse(ctx.dataReferencia());
        LocalDate v = validade.get();

        if (v.isAfter(referencia.plusYears(ANOS_MAXIMOS_A_FRENTE))) {
            return List.of(Apontamento.revisao(CODIGO, "dataValidade improvável: " + v,
                    "A validade lida (" + BR.format(v) + ") parece incorreta; um supervisor vai conferir."));
        }

        if (v.isBefore(referencia)) {
            long dias = ChronoUnit.DAYS.between(v, referencia);
            String acao = tipo == TipoItem.EXTINTOR
                    ? " Providencie a recarga com o fornecedor."
                    : " Providencie a substituição ou manutenção.";
            return List.of(Apontamento.reprova(CODIGO,
                    "Vencido em " + v + " (" + dias + " dias antes de " + referencia + ")",
                    "O " + nome + " está vencido desde " + BR.format(v) + " (há " + dias + " dia" + (dias == 1 ? "" : "s") + ")." + acao));
        }

        long diasParaVencer = ChronoUnit.DAYS.between(referencia, v);
        if (diasParaVencer <= ctx.diasAlertaVencimento()) {
            return List.of(Apontamento.alerta(CODIGO, "Vence em " + diasParaVencer + " dias (" + v + ")",
                    "O " + nome + " vence em " + diasParaVencer + " dia" + (diasParaVencer == 1 ? "" : "s")
                            + " (" + BR.format(v) + "). Agende a recarga/manutenção."));
        }
        return List.of();
    }
}
