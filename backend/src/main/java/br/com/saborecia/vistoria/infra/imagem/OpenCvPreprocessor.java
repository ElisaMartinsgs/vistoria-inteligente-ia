package br.com.saborecia.vistoria.infra.imagem;

import br.com.saborecia.vistoria.config.VistoriaProperties;
import br.com.saborecia.vistoria.dominio.QualidadeImagem;
import br.com.saborecia.vistoria.excecao.ArquivoInvalidoException;
import jakarta.annotation.PostConstruct;
import nu.pattern.OpenCV;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.CLAHE;
import org.opencv.imgproc.Imgproc;
import org.opencv.photo.Photo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Etapa 2 do pipeline — OpenCV: "trata e melhora a imagem para facilitar a leitura".
 * <ol>
 *   <li>Padroniza o tamanho (as métricas de qualidade ficam comparáveis entre fotos).</li>
 *   <li>Mede nitidez (variância do Laplaciano), brilho e contraste → decide se a foto é legível.</li>
 *   <li>Corrige perspectiva (quando encontra um retângulo grande, ex.: etiqueta/folha).</li>
 *   <li>Corrige rotação (deskew) de até 15°.</li>
 *   <li>Ajusta contraste (CLAHE), remove ruído e converte para preto e branco (limiar adaptativo).</li>
 * </ol>
 */
@Component
public class OpenCvPreprocessor {

    private static final Logger log = LoggerFactory.getLogger(OpenCvPreprocessor.class);
    private static final double ANGULO_MAXIMO_DESKEW = 15.0;

    private final VistoriaProperties.Imagem cfg;

    public OpenCvPreprocessor(VistoriaProperties props) {
        this.cfg = props.imagem();
    }

    @PostConstruct
    void carregarBibliotecaNativa() {
        OpenCV.loadLocally();
        log.info("OpenCV {} carregado", Core.VERSION);
    }

    public record ImagemProcessada(BufferedImage paraOcr, byte[] pngTratado, byte[] jpegColorido, QualidadeImagem qualidade) {}

    public ImagemProcessada processar(byte[] bytesImagem) {
        Mat original = Imgcodecs.imdecode(new MatOfByte(bytesImagem), Imgcodecs.IMREAD_COLOR);
        if (original.empty()) {
            throw new ArquivoInvalidoException("Não foi possível abrir a imagem. Envie um JPG, PNG ou PDF válido.");
        }
        Mat colorida = redimensionar(original, cfg.larguraPadrao());
        Mat cinza = new Mat();
        Imgproc.cvtColor(colorida, cinza, Imgproc.COLOR_BGR2GRAY);

        // 1) Métricas de qualidade na foto como chegou (antes de "melhorar")
        double nitidez = varianciaLaplaciano(cinza);
        MatOfDouble media = new MatOfDouble();
        MatOfDouble desvio = new MatOfDouble();
        Core.meanStdDev(cinza, media, desvio);
        double brilho = media.get(0, 0)[0];
        double contraste = desvio.get(0, 0)[0];

        // 2) Perspectiva
        boolean perspectiva = false;
        if (cfg.corrigirPerspectiva()) {
            Mat corrigida = corrigirPerspectiva(cinza);
            if (corrigida != null) {
                cinza = corrigida;
                perspectiva = true;
            }
        }

        // 3) Rotação
        double angulo = anguloInclinacao(cinza);
        if (Math.abs(angulo) >= 0.5 && Math.abs(angulo) <= ANGULO_MAXIMO_DESKEW) {
            cinza = rotacionar(cinza, angulo);
        } else {
            angulo = 0;
        }

        // 4) Contraste + ruído + preto e branco
        Mat realcada = new Mat();
        CLAHE clahe = Imgproc.createCLAHE(2.0, new Size(8, 8));
        clahe.apply(cinza, realcada);
        Mat semRuido = new Mat();
        Photo.fastNlMeansDenoising(realcada, semRuido, 10);
        Mat binaria = new Mat();
        Imgproc.adaptiveThreshold(semRuido, binaria, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
                Imgproc.THRESH_BINARY, 31, 15);

        QualidadeImagem q = new QualidadeImagem(nitidez, brilho, contraste, angulo, perspectiva);
        log.debug("Qualidade da imagem: {}", q);

        byte[] png = codificar(binaria, ".png");
        byte[] jpeg = codificar(colorida, ".jpg");
        original.release();
        realcada.release();
        semRuido.release();
        return new ImagemProcessada(paraBufferedImage(png), png, jpeg, q);
    }

    private static Mat redimensionar(Mat img, int largura) {
        if (img.cols() == largura) return img;
        double escala = (double) largura / img.cols();
        Mat out = new Mat();
        int interp = escala < 1 ? Imgproc.INTER_AREA : Imgproc.INTER_CUBIC;
        Imgproc.resize(img, out, new Size(largura, Math.round(img.rows() * escala)), 0, 0, interp);
        return out;
    }

