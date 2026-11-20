package br.com.saborecia.vistoria.dominio.extracao;

import br.com.saborecia.vistoria.dominio.TipoItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    @Test
    void montaPromptAPartirDoTemplateVersionado() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/prompts/extracao-item-v2.txt"));
        PromptBuilder p = new PromptBuilder(template);
        assertTrue(p.sistema().contains("EXTINTOR, SAIDA_EMERGENCIA"));
        assertTrue(p.sistema().contains("INCENDIO, ESCORREGAMENTO"));
        assertFalse(p.sistema().contains("{{"));
        String u = p.usuario("VALIDADE 08/2027", TipoItem.EXTINTOR, "Cozinha", "Sabor & Cia - Boa Viagem", "2026-11-19");
        assertTrue(u.contains("VALIDADE 08/2027"));
        assertTrue(u.contains("EXTINTOR"));
        assertFalse(u.contains("{{"));
    }

    @Test
    void truncaTextoMuitoGrande() throws Exception {
        String template = Files.readString(Path.of("src/main/resources/prompts/extracao-item-v2.txt"));
        String u = new PromptBuilder(template).usuario("x".repeat(10_000), null, null, null, "2026-11-19");
        assertTrue(u.length() < 6000);
        assertTrue(u.contains("texto truncado"));
    }
}
