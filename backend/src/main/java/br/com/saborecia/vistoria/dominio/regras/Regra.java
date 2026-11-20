package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;

import java.util.List;

/**
 * Uma regra de negócio da vistoria. Cada regra é pequena, testável sozinha e devolve
 * zero ou mais apontamentos (lista vazia = regra atendida).
 */
public interface Regra {

    /** Código estável usado no banco e nos relatórios (ex.: "VALIDADE"). */
    String codigo();

    List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx);
}
