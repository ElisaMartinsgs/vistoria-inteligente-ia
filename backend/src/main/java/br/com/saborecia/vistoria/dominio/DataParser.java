package br.com.saborecia.vistoria.dominio;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converte as datas que aparecem em selos/etiquetas (ou que a IA devolve) para {@link LocalDate}
 * usando Regex + validação de calendário em Java.
 * <p>
 * Formatos aceitos: 10/08/2026, 10-08-26, 10.08.2026, 2026-08-10, 08/2026, AGO/2026, AGO/26.
 * Quando só há mês/ano (comum em selo de extintor), considera o ÚLTIMO dia do mês.
 */
public final class DataParser {

    public static final int ANO_MINIMO = 2000;
    public static final int ANO_MAXIMO = 2100;

    private static final Pattern ISO = Pattern.compile("\\b(\\d{4})-(\\d{1,2})-(\\d{1,2})\\b");
    private static final Pattern DIA_MES_ANO = Pattern.compile("\\b(\\d{1,2})[/.\\-](\\d{1,2})[/.\\-](\\d{4}|\\d{2})\\b");
    private static final Pattern MES_ANO = Pattern.compile("\\b(\\d{1,2})[/.\\-](\\d{4})\\b");
    private static final Pattern MES_NOME_ANO = Pattern.compile(
            "\\b(JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)[A-Z]*[/.\\-\\s]*(\\d{4}|\\d{2})\\b");

    private static final Map<String, Integer> MESES = Map.ofEntries(
            Map.entry("JAN", 1), Map.entry("FEV", 2), Map.entry("MAR", 3), Map.entry("ABR", 4),
            Map.entry("MAI", 5), Map.entry("JUN", 6), Map.entry("JUL", 7), Map.entry("AGO", 8),
            Map.entry("SET", 9), Map.entry("OUT", 10), Map.entry("NOV", 11), Map.entry("DEZ", 12));

    private DataParser() {}

    /** Tenta interpretar o texto inteiro como uma data. Vazio se não reconhecer ou se a data não existir. */
    public static Optional<LocalDate> parse(String texto) {
        if (texto == null || texto.isBlank()) return Optional.empty();
        String t = ClassificacaoRisco.TextoUtil.normalizar(texto);

        Matcher m = ISO.matcher(t);
        if (m.find()) return montar(m.group(1), m.group(2), m.group(3));

        m = DIA_MES_ANO.matcher(t);
        if (m.find()) return montar(m.group(3), m.group(2), m.group(1));

        m = MES_ANO.matcher(t);
        if (m.find()) return ultimoDia(Integer.parseInt(m.group(2)), Integer.parseInt(m.group(1)));

        m = MES_NOME_ANO.matcher(t);
        if (m.find()) return ultimoDia(ano(m.group(2)), MESES.get(m.group(1)));

        return Optional.empty();
    }

    /** True se o texto parece uma data mas é impossível (ex.: 31/02/2026) ou fora do intervalo aceito. */
    public static boolean pareceDataInvalida(String texto) {
        if (texto == null || texto.isBlank()) return false;
        return parse(texto).isEmpty() && texto.matches(".*\\d.*");
    }

    private static Optional<LocalDate> montar(String a, String m, String d) {
        try {
            int ano = ano(a);
            if (ano < ANO_MINIMO || ano > ANO_MAXIMO) return Optional.empty();
            return Optional.of(LocalDate.of(ano, Integer.parseInt(m), Integer.parseInt(d)));
        } catch (DateTimeException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Optional<LocalDate> ultimoDia(int ano, int mes) {
        try {
            if (ano < ANO_MINIMO || ano > ANO_MAXIMO) return Optional.empty();
            return Optional.of(YearMonth.of(ano, mes).atEndOfMonth());
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    private static int ano(String s) {
        int a = Integer.parseInt(s);
        return s.length() == 2 ? 2000 + a : a;
    }
}
