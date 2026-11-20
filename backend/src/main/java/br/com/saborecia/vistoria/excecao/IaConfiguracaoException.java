package br.com.saborecia.vistoria.excecao;

/**
 * Erro que nova tentativa não resolve: chave de API ausente/inválida (401/403), modelo inexistente (404),
 * requisição rejeitada (400). Também cai no fallback, mas é registrado como erro de configuração.
 */
public class IaConfiguracaoException extends RuntimeException {
    public IaConfiguracaoException(String mensagem) {
        super(mensagem);
    }
}
