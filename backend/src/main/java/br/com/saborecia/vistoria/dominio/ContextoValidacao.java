package br.com.saborecia.vistoria.dominio;

import java.time.LocalDate;
import java.util.Set;

/**
 * Tudo o que as regras precisam saber além do JSON extraído.
 *
 * @param dataReferencia     "hoje" (injetável para testes)
 * @param unidadeCadastro    nome da unidade escolhida pelo técnico no app
 * @param tipoEsperado       o item que o técnico disse que está fotografando (pode ser null)
 * @param normasAtivas       códigos das NRs vigentes cadastradas no banco (ex.: "NR-23")
 * @param qualidade          métricas do OpenCV (null quando não houve imagem)
 * @param limites            limites de legibilidade
 * @param confiancaOcr       confiança média do Tesseract (0..100), null se não rodou
 * @param origem             de onde veio o JSON (LLM, Regex, fallback)
 * @param confiancaMinimaIa  abaixo disso o item vai para revisão manual
 * @param diasAlertaVencimento itens que vencem em até N dias geram alerta
 */
public record ContextoValidacao(
        LocalDate dataReferencia,
        String unidadeCadastro,
        TipoItem tipoEsperado,
        Set<String> normasAtivas,
        QualidadeImagem qualidade,
        QualidadeImagem.Limites limites,
        Double confiancaOcr,
        OrigemExtracao origem,
        double confiancaMinimaIa,
        int diasAlertaVencimento
) {
}
