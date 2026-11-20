package br.com.saborecia.vistoria.dominio;

import br.com.saborecia.vistoria.dominio.extracao.RegexExtractor;
import br.com.saborecia.vistoria.dominio.regras.MotorDeRegras;
import br.com.saborecia.vistoria.dominio.regras.ResultadoValidacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Regressão com os dados REAIS das imagens de exemplos/ (métricas do OpenCV e texto do Tesseract),
 * no modo sem IA (Regex). Garante que a demonstração final continua dando o resultado esperado.
 */
class CenariosDemonstracaoTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 19);
    static final Set<String> NORMAS = Set.of("NR-6", "NR-8", "NR-10", "NR-12", "NR-23", "NR-24", "NR-26");

    final RegexExtractor regex = new RegexExtractor();
    final MotorDeRegras motor = MotorDeRegras.padrao();

    static final String OCR_CONFORME = """
            EXTINTOR PQS 4 kg - Classe BC |
            Unidade: Sabor & Cia - Boa Viagem |
            Local: Cozinha
            Data da recarga: 08/2026
            Validade: 08/2027
            Norma: NR-23 | Selo INMETRO 004512""";

    static final String OCR_VENCIDO = """
            EXTINTOR PQS 4 kg - Classe BC
            Unidade: Sabor & Cia - Boa Viagem
            Local: Cozinha
            Data da recarga: 08/2026
            Validade: 10/08/2026
            Norma: NR-23 | Selo INMETRO 004512
            |""";

    ResultadoValidacao rodar(String ocr, QualidadeImagem q, double confOcr) {
        ContextoValidacao ctx = new ContextoValidacao(HOJE, "Sabor & Cia - Boa Viagem", TipoItem.EXTINTOR, NORMAS, q,
                QualidadeImagem.Limites.padrao(), confOcr, OrigemExtracao.REGEX, 0.7, 30);
        ItemVistoria item = (ocr == null ? ItemVistoria.vazio() : regex.extrair(ocr, TipoItem.EXTINTOR))
                .completarCom("Sabor & Cia - Boa Viagem", "Elisa Martins", HOJE.toString(), "COZINHA");
        return motor.validar(item, ctx);
    }

    @Test
    void foto1_conforme() {
        ResultadoValidacao r = rodar(OCR_CONFORME, new QualidadeImagem(280.5, 122.7, 76.2, 0, true), 94.2);
        assertEquals(StatusItem.CONFORME, r.status(), r.resumo());
        assertEquals(LocalDate.of(2027, 8, 31), r.dataValidade());
    }

    @Test
    void foto2_vencido() {
        ResultadoValidacao r = rodar(OCR_VENCIDO, new QualidadeImagem(283.8, 122.4, 76.3, 0, true), 92.8);
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().startsWith("O extintor está vencido desde 10/08/2026 (há 101 dias)."), r.resumo());
    }

    @Test
    void foto3_ilegivel() {
        ResultadoValidacao r = rodar(null, new QualidadeImagem(0.5, 33.6, 19.9, 0, false), 0);
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().contains("tremida ou borrada"));
        assertTrue(r.resumo().contains("muito escura"));
    }
}
