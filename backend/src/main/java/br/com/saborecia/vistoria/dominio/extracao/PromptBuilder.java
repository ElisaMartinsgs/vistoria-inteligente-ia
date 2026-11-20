package br.com.saborecia.vistoria.dominio.extracao;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Monta o prompt de extração a partir de um template versionado
 * (src/main/resources/prompts/extracao-item-v2.txt). Ver docs/05-prompts-e-relato.md.
 */
public class PromptBuilder {

    public static final String VERSAO = "extracao-item-v2";
    /** Limite de caracteres do OCR enviados ao LLM (controle de custo/tokens). */
    public static final int MAX_CARACTERES_OCR = 4000;

    private final String templateSistema;
    private final String templateUsuario;

    public PromptBuilder(String templateCompleto) {
        String[] partes = templateCompleto.split("(?m)^=== USUARIO ===\\s*$", 2);
        if (partes.length != 2) {
            throw new IllegalArgumentException("Template de prompt deve conter a linha '=== USUARIO ==='");
        }
        this.templateSistema = partes[0].replaceFirst("(?m)^=== SISTEMA ===\\s*$", "").trim();
        this.templateUsuario = partes[1].trim();
    }

    public String sistema() {
        return templateSistema
                .replace("{{tiposValidos}}", Arrays.stream(TipoItem.values()).map(Enum::name).collect(Collectors.joining(", ")))
                .replace("{{riscosValidos}}", Arrays.stream(ClassificacaoRisco.values()).map(Enum::name).collect(Collectors.joining(", ")));
    }

    public String usuario(String textoOcr, TipoItem tipoEsperado, String local, String unidade, String dataHoje) {
        String ocr = textoOcr == null ? "" : textoOcr.strip();
        if (ocr.length() > MAX_CARACTERES_OCR) ocr = ocr.substring(0, MAX_CARACTERES_OCR) + "\n[...texto truncado...]";
        if (ocr.isEmpty()) ocr = "(o OCR não encontrou texto)";
        return templateUsuario
                .replace("{{tipoEsperado}}", tipoEsperado != null ? tipoEsperado.name() : "não informado")
                .replace("{{localizacao}}", local != null ? local : "não informada")
                .replace("{{unidade}}", unidade != null ? unidade : "não informada")
                .replace("{{dataHoje}}", dataHoje)
                .replace("{{textoTesseract}}", ocr.replace("\"\"\"", "\" \" \""));
    }
}
