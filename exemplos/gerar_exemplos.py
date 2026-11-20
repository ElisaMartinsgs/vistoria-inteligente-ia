"""
Gera as 3 imagens da demonstração final (simulam a foto do selo/etiqueta de manutenção do extintor):

  1-extintor-conforme.jpg  -> validade 08/2027        => CONFORME
  2-extintor-vencido.jpg   -> validade 10/08/2026     => NÃO CONFORME (vencido)
  3-foto-ilegivel.jpg      -> foto tremida e escura   => NÃO CONFORME (ilegível)

Uso: python3 exemplos/gerar_exemplos.py
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont, ImageEnhance

PASTA = Path(__file__).parent
FONTE = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"
FONTE_NEGRITO = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def fonte(caminho, tamanho):
    try:
        return ImageFont.truetype(caminho, tamanho)
    except OSError:
        return ImageFont.load_default()


def etiqueta(validade: str) -> Image.Image:
    """Desenha a etiqueta de manutenção (fundo branco, faixa vermelha)."""
    w, h = 900, 620
    img = Image.new("RGB", (w, h), (250, 250, 246))
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, w, 110], fill=(196, 30, 36))
    d.text((30, 28), "SELO DE MANUTENÇÃO - EXTINTOR", font=fonte(FONTE_NEGRITO, 40), fill="white")
    linhas = [
        ("EXTINTOR PQS 4 kg  -  Classe BC", FONTE_NEGRITO),
        ("Unidade: Sabor & Cia - Boa Viagem", FONTE),
        ("Local: Cozinha", FONTE),
        ("Data da recarga: 08/2026", FONTE),
        (f"Validade: {validade}", FONTE_NEGRITO),
        ("Norma: NR-23  |  Selo INMETRO 004512", FONTE),
    ]
    y = 140
    for texto, f in linhas:
        d.text((40, y), texto, font=fonte(f, 36), fill=(20, 20, 20))
        y += 74
    d.rectangle([4, 4, w - 5, h - 5], outline=(120, 120, 120), width=4)
    return img


def fotografar(etq: Image.Image, angulo: float, semente: int) -> Image.Image:
    """Coloca a etiqueta sobre um fundo (parede/cilindro), gira e adiciona ruído, como uma foto de celular."""
    random.seed(semente)
    fundo = Image.new("RGB", (1400, 1050), (168, 40, 40))  # vermelho do cilindro
    d = ImageDraw.Draw(fundo)
    for x in range(0, 1400, 6):  # textura leve
        tom = 150 + random.randint(-12, 12)
        d.line([(x, 0), (x, 1050)], fill=(tom, 34, 34), width=6)
    girada = etq.rotate(angulo, expand=True, fillcolor=(168, 40, 40), resample=Image.BICUBIC)
    fundo.paste(girada, ((1400 - girada.width) // 2, (1050 - girada.height) // 2))
    ruido = Image.effect_noise(fundo.size, 18).convert("RGB")
    foto = Image.blend(fundo, ruido, 0.06)
    return foto.filter(ImageFilter.GaussianBlur(0.6))


def main():
    fotografar(etiqueta("08/2027"), 4, 1).save(PASTA / "1-extintor-conforme.jpg", quality=90)
    fotografar(etiqueta("10/08/2026"), -3, 2).save(PASTA / "2-extintor-vencido.jpg", quality=90)

    ruim = fotografar(etiqueta("08/2027"), 2, 3)
    ruim = ruim.filter(ImageFilter.GaussianBlur(9))            # tremida/fora de foco
    ruim = ImageEnhance.Brightness(ruim).enhance(0.28)         # escura
    ruim.save(PASTA / "3-foto-ilegivel.jpg", quality=85)
    print("Imagens geradas em", PASTA)


if __name__ == "__main__":
    main()
