package br.com.saborecia.vistoria.persistencia.entidade;

import br.com.saborecia.vistoria.dominio.ClassificacaoRisco;
import br.com.saborecia.vistoria.dominio.OrigemExtracao;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.Urgencia;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

/** Uma evidência (foto/PDF) enviada no checklist e o parecer do sistema sobre ela. */
@Entity
@Table(name = "item_verificado")
public class ItemVerificado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "vistoria_id")
    private Vistoria vistoria;

    @Enumerated(EnumType.STRING) @Column(name = "tipo_esperado") private TipoItem tipoEsperado;
    @Enumerated(EnumType.STRING) @Column(name = "tipo_item") private TipoItem tipoItem;
    private String localizacao;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private StatusItem status;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Urgencia urgencia;
    @Enumerated(EnumType.STRING) @Column(name = "classificacao_risco") private ClassificacaoRisco classificacaoRisco;
    @Column(name = "norma_codigo") private String normaCodigo;
    @Column(name = "data_validade") private LocalDate dataValidade;
    @Column(nullable = false, length = 1000) private String resumo;

    @Column(name = "arquivo_nome", nullable = false) private String arquivoNome;
    @Column(name = "arquivo_tipo", nullable = false) private String arquivoTipo;
    @Column(name = "arquivo_caminho", nullable = false) private String arquivoCaminho;

    private Double nitidez;
    private Double brilho;
    private Double contraste;

    @Column(name = "texto_ocr", columnDefinition = "TEXT") private String textoOcr;
    @Column(name = "confianca_ocr") private Double confiancaOcr;

    @Enumerated(EnumType.STRING) @Column(name = "origem_extracao", nullable = false) private OrigemExtracao origemExtracao;
    @Column(name = "json_extraido", columnDefinition = "TEXT") private String jsonExtraido;
    @Column(name = "confianca_extracao") private Double confiancaExtracao;
    @Column(name = "tempo_processamento_ms", nullable = false) private long tempoProcessamentoMs;

    @Enumerated(EnumType.STRING) @Column(name = "status_automatico", nullable = false) private StatusItem statusAutomatico;
    @Column(name = "revisado_por") private String revisadoPor;
    @Column(name = "observacao_revisao", length = 1000) private String observacaoRevisao;
    @Column(name = "revisado_em") private LocalDateTime revisadoEm;
    @Column(name = "criado_em", nullable = false) private LocalDateTime criadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<MotivoValidacao> motivos = new ArrayList<>();

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<ChamadaIa> chamadasIa = new ArrayList<>();

    public ItemVerificado() {}

    public void adicionarMotivo(MotivoValidacao m) {
        m.setItem(this);
        motivos.add(m);
    }

    public void adicionarChamadaIa(ChamadaIa c) {
        c.setItem(this);
        chamadasIa.add(c);
    }

    /** Decisão do supervisor: altera o status final, mas preserva o parecer automático. */
    public void registrarRevisao(StatusItem novoStatus, String supervisor, String observacao, LocalDateTime quando) {
        this.status = novoStatus;
        this.revisadoPor = supervisor;
        this.observacaoRevisao = observacao;
        this.revisadoEm = quando;
    }

    // ---- getters/setters ----
    public Long getId() { return id; }
    public Vistoria getVistoria() { return vistoria; }
    void setVistoria(Vistoria v) { this.vistoria = v; }
    public TipoItem getTipoEsperado() { return tipoEsperado; }
    public void setTipoEsperado(TipoItem t) { this.tipoEsperado = t; }
    public TipoItem getTipoItem() { return tipoItem; }
    public void setTipoItem(TipoItem t) { this.tipoItem = t; }
    public String getLocalizacao() { return localizacao; }
    public void setLocalizacao(String l) { this.localizacao = l; }
    public StatusItem getStatus() { return status; }
    public void setStatus(StatusItem s) { this.status = s; this.statusAutomatico = s; }
    public Urgencia getUrgencia() { return urgencia; }
    public void setUrgencia(Urgencia u) { this.urgencia = u; }
    public ClassificacaoRisco getClassificacaoRisco() { return classificacaoRisco; }
    public void setClassificacaoRisco(ClassificacaoRisco r) { this.classificacaoRisco = r; }
    public String getNormaCodigo() { return normaCodigo; }
    public void setNormaCodigo(String n) { this.normaCodigo = n; }
    public LocalDate getDataValidade() { return dataValidade; }
    public void setDataValidade(LocalDate d) { this.dataValidade = d; }
    public String getResumo() { return resumo; }
    public void setResumo(String r) { this.resumo = MotivoValidacao.cortar(r, 1000); }
    public String getArquivoNome() { return arquivoNome; }
    public String getArquivoTipo() { return arquivoTipo; }
    public String getArquivoCaminho() { return arquivoCaminho; }
    public void setArquivo(String nome, String tipo, String caminho) {
        this.arquivoNome = MotivoValidacao.cortar(nome, 200);
        this.arquivoTipo = tipo;
        this.arquivoCaminho = caminho;
    }
    public Double getNitidez() { return nitidez; }
    public Double getBrilho() { return brilho; }
    public Double getContraste() { return contraste; }
    public void setQualidade(Double nitidez, Double brilho, Double contraste) {
        this.nitidez = nitidez;
        this.brilho = brilho;
        this.contraste = contraste;
    }
    public String getTextoOcr() { return textoOcr; }
    public Double getConfiancaOcr() { return confiancaOcr; }
    public void setOcr(String texto, Double confianca) { this.textoOcr = texto; this.confiancaOcr = confianca; }
    public OrigemExtracao getOrigemExtracao() { return origemExtracao; }
    public void setOrigemExtracao(OrigemExtracao o) { this.origemExtracao = o; }
    public String getJsonExtraido() { return jsonExtraido; }
    public void setJsonExtraido(String j) { this.jsonExtraido = j; }
    public Double getConfiancaExtracao() { return confiancaExtracao; }
    public void setConfiancaExtracao(Double c) { this.confiancaExtracao = c; }
    public long getTempoProcessamentoMs() { return tempoProcessamentoMs; }
    public void setTempoProcessamentoMs(long t) { this.tempoProcessamentoMs = t; }
    public StatusItem getStatusAutomatico() { return statusAutomatico; }
    public String getRevisadoPor() { return revisadoPor; }
    public String getObservacaoRevisao() { return observacaoRevisao; }
    public LocalDateTime getRevisadoEm() { return revisadoEm; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public List<MotivoValidacao> getMotivos() { return motivos; }
    public List<ChamadaIa> getChamadasIa() { return chamadasIa; }
}
