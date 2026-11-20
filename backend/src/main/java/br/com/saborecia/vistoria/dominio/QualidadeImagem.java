package br.com.saborecia.vistoria.dominio;

/**
 * Métricas calculadas pelo OpenCV sobre a foto.
 *
 * @param nitidez   variância do Laplaciano (quanto maior, mais nítida; foto tremida fica baixa)
 * @param brilho    média dos pixels em tons de cinza (0 = preto, 255 = branco)
 * @param contraste desvio-padrão dos pixels
 * @param anguloCorrigido graus de rotação aplicados no deskew
 */
public record QualidadeImagem(double nitidez, double brilho, double contraste, double anguloCorrigido,
                              boolean perspectivaCorrigida) {

    public record Limites(double nitidezMinima, double brilhoMinimo, double brilhoMaximo, double contrasteMinimo) {
        public static Limites padrao() {
            return new Limites(40.0, 45.0, 240.0, 18.0);
        }
    }
}
