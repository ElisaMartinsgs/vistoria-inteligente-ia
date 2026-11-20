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

@Entity
@Table(name = "motivo_validacao")
public class MotivoValidacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id")
    private ItemVerificado item;

    @Column(name = "codigo_regra", nullable = false)
    private String codigoRegra;

    @Column(nullable = false)
    private String severidade;

    @Column(name = "mensagem_simples", nullable = false)
    private String mensagemSimples;

    @Column(name = "detalhe_tecnico")
    private String detalheTecnico;

    protected MotivoValidacao() {}

    public MotivoValidacao(String codigoRegra, String severidade, String mensagemSimples, String detalheTecnico) {
        this.codigoRegra = codigoRegra;
        this.severidade = severidade;
        this.mensagemSimples = cortar(mensagemSimples, 500);
        this.detalheTecnico = cortar(detalheTecnico, 500);
    }

    static String cortar(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max - 3) + "...";
    }

    void setItem(ItemVerificado item) { this.item = item; }
    public Long getId() { return id; }
    public String getCodigoRegra() { return codigoRegra; }
    public String getSeveridade() { return severidade; }
    public String getMensagemSimples() { return mensagemSimples; }
    public String getDetalheTecnico() { return detalheTecnico; }
}
