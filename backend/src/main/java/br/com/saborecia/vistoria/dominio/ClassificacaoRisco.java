package br.com.saborecia.vistoria.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public enum ClassificacaoRisco {
    INCENDIO,
    ESCORREGAMENTO,
    ELETRICO,
    MECANICO,
    QUIMICO,
    ERGONOMICO,
    SINALIZACAO;

    /** Converte o texto devolvido pela IA ("Incêndio", "risco elétrico"...) para o enum. */
    public static Optional<ClassificacaoRisco> deTexto(String texto) {
        if (texto == null || texto.isBlank()) return Optional.empty();
        String t = TextoUtil.normalizar(texto);
        for (ClassificacaoRisco r : values()) {
            if (t.contains(r.name())) return Optional.of(r);
        }
        if (t.contains("FOGO")) return Optional.of(INCENDIO);
        if (t.contains("QUEDA") || t.contains("ESCORREG")) return Optional.of(ESCORREGAMENTO);
        if (t.contains("CHOQUE") || t.contains("ELETR")) return Optional.of(ELETRICO);
        if (t.contains("CORTE") || t.contains("MAQUIN")) return Optional.of(MECANICO);
        return Optional.empty();
    }

    /** Utilitário interno de normalização de texto (sem acentos, maiúsculo). */
    public static final class TextoUtil {
        private TextoUtil() {}

        public static String normalizar(String s) {
            if (s == null) return "";
            String semAcento = Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
            return semAcento.toUpperCase(Locale.ROOT).trim();
        }
    }
}
