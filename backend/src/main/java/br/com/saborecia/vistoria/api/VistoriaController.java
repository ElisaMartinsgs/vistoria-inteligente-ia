package br.com.saborecia.vistoria.api;

import br.com.saborecia.vistoria.api.dto.NovaVistoriaRequest;
import br.com.saborecia.vistoria.api.dto.RevisaoRequest;
import br.com.saborecia.vistoria.api.dto.ValidacaoItemResponse;
import br.com.saborecia.vistoria.api.dto.VistoriaResponse;
import br.com.saborecia.vistoria.dominio.StatusItem;
import br.com.saborecia.vistoria.dominio.TipoItem;
import br.com.saborecia.vistoria.excecao.ArquivoInvalidoException;
import br.com.saborecia.vistoria.persistencia.entidade.Vistoria;
import br.com.saborecia.vistoria.servico.ConsultaService;
import br.com.saborecia.vistoria.servico.VistoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api")
public class VistoriaController {

    private final VistoriaService vistoriaService;
    private final ConsultaService consultaService;

    public VistoriaController(VistoriaService vistoriaService, ConsultaService consultaService) {
        this.vistoriaService = vistoriaService;
        this.consultaService = consultaService;
    }

    /** Abre um checklist (vistoria) para uma unidade. Opcional: o validar cria um se não for informado. */
    @PostMapping("/vistorias")
    @ResponseStatus(HttpStatus.CREATED)
    public VistoriaResponse criar(@Valid @RequestBody NovaVistoriaRequest req) {
        Vistoria v = vistoriaService.criarVistoria(req.unidadeId(), req.tecnico());
        return consultaService.vistoria(v.getId());
    }

    /**
     * Endpoint principal do MVP: recebe a foto/PDF de um item e devolve CONFORME / NAO_CONFORME / REVISAO_MANUAL
     * com os motivos em linguagem simples.
     */
    @PostMapping(value = "/vistorias/validar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ValidacaoItemResponse validar(
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam("unidadeId") Long unidadeId,
            @RequestParam("tecnico") String tecnico,
            @RequestParam(value = "tipoItem", required = false) TipoItem tipoItem,
            @RequestParam(value = "localizacao", required = false) String localizacao,
            @RequestParam(value = "vistoriaId", required = false) Long vistoriaId) throws IOException {
        if (arquivo.isEmpty()) throw new ArquivoInvalidoException("Nenhum arquivo foi enviado.");
        if (tecnico == null || tecnico.isBlank()) throw new ArquivoInvalidoException("Informe o nome do técnico.");
        return vistoriaService.validar(new VistoriaService.Comando(arquivo.getBytes(), arquivo.getOriginalFilename(),
                unidadeId, vistoriaId, tecnico, tipoItem, localizacao == null || localizacao.isBlank() ? null : localizacao.strip()));
    }

    @GetMapping("/vistorias")
    public List<VistoriaResponse> listar(@RequestParam(value = "unidadeId", required = false) Long unidadeId) {
        return consultaService.vistorias(unidadeId);
    }

    @GetMapping("/vistorias/{id}")
    public VistoriaResponse buscar(@PathVariable Long id) {
        return consultaService.vistoria(id);
    }

    @GetMapping("/itens/{id}")
    public ValidacaoItemResponse item(@PathVariable Long id) {
        return consultaService.item(id);
    }

    /** Fila do supervisor: GET /api/itens?status=REVISAO_MANUAL */
    @GetMapping("/itens")
    public List<ValidacaoItemResponse> itens(@RequestParam(value = "status", defaultValue = "REVISAO_MANUAL") StatusItem status) {
        return consultaService.itensPorStatus(status);
    }

    @PatchMapping("/itens/{id}/revisao")
    public ValidacaoItemResponse revisar(@PathVariable Long id, @Valid @RequestBody RevisaoRequest req) {
        return consultaService.revisar(id, req);
    }

    /** Evidência original enviada pelo técnico. */
    @GetMapping("/itens/{id}/arquivo")
    public ResponseEntity<org.springframework.core.io.Resource> arquivo(@PathVariable Long id) {
        ConsultaService.Arquivo a = consultaService.arquivo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(a.tipo()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .body(a.conteudo());
    }
}
