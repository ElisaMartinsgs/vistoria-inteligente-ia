package br.com.saborecia.vistoria.dominio;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class DataParserTest {

    @Test
    void formatosBrasileiros() {
        assertEquals(LocalDate.of(2026, 8, 10), DataParser.parse("10/08/2026").orElseThrow());
        assertEquals(LocalDate.of(2026, 8, 10), DataParser.parse("10-08-26").orElseThrow());
        assertEquals(LocalDate.of(2026, 8, 10), DataParser.parse("Validade: 10.08.2026").orElseThrow());
    }

    @Test
    void formatoIso() {
        assertEquals(LocalDate.of(2026, 8, 10), DataParser.parse("2026-08-10").orElseThrow());
    }

    @Test
    void mesAnoViraUltimoDiaDoMes() {
        assertEquals(LocalDate.of(2027, 2, 28), DataParser.parse("02/2027").orElseThrow());
        assertEquals(LocalDate.of(2026, 8, 31), DataParser.parse("AGO/2026").orElseThrow());
        assertEquals(LocalDate.of(2026, 12, 31), DataParser.parse("dezembro 26").orElseThrow());
    }

    @Test
    void dataImpossivelOuForaDoIntervalo() {
        assertTrue(DataParser.parse("31/02/2026").isEmpty());
        assertTrue(DataParser.parse("10/08/1890").isEmpty());
        assertTrue(DataParser.parse("abc").isEmpty());
        assertTrue(DataParser.parse(null).isEmpty());
        assertTrue(DataParser.pareceDataInvalida("31/02/2026"));
        assertFalse(DataParser.pareceDataInvalida("10/08/2026"));
    }
}
