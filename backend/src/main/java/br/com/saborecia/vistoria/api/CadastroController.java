package br.com.saborecia.vistoria.api;

import br.com.saborecia.vistoria.api.dto.NormaResponse;
import br.com.saborecia.vistoria.api.dto.PainelResponse;
import br.com.saborecia.vistoria.api.dto.TipoItemResponse;
import br.com.saborecia.vistoria.api.dto.UnidadeResponse;
import br.com.saborecia.vistoria.servico.ConsultaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Dados de apoio para o front-end e o painel de compliance. */
@RestController
@RequestMapping("/api")
public class CadastroController {

    private final ConsultaService consulta;

    public CadastroController(ConsultaService consulta) {
        this.consulta = consulta;
    }

    @GetMapping("/unidades")
    public List<UnidadeResponse> unidades() {
        return consulta.unidades();
    }

    @GetMapping("/normas")
    public List<NormaResponse> normas() {
        return consulta.normas();
    }

    @GetMapping("/tipos-item")
    public List<TipoItemResponse> tiposItem() {
        return consulta.tiposItem();
    }

    @GetMapping("/painel")
    public PainelResponse painel() {
        return consulta.painel();
    }
}
