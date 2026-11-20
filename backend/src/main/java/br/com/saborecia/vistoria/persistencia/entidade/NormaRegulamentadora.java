package br.com.saborecia.vistoria.persistencia.entidade;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "norma_regulamentadora")
public class NormaRegulamentadora {

    @Id
    private String codigo;
    private String titulo;
    private boolean ativa;
    private String observacao;

    protected NormaRegulamentadora() {}

    public NormaRegulamentadora(String codigo, String titulo, boolean ativa, String observacao) {
        this.codigo = codigo;
        this.titulo = titulo;
        this.ativa = ativa;
        this.observacao = observacao;
    }

    public String getCodigo() { return codigo; }
    public String getTitulo() { return titulo; }
    public boolean isAtiva() { return ativa; }
    public String getObservacao() { return observacao; }
}
