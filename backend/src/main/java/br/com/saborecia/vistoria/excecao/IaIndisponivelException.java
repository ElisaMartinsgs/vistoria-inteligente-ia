package br.com.saborecia.vistoria.excecao;

/**
 * Falha temporária do serviço de IA: timeout, erro de conexão, HTTP 429 (limite de uso) ou 5xx,
 * mesmo depois das novas tentativas. Não chega ao usuário: o sistema aciona o fallback por Regex.
 */
public class IaIndisponivelException extends RuntimeException {
    private final int tentativas;

    public IaIndisponivelException(String mensagem, int tentativas, Throwable causa) {
        super(mensagem, causa);
        this.tentativas = tentativas;
    }

    public int getTentativas() {
        return tentativas;
    }
}
