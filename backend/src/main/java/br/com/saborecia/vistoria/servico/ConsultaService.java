package br.com.saborecia.vistoria.servico;

import br.com.saborecia.vistoria.api.dto.NormaResponse;
import br.com.saborecia.vistoria.api.dto.PainelResponse;
import br.com.saborecia.vistoria.api.dto.RevisaoRequest;
import br.com.saborecia.vistoria.api.dto.TipoItemResponse;
import br.com.saborecia.vistoria.api.dto.UnidadeResponse;
import br.com.saborecia.vistoria.api.dto.ValidacaoItemResponse;
import br.com.saborecia.vistoria.api.dto.VistoriaResponse;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.excecao.RecursoNaoEncontradoException;
import br.com.saborecia.vistoria.excecao.RegraDeNegocioException;
import br.com.saborecia.vistoria.persistencia.entidade.ItemVerificado;
import br.com.saborecia.vistoria.persistencia.repositorio.ItemVerificadoRepository;
import br.com.saborecia.vistoria.persistencia.repositorio.NormaRepository;
import br.com.saborecia.vistoria.persistencia.repositorio.UnidadeRepository;
import br.com.saborecia.vistoria.persistencia.repositorio.VistoriaRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class ConsultaService {

    private final UnidadeRepository unidades;
    private final NormaRepository normas;
    private final VistoriaRepository vistorias;
    private final ItemVerificadoRepository itens;
    private final ItemMapper mapper;
    private final ArmazenamentoService armazenamento;

    public ConsultaService(UnidadeRepository unidades, NormaRepository normas, VistoriaRepository vistorias,
                           ItemVerificadoRepository itens, ItemMapper mapper, ArmazenamentoService armazenamento) {
        this.unidades = unidades;
        this.normas = normas;
        this.vistorias = vistorias;
        this.itens = itens;
        this.mapper = mapper;
        this.armazenamento = armazenamento;
    }

    public List<UnidadeResponse> unidades() {
        return unidades.findByAtivaTrueOrderByNomeAsc().stream()
                .map(u -> new UnidadeResponse(u.getId(), u.getNome(), u.getEndereco(), u.getCidade(), u.getUf()))
                .toList();
    }

    public List<NormaResponse> normas() {
        return normas.findAll(Sort.by("codigo")).stream()
                .map(n -> new NormaResponse(n.getCodigo(), n.getTitulo(), n.isAtiva(), n.getObservacao()))
                .toList();
    }

    public List<TipoItemResponse> tiposItem() {
        return Arrays.stream(TipoItem.values())
                .map(t -> new TipoItemResponse(t.name(), t.nomeAmigavel(), t.normaPadrao(), t.exigeValidade()))
                .toList();
    }

    public List<VistoriaResponse> vistorias(Long unidadeId) {
        var lista = unidadeId == null ? vistorias.findTop50ByOrderByIdDesc() : vistorias.findTop50ByUnidadeIdOrderByIdDesc(unidadeId);
        return lista.stream().map(mapper::paraResposta).toList();
    }

    public VistoriaResponse vistoria(Long id) {
        return vistorias.findWithItensById(id).map(mapper::paraResposta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vistoria", id));
    }

    public ValidacaoItemResponse item(Long id) {
        ItemVerificado i = carregar(id);
        return mapper.paraResposta(i, i.getVistoria().getUnidade().getNome(), null);
    }

    public List<ValidacaoItemResponse> itensPorStatus(StatusItem status) {
        return itens.findByStatusOrderByIdAsc(status).stream()
                .map(i -> mapper.paraResposta(i, i.getVistoria().getUnidade().getNome(), null))
                .toList();
    }

    public record Arquivo(Resource conteudo, String tipo, String nome) {}

    public Arquivo arquivo(Long itemId) {
        ItemVerificado i = carregar(itemId);
        return new Arquivo(armazenamento.abrir(i.getArquivoCaminho()), i.getArquivoTipo(), i.getArquivoNome());
    }

    /** Supervisor decide um caso duvidoso (ou contesta um parecer automático). */
    @Transactional
    public ValidacaoItemResponse revisar(Long itemId, RevisaoRequest req) {
        if (req.status() == StatusItem.REVISAO_MANUAL) {
            throw new RegraDeNegocioException("A decisão do supervisor deve ser CONFORME ou NAO_CONFORME.");
        }
        ItemVerificado i = carregar(itemId);
        if (i.getRevisadoPor() != null) {
            throw new RegraDeNegocioException("Este item já foi revisado por " + i.getRevisadoPor() + ".");
        }
        i.registrarRevisao(req.status(), req.supervisor().strip(), req.observacao(), LocalDateTime.now());
        return mapper.paraResposta(i, i.getVistoria().getUnidade().getNome(), null);
    }

    public PainelResponse painel() {
        Map<StatusItem, Long> porStatus = new EnumMap<>(StatusItem.class);
        for (Object[] r : itens.contarPorStatus()) porStatus.put((StatusItem) r[0], (Long) r[1]);
        long conformes = porStatus.getOrDefault(StatusItem.CONFORME, 0L);
        long nao = porStatus.getOrDefault(StatusItem.NAO_CONFORME, 0L);
        long revisao = porStatus.getOrDefault(StatusItem.REVISAO_MANUAL, 0L);
        long total = conformes + nao + revisao;

        List<PainelResponse.MotivoFrequente> motivos = itens.principaisMotivosDeReprovacao().stream()
                .limit(5)
                .map(r -> new PainelResponse.MotivoFrequente((String) r[0], (Long) r[1]))
                .toList();

        Map<String, long[]> porUnidade = new LinkedHashMap<>(); // [total, conformes]
        for (Object[] r : itens.contarPorUnidadeEStatus()) {
            long[] acc = porUnidade.computeIfAbsent((String) r[0], k -> new long[2]);
            acc[0] += (Long) r[2];
            if (r[1] == StatusItem.CONFORME) acc[1] += (Long) r[2];
        }
        List<PainelResponse.PorUnidade> unidadesResumo = porUnidade.entrySet().stream()
                .map(e -> new PainelResponse.PorUnidade(e.getKey(), e.getValue()[0], e.getValue()[1],
                        taxa(e.getValue()[1], e.getValue()[0])))
                .toList();

        return new PainelResponse(total, conformes, nao, revisao, taxa(conformes, total), motivos, unidadesResumo);
    }

    private static double taxa(long parte, long total) {
        return total == 0 ? 0 : Math.round(1000.0 * parte / total) / 10.0;
    }

    private ItemVerificado carregar(Long id) {
        return itens.findDetalhadoById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Item", id));
    }
}
