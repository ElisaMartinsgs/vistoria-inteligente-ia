package br.com.saborecia.vistoria.dominio.extracao;

import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegexExtractorTest {

    final RegexExtractor ex = new RegexExtractor();

    @Test
    void etiquetaDeExtintor() {
        String ocr = """
                SELO DE MANUTENÇÃO - INMETRO
                EXTINTOR PQS 4kg  Classe BC
                Unidade: Sabor & Cia - Boa Viagem
                Local: Cozinha
                Validade: 08/2027
                NR-23
                """;
        ItemVistoria i = ex.extrair(ocr, TipoItem.EXTINTOR);
        assertEquals("EXTINTOR", i.tipoItem());
        assertEquals("2027-08-31", i.dataValidade());
        assertEquals("Sabor & Cia - Boa Viagem", i.unidade());
        assertEquals("COZINHA", i.localizacao());
        assertEquals("NR-23", i.normaAssociada());
        assertEquals(Boolean.TRUE, i.seloValidadePresente());
        assertEquals(0.85, i.confianca(), 0.0001);
    }

    @Test
    void dataNaLinhaSeguinte() {
        ItemVistoria i = ex.extrair("EXTINTOR CO2\nPROX. RECARGA\n10/08/2026", null);
        assertEquals("2026-08-10", i.dataValidade());
    }

    @Test
    void semTipoReconhecido() {
        ItemVistoria i = ex.extrair("cardápio do dia: feijoada", TipoItem.EXTINTOR);
        assertNull(i.tipoItem());
        assertEquals(0.2, i.confianca(), 0.0001);
    }

    @Test
    void textoVazio() {
        assertNull(ex.extrair("", null).tipoItem());
    }
}
