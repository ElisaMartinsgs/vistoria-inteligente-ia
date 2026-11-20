package br.com.saborecia.vistoria.dominio;

public enum Urgencia {
    BAIXA, MEDIA, ALTA, CRITICA;

    /**
     * Grau de urgência derivado do status e do risco (não confiamos só no que a IA disser).
     */
    public static Urgencia calcular(StatusItem status, ClassificacaoRisco risco, boolean temAlerta) {
        if (status == StatusItem.CONFORME) return temAlerta ? MEDIA : BAIXA;
        if (status == StatusItem.REVISAO_MANUAL) return MEDIA;
        if (risco == ClassificacaoRisco.INCENDIO || risco == ClassificacaoRisco.ELETRICO) return CRITICA;
        return ALTA;
    }
}
