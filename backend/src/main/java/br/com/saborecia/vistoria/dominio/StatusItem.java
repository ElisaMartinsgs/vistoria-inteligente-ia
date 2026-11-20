package br.com.saborecia.vistoria.dominio;

/**
 * Parecer final de um item vistoriado.
 */
public enum StatusItem {
    /** Todas as regras atendidas: segue sem conferência manual. */
    CONFORME,
    /** Pelo menos uma regra eliminatória falhou (item vencido, ausente, imagem ilegível...). */
    NAO_CONFORME,
    /** Caso duvidoso: a máquina não tem segurança para decidir, vai para um supervisor. */
    REVISAO_MANUAL
}
