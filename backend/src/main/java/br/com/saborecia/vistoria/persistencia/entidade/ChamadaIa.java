package br.com.saborecia.vistoria.persistencia.entidade;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Auditoria de uma chamada ao LLM: permite medir custo (tokens), latência e taxa de falha. */
@Entity
@Table(name = "chamada_ia")
public class ChamadaIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private ItemVerificado item;

    @Column(nullable = false) private String provedor;
    @Column(nullable = false) private String modelo;
    @Column(name = "versao_prompt", nullable = false) private String versaoPrompt;
    @Column(nullable = false) private boolean sucesso;
    private String erro;
    @Column(nullable = false) private int tentativas;
    @Column(name = "latencia_ms", nullable = false) private long latenciaMs;
    @Column(name = "tokens_entrada") private Integer tokensEntrada;
    @Column(name = "tokens_saida") private Integer tokensSaida;
    @Column(name = "resposta_bruta", columnDefinition = "TEXT") private String respostaBruta;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    protected ChamadaIa() {}

    public ChamadaIa(String provedor, String modelo, String versaoPrompt, boolean sucesso, String erro, int tentativas,
                     long latenciaMs, Integer tokensEntrada, Integer tokensSaida, String respostaBruta) {
        this.provedor = provedor;
        this.modelo = modelo;
        this.versaoPrompt = versaoPrompt;
        this.sucesso = sucesso;
        this.erro = MotivoValidacao.cortar(erro, 500);
        this.tentativas = tentativas;
        this.latenciaMs = latenciaMs;
        this.tokensEntrada = tokensEntrada;
        this.tokensSaida = tokensSaida;
        this.respostaBruta = respostaBruta;
    }

    void setItem(ItemVerificado item) { this.item = item; }
    public Long getId() { return id; }
    public String getProvedor() { return provedor; }
    public String getModelo() { return modelo; }
    public String getVersaoPrompt() { return versaoPrompt; }
    public boolean isSucesso() { return sucesso; }
    public String getErro() { return erro; }
    public int getTentativas() { return tentativas; }
    public long getLatenciaMs() { return latenciaMs; }
    public Integer getTokensEntrada() { return tokensEntrada; }
    public Integer getTokensSaida() { return tokensSaida; }
}
