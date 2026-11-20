package br.com.saborecia.vistoria.dominio;

import br.com.saborecia.vistoria.dominio.regras.Apontamento;
import br.com.saborecia.vistoria.dominio.regras.MotorDeRegras;
import br.com.saborecia.vistoria.dominio.regras.ResultadoValidacao;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Os três cenários da demonstração final (conforme, vencido, ilegível) e os principais casos de borda.
 */
class MotorDeRegrasTest {

    static final LocalDate HOJE = LocalDate.of(2026, 11, 19);
    static final Set<String> NORMAS = Set.of("NR-6", "NR-8", "NR-10", "NR-12", "NR-23", "NR-24", "NR-26");
    static final QualidadeImagem BOA = new QualidadeImagem(350, 140, 60, 0, false);
    static final QualidadeImagem BORRADA = new QualidadeImagem(8, 130, 40, 0, false);
    static final QualidadeImagem ESCURA = new QualidadeImagem(120, 20, 25, 0, false);

    final MotorDeRegras motor = MotorDeRegras.padrao();

    static ContextoValidacao ctx(QualidadeImagem q, OrigemExtracao origem) {
        return new ContextoValidacao(HOJE, "Sabor & Cia - Boa Viagem", TipoItem.EXTINTOR, NORMAS, q,
                QualidadeImagem.Limites.padrao(), 85.0, origem, 0.7, 30);
    }

    static ItemVistoria extintor(String validade, Boolean presente, String condicao, Double confianca) {
        return new ItemVistoria("EXTINTOR", null, null, "Elisa", "19/11/2026", presente, condicao, true,
                validade, "COZINHA", "INCENDIO", "NR-23", null, confianca);
    }

