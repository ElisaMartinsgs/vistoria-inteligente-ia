package br.com.saborecia.vistoria.persistencia.entidade;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Uma visita do técnico a uma unidade: agrupa os itens do checklist. */
@Entity
@Table(name = "vistoria")
public class Vistoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id")
    private Unidade unidade;

    @Column(nullable = false)
    private String tecnico;

    @Column(name = "data_vistoria", nullable = false)
    private LocalDate dataVistoria;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "vistoria", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ItemVerificado> itens = new ArrayList<>();

    protected Vistoria() {}

    public Vistoria(Unidade unidade, String tecnico, LocalDate dataVistoria) {
        this.unidade = unidade;
        this.tecnico = tecnico;
        this.dataVistoria = dataVistoria;
    }

    public void adicionar(ItemVerificado item) {
        item.setVistoria(this);
        itens.add(item);
    }

    public Long getId() { return id; }
    public Unidade getUnidade() { return unidade; }
    public String getTecnico() { return tecnico; }
    public LocalDate getDataVistoria() { return dataVistoria; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<ItemVerificado> getItens() { return itens; }
}
