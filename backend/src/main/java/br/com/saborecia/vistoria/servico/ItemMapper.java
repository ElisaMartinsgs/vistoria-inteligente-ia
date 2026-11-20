package br.com.saborecia.vistoria.servico;

import br.com.saborecia.vistoria.api.dto.ValidacaoItemResponse;
import br.com.saborecia.vistoria.api.dto.VistoriaResponse;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.persistencia.entidade.ItemVerificado;
import br.com.saborecia.vistoria.persistencia.entidade.Vistoria;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ItemMapper {

    private final ObjectMapper json;

    public ItemMapper(ObjectMapper json) {
        this.json = json;
    }

    public ValidacaoItemResponse paraResposta(ItemVerificado i, String unidade, List<String> regrasAplicadas) {
        List<ValidacaoItemResponse.MotivoResponse> motivos = i.getMotivos().stream()
                .map(m -> new ValidacaoItemResponse.MotivoResponse(m.getCodigoRegra(), m.getSeveridade(),
                        m.getMensagemSimples(), m.getDetalheTecnico()))
                .toList();
        ValidacaoItemResponse.Pipeline pipeline = new ValidacaoItemResponse.Pipeline(
                i.getNitidez(), i.getBrilho(), i.getContraste(),
                i.getTextoOcr(), i.getConfiancaOcr(),
                i.getOrigemExtracao(), lerJson(i.getJsonExtraido()), i.getConfiancaExtracao(),
                regrasAplicadas, i.getTempoProcessamentoMs());
        ValidacaoItemResponse.Revisao revisao = i.getRevisadoPor() == null ? null
                : new ValidacaoItemResponse.Revisao(i.getRevisadoPor(), i.getObservacaoRevisao(), i.getRevisadoEm());
        return new ValidacaoItemResponse(
                i.getId(), i.getVistoria().getId(), unidade,
                i.getStatus(), i.getStatusAutomatico(), i.getUrgencia(), i.getResumo(), motivos,
                i.getTipoEsperado(), i.getTipoItem(), i.getLocalizacao(), i.getDataValidade(),
                i.getClassificacaoRisco(), i.getNormaCodigo(), pipeline, revisao, i.getCriadoEm());
    }

    public VistoriaResponse paraResposta(Vistoria v) {
        List<VistoriaResponse.ItemResumo> itens = v.getItens().stream()
                .map(i -> new VistoriaResponse.ItemResumo(i.getId(), i.getTipoItem(), i.getTipoEsperado(),
                        i.getLocalizacao(), i.getStatus(), i.getUrgencia(), i.getResumo(), i.getCriadoEm()))
                .toList();
        long conformes = itens.stream().filter(i -> i.status() == StatusItem.CONFORME).count();
        long nao = itens.stream().filter(i -> i.status() == StatusItem.NAO_CONFORME).count();
        long revisao = itens.stream().filter(i -> i.status() == StatusItem.REVISAO_MANUAL).count();
        StatusItem geral = itens.isEmpty() ? null
                : nao > 0 ? StatusItem.NAO_CONFORME
                : revisao > 0 ? StatusItem.REVISAO_MANUAL
                : StatusItem.CONFORME;
        return new VistoriaResponse(v.getId(), v.getUnidade().getId(), v.getUnidade().getNome(), v.getTecnico(),
                v.getDataVistoria(), geral, itens.size(), conformes, nao, revisao, itens);
    }

    private ItemVistoria lerJson(String s) {
        if (s == null) return null;
        try {
            return json.readValue(s, ItemVistoria.class);
        } catch (Exception e) {
            return null;
        }
    }
}
