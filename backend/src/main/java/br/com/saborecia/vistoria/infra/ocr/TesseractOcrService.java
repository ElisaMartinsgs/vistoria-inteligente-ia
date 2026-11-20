package br.com.saborecia.vistoria.infra.ocr;

import br.com.saborecia.vistoria.config.VistoriaProperties;
import br.com.saborecia.vistoria.excecao.OcrIndisponivelException;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Etapa 3 do pipeline — Tesseract: "extrai o texto da imagem".
 * Reconhece caracteres e devolve uma String; NÃO entende o significado do documento (isso é papel da IA).
 * <p>
 * Uma única passada por linha (RIL_TEXTLINE) devolve o texto e a confiança de cada linha.
 * A chamada roda em um pool limitado e com tempo máximo, para não travar a API.
 */
@Component
public class TesseractOcrService {

    private static final Logger log = LoggerFactory.getLogger(TesseractOcrService.class);

    private final VistoriaProperties.Ocr cfg;
    private final ExecutorService executor;

    public TesseractOcrService(VistoriaProperties props, ExecutorService ocrExecutor) {
        this.cfg = props.ocr();
        this.executor = ocrExecutor;
    }

    public record ResultadoOcr(String texto, double confiancaMedia, long latenciaMs) {}

    public ResultadoOcr extrairTexto(BufferedImage imagem) {
        long inicio = System.currentTimeMillis();
        Future<ResultadoOcr> futuro = executor.submit(() -> executar(imagem, inicio));
        try {
            return futuro.get(cfg.timeoutSegundos(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            futuro.cancel(true);
            throw new OcrIndisponivelException("OCR excedeu " + cfg.timeoutSegundos() + "s", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new OcrIndisponivelException("OCR interrompido", e);
        } catch (ExecutionException e) {
            throw new OcrIndisponivelException("Falha no Tesseract: " + e.getCause().getMessage(), e.getCause());
        }
    }

    private ResultadoOcr executar(BufferedImage imagem, long inicio) {
        // Tesseract não é thread-safe: uma instância por chamada.
        Tesseract t = new Tesseract();
        if (cfg.tessdata() != null && !cfg.tessdata().isBlank()) t.setDatapath(cfg.tessdata());
        t.setLanguage(cfg.idioma());
        t.setPageSegMode(cfg.psm());
        t.setOcrEngineMode(ITessAPI.TessOcrEngineMode.OEM_LSTM_ONLY);
        t.setVariable("user_defined_dpi", "300");

        List<Word> linhas = t.getWords(imagem, ITessAPI.TessPageIteratorLevel.RIL_TEXTLINE);
        List<Word> validas = linhas.stream().filter(w -> !w.getText().isBlank()).toList();
        String texto = validas.stream().map(w -> w.getText().strip()).collect(Collectors.joining("\n"));
        double confianca = validas.stream().mapToDouble(Word::getConfidence).average().orElse(0);
        long ms = System.currentTimeMillis() - inicio;
        log.debug("OCR: {} linhas, confiança média {}, {} ms", validas.size(), confianca, ms);
        return new ResultadoOcr(texto, confianca, ms);
    }
}
