package br.com.saborecia.vistoria.dominio.extracao;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.DataParser;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.regras.RegraNormaVigente;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * "E se eu não quiser usar IA?" — extrai os dados diretamente do texto do OCR com palavras-chave e Regex.
 * <p>
 * Usado em dois cenários:
 * <ol>
 *   <li>modo {@code vistoria.ia.provedor=regex} (sem custo, sem chave de API);</li>
 *   <li>fallback automático quando o LLM está indisponível, estoura o timeout ou devolve JSON inválido.</li>
 * </ol>
 * Funciona bem para etiquetas com layout previsível; com muitos formatos diferentes a IA é mais flexível.
 */
public class RegexExtractor {

    private static final Pattern DATA = Pattern.compile(
            "(\\d{4}-\\d{1,2}-\\d{1,2}|\\d{1,2}[/.\\-]\\d{1,2}[/.\\-]\\d{2,4}|\\d{1,2}[/.\\-]\\d{4}"
                    + "|(?:JAN|FEV|MAR|ABR|MAI|JUN|JUL|AGO|SET|OUT|NOV|DEZ)[A-Z]*[/.\\-\\s]*\\d{2,4})");
    private static final Pattern LINHA_VALIDADE = Pattern.compile(
            "(VALIDADE|VALIDO ATE|VENCIMENTO|VENC\\.?|VENCE|\\bVAL\\b\\.?|PROX(IMA|\\.)?\\s*RECARGA|PROXIMA MANUTENCAO)");
    private static final Pattern LINHA_VISTORIA = Pattern.compile("(DATA DA VISTORIA|VISTORIA EM|DATA DE VISTORIA|INSPECAO EM)");
    private static final Pattern ROTULO_UNIDADE = Pattern.compile("(?:UNIDADE|FILIAL|RESTAURANTE)\\s*[:\\-]\\s*(.+)");
    private static final Pattern ROTULO_TECNICO = Pattern.compile("(?:TECNICO(?:\\(A\\))?|RESPONSAVEL|INSPETOR)\\s*[:\\-]\\s*(.+)");
    private static final Pattern ROTULO_ENDERECO = Pattern.compile("(?:ENDERECO)\\s*[:\\-]\\s*(.+)");

    public ItemVistoria extrair(String textoOcr, TipoItem tipoEsperado) {
        if (textoOcr == null || textoOcr.isBlank()) {
            return ItemVistoria.vazio();
        }
        String original = textoOcr;
        String t = ClassificacaoRisco.TextoUtil.normalizar(textoOcr);
        String[] linhas = t.split("\\R");
        String[] linhasOriginais = original.split("\\R");

        TipoItem tipo = detectarTipo(t, tipoEsperado);
        String validade = dataPerto(linhas, LINHA_VALIDADE).orElse(null);
        String vistoria = dataPerto(linhas, LINHA_VISTORIA).orElse(null);
        String unidade = rotulo(linhas, linhasOriginais, ROTULO_UNIDADE);
        String tecnico = rotulo(linhas, linhasOriginais, ROTULO_TECNICO);
        String endereco = rotulo(linhas, linhasOriginais, ROTULO_ENDERECO);
        String norma = RegraNormaVigente.normalizar(t).orElse(null);
        String local = detectarLocal(t);

        Boolean selo = null;
        if (validade != null || t.contains("INMETRO") || t.contains("SELO")) selo = Boolean.TRUE;

        double confianca;
        if (tipo == null) confianca = 0.2;
        else if (tipo.exigeValidade() && validade == null) confianca = 0.6;
        else confianca = 0.85;

        return new ItemVistoria(
                tipo != null ? tipo.name() : null,
                unidade, endereco, tecnico,
                vistoria,
                tipo != null ? Boolean.TRUE : null,
                null,
                selo,
                validade,
                local,
                null,
                norma,
                null,
                confianca);
    }

    /** Tipo com mais palavras-chave encontradas; em empate, prefere o tipo esperado. */
    static TipoItem detectarTipo(String t, TipoItem esperado) {
        TipoItem melhor = null;
        int melhorPontos = 0;
        for (TipoItem tipo : TipoItem.values()) {
            int pontos = 0;
            for (String chave : tipo.palavrasChave()) {
                if (t.contains(chave)) pontos += chave.length() > 6 ? 2 : 1;
            }
            if (pontos > melhorPontos || (pontos == melhorPontos && pontos > 0 && tipo == esperado)) {
                melhor = tipo;
                melhorPontos = pontos;
            }
        }
        return melhor;
    }

    /** Procura uma data na mesma linha da palavra-chave ou na linha seguinte. */
    static Optional<String> dataPerto(String[] linhas, Pattern chave) {
        for (int i = 0; i < linhas.length; i++) {
            Matcher k = chave.matcher(linhas[i]);
            if (!k.find()) continue;
            String resto = linhas[i].substring(k.end());
            Optional<String> d = primeiraData(resto);
            if (d.isEmpty() && i + 1 < linhas.length) d = primeiraData(linhas[i + 1]);
            if (d.isPresent()) return d;
        }
        return Optional.empty();
    }

    private static Optional<String> primeiraData(String s) {
        Matcher m = DATA.matcher(s);
        while (m.find()) {
            String bruto = m.group(1).trim();
            Optional<LocalDate> d = DataParser.parse(bruto);
            if (d.isPresent()) return Optional.of(d.get().format(DateTimeFormatter.ISO_LOCAL_DATE));
            return Optional.of(bruto); // parece data mas é inválida: devolve crua para a regra apontar
        }
        return Optional.empty();
    }

    private static String rotulo(String[] linhas, String[] originais, Pattern p) {
        for (int i = 0; i < linhas.length; i++) {
            Matcher m = p.matcher(linhas[i]);
            if (m.find()) {
                String valor = (i < originais.length && originais[i].length() == linhas[i].length())
                        ? originais[i].substring(m.start(1)).trim()
                        : m.group(1).trim();
                valor = valor.replaceAll("[|\\s]+$", ""); // OCR costuma ler bordas como "|"
                return valor.isEmpty() ? null : valor;
            }
        }
        return null;
    }

    private static String detectarLocal(String t) {
        if (t.contains("COZINHA")) return "COZINHA";
        if (t.contains("SALAO")) return "SALAO";
        if (t.contains("ESTOQUE")) return "ESTOQUE";
        if (t.contains("SAIDA")) return "SAIDA";
        return null;
    }
}
