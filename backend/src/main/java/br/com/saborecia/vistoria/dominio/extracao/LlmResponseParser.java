package br.com.saborecia.vistoria.dominio.extracao;

import br.com.saborecia.vistoria.dominio.ItemVistoria;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Locale;

/**
 * Converte a resposta textual do LLM em {@link ItemVistoria}, de forma tolerante:
 * <ul>
 *   <li>remove blocos ```json ... ``` e texto antes/depois do objeto;</li>
 *   <li>aceita chaves em camelCase ou snake_case;</li>
 *   <li>trata "null", "", "N/A" como null (a IA não deve inventar, mas às vezes escreve isso);</li>
 *   <li>aceita booleanos como true/"true"/"sim";</li>
 *   <li>normaliza confiança 0..100 para 0..1.</li>
 * </ul>
 * Se não houver um objeto JSON utilizável, lança {@link RespostaIaInvalidaException}
 * (o serviço então aciona o fallback por Regex).
 */
public class LlmResponseParser {

    private final ObjectMapper mapper;

    public LlmResponseParser(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public ItemVistoria parse(String resposta) {
        if (resposta == null || resposta.isBlank()) {
            throw new RespostaIaInvalidaException("Resposta vazia do LLM");
        }
        int ini = resposta.indexOf('{');
        int fim = resposta.lastIndexOf('}');
        if (ini < 0 || fim <= ini) {
            throw new RespostaIaInvalidaException("Resposta do LLM não contém um objeto JSON");
        }
        JsonNode n;
        try {
            n = mapper.readTree(resposta.substring(ini, fim + 1));
        } catch (Exception e) {
            throw new RespostaIaInvalidaException("JSON malformado devolvido pelo LLM: " + e.getMessage());
        }
        if (!n.isObject()) throw new RespostaIaInvalidaException("JSON do LLM não é um objeto");
        if (n.has("item") && n.get("item").isObject()) n = n.get("item"); // às vezes vem embrulhado

        ItemVistoria item = new ItemVistoria(
                texto(n, "tipoItem", "tipo_item", "tipo"),
                texto(n, "unidade", "filial"),
                texto(n, "enderecoUnidade", "endereco_unidade", "endereco"),
                texto(n, "tecnicoResponsavel", "tecnico_responsavel", "tecnico"),
                texto(n, "dataVistoria", "data_vistoria"),
                booleano(n, "itemPresente", "item_presente"),
                texto(n, "condicaoVisual", "condicao_visual", "condicao"),
                booleano(n, "seloValidadePresente", "selo_validade_presente", "seloPresente"),
                texto(n, "dataValidade", "data_validade", "validade", "vencimento"),
                texto(n, "localizacao", "local"),
                texto(n, "classificacaoRisco", "classificacao_risco", "risco"),
                texto(n, "normaAssociada", "norma_associada", "norma"),
                texto(n, "grauUrgencia", "grau_urgencia", "urgencia"),
                numero(n, "confianca", "confidence"));

        if (item.tipoItem() == null && item.itemPresente() == null && item.dataValidade() == null
                && item.condicaoVisual() == null) {
            throw new RespostaIaInvalidaException("JSON do LLM sem nenhum dos campos esperados");
        }
        return item;
    }

    private static JsonNode campo(JsonNode n, String... nomes) {
        for (String nome : nomes) {
            JsonNode v = n.get(nome);
            if (v != null && !v.isNull()) return v;
        }
        return null;
    }

    static String texto(JsonNode n, String... nomes) {
        JsonNode v = campo(n, nomes);
        if (v == null) return null;
        String s = v.isValueNode() ? v.asText().trim() : v.toString();
        String u = s.toUpperCase(Locale.ROOT);
        if (s.isEmpty() || u.equals("NULL") || u.equals("N/A") || u.equals("NA") || u.equals("NAO ENCONTRADO")
                || u.equals("NÃO ENCONTRADO") || u.equals("DESCONHECIDO")) {
            return null;
        }
        return s;
    }

    static Boolean booleano(JsonNode n, String... nomes) {
        JsonNode v = campo(n, nomes);
        if (v == null) return null;
        if (v.isBoolean()) return v.asBoolean();
        String s = v.asText().trim().toLowerCase(Locale.ROOT);
        return switch (s) {
            case "true", "sim", "yes", "s", "1", "presente" -> Boolean.TRUE;
            case "false", "nao", "não", "no", "n", "0", "ausente" -> Boolean.FALSE;
            default -> null;
        };
    }

    static Double numero(JsonNode n, String... nomes) {
        JsonNode v = campo(n, nomes);
        if (v == null) return null;
        double d;
        if (v.isNumber()) {
            d = v.asDouble();
        } else {
            try {
                d = Double.parseDouble(v.asText().replace("%", "").replace(',', '.').trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        if (d > 1 && d <= 100) d = d / 100.0;
        return Math.max(0, Math.min(1, d));
    }
}
