package br.com.saborecia.vistoria.dominio.regras;

import br.com.saborecia.vistoria.dominio.ContextoValidacao;
import br.com.saborecia.vistoria.dominio.ItemVistoria;
import br.com.saborecia.vistoria.dominio.QualidadeImagem;

import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;

/**
 * Regra 5 do documento: foto legível. Usa as métricas do OpenCV (nitidez, brilho e contraste).
 * Se reprovar, o motor para aqui: não faz sentido validar dados lidos de uma foto ilegível.
 */
public class RegraLegibilidade implements Regra {

    public static final String CODIGO = "LEGIBILIDADE";

    @Override
    public String codigo() { return CODIGO; }

    @Override
    public List<Apontamento> avaliar(ItemVistoria item, ContextoValidacao ctx) {
        List<Apontamento> out = new ArrayList<>();
        QualidadeImagem q = ctx.qualidade();
        if (q == null) return out;
        QualidadeImagem.Limites lim = ctx.limites();

        if (q.nitidez() < lim.nitidezMinima()) {
            out.add(Apontamento.reprova(CODIGO,
                    format(java.util.Locale.ROOT, "Nitidez %.1f abaixo do mínimo %.1f (variância do Laplaciano)", q.nitidez(), lim.nitidezMinima()),
                    "A foto está tremida ou borrada. Segure o celular firme e envie outra foto."));
        }
        if (q.brilho() < lim.brilhoMinimo()) {
            out.add(Apontamento.reprova(CODIGO,
                    format(java.util.Locale.ROOT, "Brilho médio %.1f abaixo do mínimo %.1f", q.brilho(), lim.brilhoMinimo()),
                    "A foto está muito escura. Acenda a luz ou use o flash e envie outra foto."));
        } else if (q.brilho() > lim.brilhoMaximo()) {
            out.add(Apontamento.reprova(CODIGO,
                    format(java.util.Locale.ROOT, "Brilho médio %.1f acima do máximo %.1f", q.brilho(), lim.brilhoMaximo()),
                    "A foto está estourada de luz. Evite reflexo direto e envie outra foto."));
        }
        if (q.contraste() < lim.contrasteMinimo()) {
            out.add(Apontamento.reprova(CODIGO,
                    format(java.util.Locale.ROOT, "Contraste %.1f abaixo do mínimo %.1f", q.contraste(), lim.contrasteMinimo()),
                    "Não dá para distinguir o item na foto. Aproxime-se e envie outra foto."));
        }
        if (out.isEmpty() && ctx.confiancaOcr() != null && ctx.confiancaOcr() < 20) {
            out.add(Apontamento.alerta(CODIGO,
                    format(java.util.Locale.ROOT, "Confiança média do OCR baixa: %.1f", ctx.confiancaOcr()),
                    "O texto da etiqueta ficou pouco legível; se possível, fotografe o selo mais de perto."));
        }
        return out;
    }
}