    /** Foto tremida/borrada tem poucas bordas → variância do Laplaciano baixa. */
    static double varianciaLaplaciano(Mat cinza) {
        Mat lap = new Mat();
        Imgproc.Laplacian(cinza, lap, CvType.CV_64F);
        MatOfDouble media = new MatOfDouble();
        MatOfDouble desvio = new MatOfDouble();
        Core.meanStdDev(lap, media, desvio);
        lap.release();
        double d = desvio.get(0, 0)[0];
        return d * d;
    }

    /** Procura o maior quadrilátero (etiqueta/folha) e "endireita" a visão dele. */
    private static Mat corrigirPerspectiva(Mat cinza) {
        Mat blur = new Mat();
        Imgproc.GaussianBlur(cinza, blur, new Size(5, 5), 0);
        Mat bordas = new Mat();
        Imgproc.Canny(blur, bordas, 50, 150);
        Imgproc.dilate(bordas, bordas, Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3)));
        List<MatOfPoint> contornos = new ArrayList<>();
        Imgproc.findContours(bordas, contornos, new Mat(), Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
        double areaImagem = cinza.rows() * (double) cinza.cols();

        contornos.sort(Comparator.<MatOfPoint>comparingDouble(Imgproc::contourArea).reversed());
        for (MatOfPoint c : contornos.subList(0, Math.min(5, contornos.size()))) {
            double area = Imgproc.contourArea(c);
            if (area < 0.25 * areaImagem || area > 0.98 * areaImagem) continue;
            MatOfPoint2f c2f = new MatOfPoint2f(c.toArray());
            MatOfPoint2f aprox = new MatOfPoint2f();
            Imgproc.approxPolyDP(c2f, aprox, 0.02 * Imgproc.arcLength(c2f, true), true);
            if (aprox.total() != 4) continue;

            Point[] p = ordenarCantos(aprox.toArray());
            double largura = Math.max(dist(p[0], p[1]), dist(p[3], p[2]));
            double altura = Math.max(dist(p[0], p[3]), dist(p[1], p[2]));
            MatOfPoint2f origem = new MatOfPoint2f(p);
            MatOfPoint2f destino = new MatOfPoint2f(
                    new Point(0, 0), new Point(largura - 1, 0),
                    new Point(largura - 1, altura - 1), new Point(0, altura - 1));
            Mat m = Imgproc.getPerspectiveTransform(origem, destino);
            Mat out = new Mat();
            Imgproc.warpPerspective(cinza, out, m, new Size(largura, altura));
            return out;
        }
        return null;
    }

    /** Ordem: topo-esquerda, topo-direita, baixo-direita, baixo-esquerda. */
    private static Point[] ordenarCantos(Point[] pts) {
        Point tl = pts[0], tr = pts[0], br = pts[0], bl = pts[0];
        for (Point p : pts) {
            if (p.x + p.y < tl.x + tl.y) tl = p;
            if (p.x + p.y > br.x + br.y) br = p;
            if (p.y - p.x < tr.y - tr.x) tr = p;
            if (p.y - p.x > bl.y - bl.x) bl = p;
        }
        return new Point[]{tl, tr, br, bl};
    }

    private static double dist(Point a, Point b) {
        return Math.hypot(a.x - b.x, a.y - b.y);
    }

    /** Ângulo do texto: retângulo de área mínima que envolve os pixels escuros. */
    private static double anguloInclinacao(Mat cinza) {
        Mat bin = new Mat();
        Imgproc.threshold(cinza, bin, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
        Mat pontos = new Mat();
        Core.findNonZero(bin, pontos);
        if (pontos.rows() < 50) return 0;
        MatOfPoint mp = new MatOfPoint(pontos);
        RotatedRect r = Imgproc.minAreaRect(new MatOfPoint2f(mp.toArray()));
        double angulo = r.angle;
        if (angulo > 45) angulo -= 90;
        if (angulo < -45) angulo += 90;
        bin.release();
        pontos.release();
        return angulo;
    }

    private static Mat rotacionar(Mat img, double angulo) {
        Point centro = new Point(img.cols() / 2.0, img.rows() / 2.0);
        Mat m = Imgproc.getRotationMatrix2D(centro, angulo, 1.0);
        Mat out = new Mat();
        Imgproc.warpAffine(img, out, m, img.size(), Imgproc.INTER_CUBIC, Core.BORDER_CONSTANT, new Scalar(255));
        return out;
    }

    private static byte[] codificar(Mat img, String ext) {
        MatOfByte buf = new MatOfByte();
        Imgcodecs.imencode(ext, img, buf);
        return buf.toArray();
    }

    private static BufferedImage paraBufferedImage(byte[] png) {
        try {
            return ImageIO.read(new ByteArrayInputStream(png));
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao converter imagem tratada", e);
        }
    }
}