    @Test
    void cenario1_extintorConforme() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, "OK", 0.92), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.CONFORME, r.status(), r.resumo());
        assertEquals(TipoItem.EXTINTOR, r.tipoItem());
        assertEquals(ClassificacaoRisco.INCENDIO, r.risco());
        assertEquals("NR-23", r.norma());
        assertEquals(LocalDate.of(2027, 8, 31), r.dataValidade());
        assertEquals(Urgencia.BAIXA, r.urgencia());
        assertTrue(r.apontamentos().isEmpty());
    }

    @Test
    void cenario2_extintorVencido() {
        ResultadoValidacao r = motor.validar(extintor("10/08/2026", true, "OK", 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertEquals(Urgencia.CRITICA, r.urgencia());
        assertTrue(r.resumo().contains("vencido desde 10/08/2026"), r.resumo());
        assertTrue(r.resumo().contains("101 dias"), r.resumo());
        assertTrue(r.resumo().contains("recarga"), r.resumo());
    }

    @Test
    void cenario3_fotoIlegivel_paraNaLegibilidade() {
        ResultadoValidacao r = motor.validar(ItemVistoria.vazio().completarCom("Sabor & Cia - Boa Viagem", "Elisa", "2026-11-19", null),
                ctx(BORRADA, OrigemExtracao.NENHUMA));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertEquals(1, r.regrasAplicadas().size(), "só a legibilidade deve rodar");
        assertTrue(r.resumo().contains("tremida ou borrada"), r.resumo());
    }

    @Test
    void fotoEscura() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, "OK", 0.9), ctx(ESCURA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().contains("escura"));
    }

    @Test
    void venceEmBreve_continuaConformeComAlerta() {
        ResultadoValidacao r = motor.validar(extintor("05/12/2026", true, "OK", 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.CONFORME, r.status());
        assertEquals(Urgencia.MEDIA, r.urgencia());
        assertTrue(r.resumo().contains("vence em 16 dias"), r.resumo());
    }

    @Test
    void itemAusente() {
        ResultadoValidacao r = motor.validar(extintor(null, false, null, 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().contains("O extintor não foi encontrado na foto. Envie outra foto."), r.resumo());
    }

    @Test
    void semDataDeValidadeEmExtintor_reprova() {
        ResultadoValidacao r = motor.validar(extintor(null, true, "OK", 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.apontamentos().stream().anyMatch(a -> a.codigoRegra().equals("VALIDADE")));
    }

    @Test
    void dataImpossivel_vaiParaRevisao() {
        ResultadoValidacao r = motor.validar(extintor("31/02/2027", true, "OK", 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.REVISAO_MANUAL, r.status());
    }

    @Test
    void dataAbsurda_vaiParaRevisao() {
        ResultadoValidacao r = motor.validar(extintor("08/2087", true, "OK", 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.REVISAO_MANUAL, r.status());
    }

    @Test
    void baixaConfiancaDaIa_vaiParaRevisao() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, "OK", 0.4), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.REVISAO_MANUAL, r.status());
    }

    @Test
    void fallbackDeRegex_vaiParaRevisao() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, null, 0.85), ctx(BOA, OrigemExtracao.REGEX_FALLBACK));
        assertEquals(StatusItem.REVISAO_MANUAL, r.status());
    }

    @Test
    void modoSemIa_regexCompletoPodeAprovar() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, null, 0.85), ctx(BOA, OrigemExtracao.REGEX));
        assertEquals(StatusItem.CONFORME, r.status(), r.resumo());
    }

    @Test
    void objetoDiferenteDoVistoriado() {
        ItemVistoria piso = new ItemVistoria("PISO_ANTIDERRAPANTE", null, null, null, "19/11/2026", true, "OK", null,
                null, "COZINHA", "ESCORREGAMENTO", "NR-8", null, 0.9);
        ResultadoValidacao r = motor.validar(piso, ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().contains("piso antiderrapante"), r.resumo());
    }

    @Test
    void normaRevogada_reprova() {
        ItemVistoria i = new ItemVistoria("EXTINTOR", null, null, null, "19/11/2026", true, "OK", true,
                "08/2027", null, "INCENDIO", "NR 27", null, 0.9);
        ResultadoValidacao r = motor.validar(i, ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().contains("NR-27"), r.resumo());
    }

    @Test
    void riscoIncompativel_vaiParaRevisao() {
        ItemVistoria i = new ItemVistoria("EXTINTOR", null, null, null, "19/11/2026", true, "OK", true,
                "08/2027", null, "ESCORREGAMENTO", "NR-23", null, 0.9);
        ResultadoValidacao r = motor.validar(i, ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.REVISAO_MANUAL, r.status());
        assertEquals(ClassificacaoRisco.INCENDIO, r.risco(), "risco incompatível não é aceito; usa o padrão do tipo");
    }

    @Test
    void outraUnidade_vaiParaRevisao() {
        ItemVistoria i = new ItemVistoria("EXTINTOR", "Sabor & Cia - Olinda", null, null, "19/11/2026", true, "OK", true,
                "08/2027", null, "INCENDIO", "NR-23", null, 0.9);
        assertEquals(StatusItem.REVISAO_MANUAL, motor.validar(i, ctx(BOA, OrigemExtracao.LLM)).status());

        ItemVistoria mesma = new ItemVistoria("EXTINTOR", "SABOR & CIA BOA VIAGEM", null, null, "19/11/2026", true, "OK", true,
                "08/2027", null, "INCENDIO", "NR-23", null, 0.9);
        assertEquals(StatusItem.CONFORME, motor.validar(mesma, ctx(BOA, OrigemExtracao.LLM)).status());
    }

    @Test
    void extintorObstruido_reprova() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, "OBSTRUIDO", 0.9), ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
        assertTrue(r.resumo().contains("obstruído"));
    }

    @Test
    void tipoNaoIdentificado_reprova() {
        ItemVistoria i = new ItemVistoria(null, null, null, null, "19/11/2026", null, null, null, null, null, null, null, null, 0.3);
        ResultadoValidacao r = motor.validar(i, ctx(BOA, OrigemExtracao.LLM));
        assertEquals(StatusItem.NAO_CONFORME, r.status());
    }

    @Test
    void apontamentoExterno_entraNaDecisao() {
        ResultadoValidacao r = motor.validar(extintor("08/2027", true, "OK", 0.9), ctx(BOA, OrigemExtracao.LLM),
                java.util.List.of(Apontamento.revisao("OCR_INDISPONIVEL", "x", "y")));
        assertEquals(StatusItem.REVISAO_MANUAL, r.status());
    }
}
