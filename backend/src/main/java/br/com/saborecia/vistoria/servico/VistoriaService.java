package br.com.saborecia.vistoria.servico;

import br.com.saborecia.vistoria.api.dto.ValidacaoItemResponse;
import br.com.saborecia.vistoria.config.VistoriaProperties;
import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.OrigemExtracao;
import br.com.saborecia.vistoria.dominio.QualidadeImagem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.dominio.regras.Apontamento;
import br.com.saborecia.vistoria.dominio.regras.MotorDeRegras;
import br.com.saborecia.vistoria.dominio.regras.ResultadoValidacao;
import br.com.saborecia.vistoria.excecao.ArquivoInvalidoException;
import br.com.saborecia.vistoria.excecao.OcrIndisponivelException;
import br.com.saborecia.vistoria.excecao.RecursoNaoEncontradoException;
import br.com.saborecia.vistoria.excecao.RegraDeNegocioException;
import br.com.saborecia.vistoria.infra.imagem.OpenCvPreprocessor;
import br.com.saborecia.vistoria.infra.ocr.TesseractOcrService;
import br.com.saborecia.vistoria.infra.pdf.PdfParaImagem;
import br.com.saborecia.vistoria.persistencia.entidade.ItemVerificado;
import br.com.saborecia.vistoria.persistencia.entidade.MotivoValidacao;
import br.com.saborecia.vistoria.persistencia.entidade.Unidade;
import br.com.saborecia.vistoria.persistencia.entidade.Vistoria;
import br.com.saborecia.vistoria.persistencia.repositorio.ItemVerificadoRepository;
import br.com.saborecia.vistoria.persistencia.repositorio.NormaRepository;
import br.com.saborecia.vistoria.persistencia.repositorio.UnidadeRepository;
import br.com.saborecia.vistoria.persistencia.repositorio.VistoriaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Orquestra o pipeline completo de um item:
 * <pre>
 *  arquivo → (PDF→imagem) → OpenCV → [legível?] → Tesseract → IA/Regex → Regras → PostgreSQL → resposta
 * </pre>
 * O processamento pesado roda FORA da transação; só a gravação final é transacional.
 */
@Service
public class VistoriaService {

    private static final Logger log = LoggerFactory.getLogger(VistoriaService.class);

    private final VistoriaProperties props;
    private final OpenCvPreprocessor opencv;
    private final TesseractOcrService ocr;
    private final PdfParaImagem pdf;
    private final ExtracaoService extracao;
    private final MotorDeRegras motor;
    private final ArmazenamentoService armazenamento;
    private final UnidadeRepository unidades;
    private final VistoriaRepository vistorias;
    private final NormaRepository normas;
    private final ItemVerificadoRepository itens;
    private final ItemMapper mapper;
    private final ObjectMapper json;
    private final TransactionTemplate tx;
    private final Supplier<LocalDate> hoje;

    public VistoriaService(VistoriaProperties props, OpenCvPreprocessor opencv, TesseractOcrService ocr,
                           PdfParaImagem pdf, ExtracaoService extracao, MotorDeRegras motor,
                           ArmazenamentoService armazenamento, UnidadeRepository unidades,
                           VistoriaRepository vistorias, NormaRepository normas, ItemVerificadoRepository itens,
                           ItemMapper mapper,
                           ObjectMapper json, TransactionTemplate tx, Supplier<LocalDate> hoje) {
        this.props = props;
        this.opencv = opencv;
        this.ocr = ocr;
        this.pdf = pdf;
        this.extracao = extracao;
        this.motor = motor;
        this.armazenamento = armazenamento;
        this.unidades = unidades;
        this.vistorias = vistorias;
        this.normas = normas;
        this.itens = itens;
        this.mapper = mapper;
        this.json = json;
        this.tx = tx;
        this.hoje = hoje;
    }

    /** Dados de entrada do endpoint POST /api/vistorias/validar. */
    public record Comando(byte[] conteudo, String nomeArquivo, Long unidadeId, Long vistoriaId,
                          String tecnico, TipoItem tipoEsperado, String localizacao) {}

