package br.com.saborecia.vistoria.dominio;

import java.util.List;
import java.util.Optional;

import static br.com.saborecia.vistoria.dominio.ClassificacaoRisco.*;

/**
 * Tipos de item de segurança que a vistoria reconhece.
 * Cada tipo sabe se exige validade, quais riscos são compatíveis com ele e qual NR o rege.
 */
public enum TipoItem {
    EXTINTOR(true, true, List.of(INCENDIO), "NR-23",
            List.of("EXTINTOR", "EXTINGUIDOR", "PQS", "CO2", "AGUA PRESSURIZADA", "PO QUIMICO", "CARGA", "RECARGA")),
    SAIDA_EMERGENCIA(false, false, List.of(INCENDIO, SINALIZACAO), "NR-23",
            List.of("SAIDA DE EMERGENCIA", "SAIDA", "ROTA DE FUGA", "EXIT")),
    ILUMINACAO_EMERGENCIA(true, false, List.of(INCENDIO, ELETRICO), "NR-23",
            List.of("ILUMINACAO DE EMERGENCIA", "LUMINARIA", "BATERIA")),
    PISO_ANTIDERRAPANTE(false, false, List.of(ESCORREGAMENTO), "NR-8",
            List.of("PISO", "ANTIDERRAPANTE", "ESCORREGADIO")),
    SINALIZACAO_SEGURANCA(false, false, List.of(SINALIZACAO, INCENDIO, ELETRICO, ESCORREGAMENTO), "NR-26",
            List.of("SINALIZACAO", "PLACA", "CUIDADO", "ATENCAO", "PERIGO")),
    QUADRO_ELETRICO(false, false, List.of(ELETRICO), "NR-10",
            List.of("QUADRO ELETRICO", "QUADRO DE DISTRIBUICAO", "DISJUNTOR", "RISCO DE CHOQUE", "220V", "380V")),
    EQUIPAMENTO_COZINHA(false, false, List.of(MECANICO, ELETRICO), "NR-12",
            List.of("FATIADOR", "CORTADOR", "MOEDOR", "AMASSADEIRA", "PROTECAO", "BOTAO DE EMERGENCIA")),
    EPI(true, false, List.of(MECANICO, QUIMICO, ERGONOMICO), "NR-6",
            List.of("EPI", "LUVA", "AVENTAL", "BOTA", "CA N", "CERTIFICADO DE APROVACAO"));

    private final boolean podeTerValidade;
    private final boolean exigeValidade;
    private final List<ClassificacaoRisco> riscosCompativeis;
    private final String normaPadrao;
    private final List<String> palavrasChave;

    TipoItem(boolean podeTerValidade, boolean exigeValidade, List<ClassificacaoRisco> riscos,
             String normaPadrao, List<String> palavrasChave) {
        this.podeTerValidade = podeTerValidade;
        this.exigeValidade = exigeValidade;
        this.riscosCompativeis = riscos;
        this.normaPadrao = normaPadrao;
        this.palavrasChave = palavrasChave;
    }

    public boolean podeTerValidade() { return podeTerValidade; }
    /** Quando true, a ausência de data de validade/selo reprova o item (ex.: extintor). */
    public boolean exigeValidade() { return exigeValidade; }
    public List<ClassificacaoRisco> riscosCompativeis() { return riscosCompativeis; }
    public ClassificacaoRisco riscoPadrao() { return riscosCompativeis.get(0); }
    public String normaPadrao() { return normaPadrao; }
    public List<String> palavrasChave() { return palavrasChave; }

    public String nomeAmigavel() {
        return switch (this) {
            case EXTINTOR -> "extintor";
            case SAIDA_EMERGENCIA -> "saída de emergência";
            case ILUMINACAO_EMERGENCIA -> "iluminação de emergência";
            case PISO_ANTIDERRAPANTE -> "piso antiderrapante";
            case SINALIZACAO_SEGURANCA -> "sinalização de segurança";
            case QUADRO_ELETRICO -> "quadro elétrico";
            case EQUIPAMENTO_COZINHA -> "equipamento de cozinha";
            case EPI -> "EPI";
        };
    }

    /** Aceita "EXTINTOR", "Extintor de incêndio", "saida_emergencia"... */
    public static Optional<TipoItem> deTexto(String texto) {
        if (texto == null || texto.isBlank()) return Optional.empty();
        String t = ClassificacaoRisco.TextoUtil.normalizar(texto).replace('_', ' ');
        for (TipoItem tipo : values()) {
            if (t.equals(tipo.name().replace('_', ' '))) return Optional.of(tipo);
        }
        for (TipoItem tipo : values()) {
            String nome = tipo.name().replace('_', ' ');
            if (t.contains(nome)) return Optional.of(tipo);
        }
        if (t.contains("EXTINTOR")) return Optional.of(EXTINTOR);
        if (t.contains("SAIDA")) return Optional.of(SAIDA_EMERGENCIA);
        if (t.contains("PISO")) return Optional.of(PISO_ANTIDERRAPANTE);
        if (t.contains("ILUMINACAO")) return Optional.of(ILUMINACAO_EMERGENCIA);
        if (t.contains("QUADRO")) return Optional.of(QUADRO_ELETRICO);
        if (t.contains("SINALIZACAO") || t.contains("PLACA")) return Optional.of(SINALIZACAO_SEGURANCA);
        if (t.contains("EPI")) return Optional.of(EPI);
        return Optional.empty();
    }
}
