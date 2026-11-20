package br.com.saborecia.vistoria.dominio.extracao;

import br.com.saborecia.vistoria.dominio.ItemVistoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LlmResponseParserTest {

    final LlmResponseParser parser = new LlmResponseParser(new ObjectMapper());

    @Test
    void jsonLimpo() {
        ItemVistoria i = parser.parse("""
                {"tipoItem":"EXTINTOR","itemPresente":true,"seloValidadePresente":true,
                 "dataValidade":"08/2027","classificacaoRisco":"INCENDIO","normaAssociada":"NR-23","confianca":0.93}
                """);
        assertEquals("EXTINTOR", i.tipoItem());
        assertEquals(Boolean.TRUE, i.itemPresente());
        assertEquals("08/2027", i.dataValidade());
        assertEquals(0.93, i.confianca(), 0.0001);
        assertNull(i.unidade());
    }

    @Test
    void jsonDentroDeMarkdownComTextoExtra() {
        ItemVistoria i = parser.parse("Claro! Aqui está:\n```json\n{\"tipo_item\": \"EXTINTOR\", \"item_presente\": \"sim\", \"confianca\": 85}\n```\nAbraço");
        assertEquals("EXTINTOR", i.tipoItem());
        assertEquals(Boolean.TRUE, i.itemPresente());
        assertEquals(0.85, i.confianca(), 0.0001);
    }

    @Test
    void nullEscritoComoTexto() {
        ItemVistoria i = parser.parse("{\"tipoItem\":\"EXTINTOR\",\"dataValidade\":\"null\",\"unidade\":\"N/A\"}");
        assertNull(i.dataValidade());
        assertNull(i.unidade());
    }

    @Test
    void respostasInutilizaveis() {
        assertThrows(RespostaIaInvalidaException.class, () -> parser.parse(""));
        assertThrows(RespostaIaInvalidaException.class, () -> parser.parse("Não consegui ler o documento."));
        assertThrows(RespostaIaInvalidaException.class, () -> parser.parse("{\"tipoItem\": \"EXTINTOR\", "));
        assertThrows(RespostaIaInvalidaException.class, () -> parser.parse("{\"foo\": 1}"));
    }
}
