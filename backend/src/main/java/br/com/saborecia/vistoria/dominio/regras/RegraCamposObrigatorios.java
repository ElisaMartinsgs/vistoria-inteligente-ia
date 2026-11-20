package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.DataParser;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.TipoItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Regra 1: todos os campos obrigatórios foram encontrados (tipo do item, unidade, data da vistoria, status).
 */
public class RegraCamposObrigatorios implements Regra {

    public static final String CODIGO = "CAMPOS_OBRIGATORIOS";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        List<Apontamento> out = new ArrayList<>();

        if (item.tipoItem() == null) {
            out.add(Apontamento.reprova(CODIGO, "Campo tipoItem retornou null",
                    "Não conseguimos identificar qual item aparece na foto. Envie outra foto mostrando o item inteiro."));
        } else if (TipoItem.deTexto(item.tipoItem()).isEmpty()) {
            out.add(Apontamento.reprova(CODIGO, "tipoItem inválido: '" + item.tipoItem() + "'",
                    "O item da foto (" + item.tipoItem() + ") não faz parte do checklist de segurança."));
        }

        boolean semUnidade = vazio(item.unidade()) && vazio(ctx.unidadeCadastro());
        if (semUnidade) {
            out.add(Apontamento.reprova(CODIGO, "Unidade ausente no documento e no cadastro",
                    "Selecione a unidade (restaurante) da vistoria."));
        }

        if (vazio(item.dataVistoria())) {
            out.add(Apontamento.reprova(CODIGO, "Campo dataVistoria retornou null",
                    "A data da vistoria não foi informada."));
        } else {
            Optional<LocalDate> dv = DataParser.parse(item.dataVistoria());
            if (dv.isEmpty()) {
                out.add(Apontamento.reprova(CODIGO, "dataVistoria em formato inválido: '" + item.dataVistoria() + "'",
                        "A data da vistoria está em um formato que não reconhecemos."));
            } else if (dv.get().isAfter(ctx.dataReferencia().plusDays(1))) {
                out.add(Apontamento.revisao(CODIGO, "dataVistoria no futuro: " + dv.get(),
                        "A data da vistoria está no futuro; um supervisor vai conferir."));
            }
        }

        if (item.itemPresente() == null) {
            out.add(Apontamento.revisao(CODIGO, "Campo itemPresente retornou null",
                    "Não ficou claro se o item aparece na foto; um supervisor vai conferir."));
        }
        return out;
    }

    static boolean vazio(String s) {
        return s == null || s.isBlank();
    }
}