    public Vistoria criarVistoria(Long unidadeId, String tecnico) {
        return tx.execute(s -> {
            Unidade u = unidades.findById(unidadeId).orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", unidadeId));
            return vistorias.save(new Vistoria(u, tecnico.strip(), hoje.get()));
        });
    }

    public ValidacaoItemResponse validar(Comando cmd) {
        long inicio = System.currentTimeMillis();
        LocalDate dataHoje = hoje.get();

        // 0) Entrada
        TipoArquivo tipoArquivo = detectarTipo(cmd.conteudo());
        Unidade unidade = unidades.findById(cmd.unidadeId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade", cmd.unidadeId()));
        if (cmd.vistoriaId() != null) {
            Long unidadeDaVistoria = tx.execute(s -> vistorias.findById(cmd.vistoriaId())
                    .map(v -> v.getUnidade().getId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Vistoria", cmd.vistoriaId())));
            if (!unidade.getId().equals(unidadeDaVistoria)) {
                throw new RegraDeNegocioException("A vistoria " + cmd.vistoriaId() + " pertence a outra unidade.");
            }
        }
        byte[] imagem = tipoArquivo == TipoArquivo.PDF ? pdf.primeiraPagina(cmd.conteudo()) : cmd.conteudo();

        // 1) OpenCV
        OpenCvPreprocessor.ImagemProcessada proc = opencv.processar(imagem);
        Set<String> normasAtivas = normas.codigosAtivos();
        ContextoValidacao ctxBase = contexto(unidade, cmd.tipoEsperado(), normasAtivas, proc.qualidade(), null,
                OrigemExtracao.NENHUMA, dataHoje);

        ItemVistoria item;
        OrigemExtracao origem;
        ExtracaoService.Resultado ext = null;
        String textoOcr = null;
        Double confOcr = null;
        List<Apontamento> extras = new ArrayList<>();

        boolean ilegivel = motor.avaliarLegibilidade(ctxBase).stream()
                .anyMatch(a -> a.severidade() == Apontamento.Severidade.REPROVA);
        if (ilegivel) {
            // Não gastamos OCR nem IA com uma foto que já sabemos ser ilegível.
            item = ItemVistoria.vazio();
            origem = OrigemExtracao.NENHUMA;
        } else {
            // 2) Tesseract
            try {
                TesseractOcrService.ResultadoOcr r = ocr.extrairTexto(proc.paraOcr());
                textoOcr = r.texto();
                confOcr = r.confiancaMedia();
            } catch (OcrIndisponivelException e) {
                log.error("OCR indisponível: {}", e.getMessage());
                extras.add(Apontamento.revisao("OCR_INDISPONIVEL", e.getMessage(),
                        "A leitura do texto falhou; um supervisor vai conferir este item."));
            }
            // 3) IA (ou Regex)
            ext = extracao.extrair(textoOcr, proc.jpegColorido(), cmd.tipoEsperado(), cmd.localizacao(),
                    unidade.getNome(), dataHoje);
            origem = ext.origem();
            item = ext.item();
        }

        // 4) Regras (Java)
        item = item.completarCom(unidade.getNome(), cmd.tecnico(), dataHoje.toString(), cmd.localizacao());
        ContextoValidacao ctx = contexto(unidade, cmd.tipoEsperado(), normasAtivas, proc.qualidade(), confOcr, origem, dataHoje);
        ResultadoValidacao resultado = motor.validar(item, ctx, extras);
        long tempo = System.currentTimeMillis() - inicio;
        log.info("Item validado: unidade={} tipo={} status={} origem={} {}ms",
                unidade.getNome(), resultado.tipoItem(), resultado.status(), origem, tempo);

        // 5) Persistência
        String caminho = armazenamento.salvar(cmd.conteudo(), tipoArquivo.extensao);
        ItemVerificado entidade = montarEntidade(cmd, tipoArquivo, caminho, proc.qualidade(), textoOcr, confOcr,
                origem, item, ext, resultado, tempo, normasAtivas);
        ItemVerificado salvo = tx.execute(s -> {
            Vistoria v = cmd.vistoriaId() != null
                    ? vistorias.findById(cmd.vistoriaId()).orElseThrow()
                    : vistorias.save(new Vistoria(unidade, cmd.tecnico().strip(), dataHoje));
            v.adicionar(entidade);
            // persist direto no item (é novo): evita o merge da vistoria, que criaria uma cópia sem id
            return itens.save(entidade);
        });
        return mapper.paraResposta(salvo, unidade.getNome(), resultado.regrasAplicadas());
    }

    private ContextoValidacao contexto(Unidade u, TipoItem esperado, Set<String> normasAtivas, QualidadeImagem q,
                                       Double confOcr, OrigemExtracao origem, LocalDate dataHoje) {
        VistoriaProperties.Imagem im = props.imagem();
        return new ContextoValidacao(dataHoje, u.getNome(), esperado, normasAtivas, q,
                new QualidadeImagem.Limites(im.nitidezMinima(), im.brilhoMinimo(), im.brilhoMaximo(), im.contrasteMinimo()),
                confOcr, origem, props.ia().confiancaMinima(), props.regras().diasAlertaVencimento());
    }

    private ItemVerificado montarEntidade(Comando cmd, TipoArquivo tipoArquivo, String caminho, QualidadeImagem q,
                                          String textoOcr, Double confOcr, OrigemExtracao origem, ItemVistoria item,
                                          ExtracaoService.Resultado ext, ResultadoValidacao r, long tempo,
                                          Set<String> normasAtivas) {
        ItemVerificado e = new ItemVerificado();
        e.setTipoEsperado(cmd.tipoEsperado());
        e.setTipoItem(r.tipoItem());
        e.setLocalizacao(item.localizacao());
        e.setStatus(r.status());
        e.setUrgencia(r.urgencia());
        e.setClassificacaoRisco(r.risco());
        // Só referencia normas cadastradas (FK). Norma desconhecida fica registrada no motivo.
        e.setNormaCodigo(r.norma() != null && normas.existsById(r.norma()) ? r.norma() : null);
        e.setDataValidade(r.dataValidade());
        e.setResumo(r.resumo());
        e.setArquivo(cmd.nomeArquivo() != null ? cmd.nomeArquivo() : "arquivo." + tipoArquivo.extensao,
                tipoArquivo.mime, caminho);
        e.setQualidade(q.nitidez(), q.brilho(), q.contraste());
        e.setOcr(textoOcr, confOcr);
        e.setOrigemExtracao(origem);
        e.setJsonExtraido(paraJson(item));
        e.setConfiancaExtracao(item.confianca());
        e.setTempoProcessamentoMs(tempo);
        for (Apontamento a : r.apontamentos()) {
            e.adicionarMotivo(new MotivoValidacao(a.codigoRegra(), a.severidade().name(), a.mensagemSimples(), a.detalheTecnico()));
        }
        if (ext != null && ext.chamada() != null) e.adicionarChamadaIa(ext.chamada());
        return e;
    }

    private String paraJson(ItemVistoria item) {
        try {
            return json.writeValueAsString(item);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    // ---- validação do arquivo pelos "magic bytes" (não confiamos na extensão nem no Content-Type) ----

    enum TipoArquivo {
        JPEG("jpg", "image/jpeg"), PNG("png", "image/png"), PDF("pdf", "application/pdf");
        final String extensao;
        final String mime;

        TipoArquivo(String extensao, String mime) {
            this.extensao = extensao;
            this.mime = mime;
        }
    }

    static TipoArquivo detectarTipo(byte[] b) {
        if (b == null || b.length == 0) throw new ArquivoInvalidoException("Nenhum arquivo foi enviado.");
        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) return TipoArquivo.JPEG;
        if (b.length >= 4 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G') return TipoArquivo.PNG;
        if (b.length >= 4 && b[0] == '%' && b[1] == 'P' && b[2] == 'D' && b[3] == 'F') return TipoArquivo.PDF;
        throw new ArquivoInvalidoException("Formato não suportado. Envie uma foto JPG/PNG ou um PDF.");
    }
}
