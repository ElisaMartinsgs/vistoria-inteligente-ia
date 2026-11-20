package br.com.saborecia.vistoria.infra.pdf;

import br.com.saborecia.vistoria.excecao.ArquivoInvalidoException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Converte a primeira página de um PDF (checklist escaneado) em PNG a 300 DPI,
 * para seguir o mesmo pipeline das fotos.
 */
@Component
public class PdfParaImagem {

    private static final int DPI = 300;

    public byte[] primeiraPagina(byte[] pdf) {
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            if (doc.getNumberOfPages() == 0) throw new ArquivoInvalidoException("O PDF não tem páginas.");
            BufferedImage img = new PDFRenderer(doc).renderImageWithDPI(0, DPI, ImageType.RGB);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(img, "png", out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o PDF (arquivo corrompido ou protegido por senha).");
        }
    }
}
