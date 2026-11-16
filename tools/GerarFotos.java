import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Random;
import javax.imageio.ImageIO;

/**
 * Gera as 6 imagens ilustrativas do seed (sem pessoas, sem marcas).
 * Uso: java tools/GerarFotos.java src/main/resources/static/seed-fotos
 */
public class GerarFotos {

    static final int L = 960, A = 720;
    static final Random RNG = new Random(7);

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        File pasta = new File(args[0]);
        pasta.mkdirs();
        gravar(pasta, "ferido", GerarFotos::ferido);
        gravar(pasta, "colonia", GerarFotos::colonia);
        gravar(pasta, "filhote", GerarFotos::filhote);
        gravar(pasta, "doente", GerarFotos::doente);
        gravar(pasta, "maustratos", GerarFotos::maustratos);
        gravar(pasta, "risco", GerarFotos::risco);
    }

    interface Cena { void desenhar(Graphics2D g); }

    static void gravar(File pasta, String nome, Cena cena) throws Exception {
        BufferedImage img = new BufferedImage(L, A, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        cena.desenhar(g);
        g.dispose();
        granulado(img);
        ImageIO.write(img, "jpg", new File(pasta, nome + ".jpg"));
    }

    static void granulado(BufferedImage img) {
        for (int y = 0; y < A; y++) {
            for (int x = 0; x < L; x++) {
                int rgb = img.getRGB(x, y);
                int d = RNG.nextInt(13) - 6;
                int r = clamp(((rgb >> 16) & 255) + d), gr = clamp(((rgb >> 8) & 255) + d), b = clamp((rgb & 255) + d);
                img.setRGB(x, y, (r << 16) | (gr << 8) | b);
            }
        }
    }

    static int clamp(int v) { return Math.max(0, Math.min(255, v)); }

    static Color c(int r, int g, int b) { return new Color(r, g, b); }

    /** Muro ao fundo, calçada e meio-fio. */
    static void rua(Graphics2D g, Color muro, Color calcada) {
        g.setColor(muro);
        g.fillRect(0, 0, L, 470);
        g.setColor(muro.darker());
        for (int x = 0; x < L; x += 120) g.fillRect(x, 0, 3, 470);
        for (int y = 90; y < 470; y += 110) g.fillRect(0, y, L, 3);
        g.setColor(calcada);
        g.fillRect(0, 470, L, 150);
        g.setColor(calcada.darker());
        g.fillRect(0, 470, L, 8);
        g.setColor(c(88, 90, 86));
        g.fillRect(0, 620, L, 100);
        g.setColor(c(224, 214, 120));
        g.fillRect(0, 668, L, 10);
    }

    static void sombra(Graphics2D g, double x, double y, double w) {
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Double(x - w / 2, y - 14, w, 28));
    }

    static void cachorroDeitado(Graphics2D g, double x, double y, double s, Color pelo, boolean machucado) {
        sombra(g, x, y + 4, 330 * s);
        g.setColor(pelo);
        g.fill(new Ellipse2D.Double(x - 150 * s, y - 80 * s, 270 * s, 100 * s));
        g.fill(new Ellipse2D.Double(x + 80 * s, y - 100 * s, 90 * s, 80 * s));
        Path2D orelha = new Path2D.Double();
        orelha.moveTo(x + 105 * s, y - 98 * s);
        orelha.lineTo(x + 92 * s, y - 40 * s);
        orelha.lineTo(x + 130 * s, y - 70 * s);
        orelha.closePath();
        g.setColor(pelo.darker());
        g.fill(orelha);
        g.setColor(pelo);
        g.fill(new Ellipse2D.Double(x + 140 * s, y - 70 * s, 50 * s, 34 * s));
        g.setColor(c(30, 28, 26));
        g.fill(new Ellipse2D.Double(x + 178 * s, y - 66 * s, 14 * s, 11 * s));
        g.fill(new Ellipse2D.Double(x + 134 * s, y - 80 * s, 9 * s, 9 * s));
        g.setColor(pelo);
        g.setStroke(new BasicStroke((float) (22 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) (x - 130 * s), (int) (y - 30 * s), (int) (x - 200 * s), (int) (y - 6 * s));
        g.drawLine((int) (x + 40 * s), (int) (y - 6 * s), (int) (x + 120 * s), (int) (y + 2 * s));
        g.drawLine((int) (x - 40 * s), (int) (y - 6 * s), (int) (x + 20 * s), (int) (y + 2 * s));
        if (machucado) {
            g.setColor(c(150, 40, 30));
            g.fill(new Ellipse2D.Double(x - 70 * s, y - 20 * s, 44 * s, 20 * s));
            g.setColor(c(235, 235, 225));
            g.fill(new RoundRectangle2D.Double(x - 66 * s, y - 24 * s, 36 * s, 12 * s, 6, 6));
        }
    }

    static void gato(Graphics2D g, double x, double y, double s, Color pelo, boolean sentado) {
        sombra(g, x, y + 2, 120 * s);
        g.setColor(pelo);
        if (sentado) {
            g.fill(new Ellipse2D.Double(x - 45 * s, y - 100 * s, 90 * s, 100 * s));
            g.fill(new Ellipse2D.Double(x - 30 * s, y - 150 * s, 60 * s, 55 * s));
            Path2D o = new Path2D.Double();
            o.moveTo(x - 30 * s, y - 135 * s); o.lineTo(x - 26 * s, y - 172 * s); o.lineTo(x - 8 * s, y - 148 * s); o.closePath();
            o.moveTo(x + 30 * s, y - 135 * s); o.lineTo(x + 26 * s, y - 172 * s); o.lineTo(x + 8 * s, y - 148 * s); o.closePath();
            g.fill(o);
            g.setStroke(new BasicStroke((float) (12 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.drawArc((int) (x + 20 * s), (int) (y - 70 * s), (int) (70 * s), (int) (70 * s), 270, 140);
        } else {
            g.fill(new Ellipse2D.Double(x - 70 * s, y - 55 * s, 140 * s, 55 * s));
            g.fill(new Ellipse2D.Double(x + 40 * s, y - 80 * s, 48 * s, 44 * s));
            Path2D o = new Path2D.Double();
            o.moveTo(x + 46 * s, y - 70 * s); o.lineTo(x + 50 * s, y - 100 * s); o.lineTo(x + 64 * s, y - 78 * s); o.closePath();
            o.moveTo(x + 84 * s, y - 70 * s); o.lineTo(x + 80 * s, y - 100 * s); o.lineTo(x + 66 * s, y - 78 * s); o.closePath();
            g.fill(o);
        }
        g.setColor(c(228, 214, 120));
        double hx = x - 14 * s + (sentado ? 0 : 66 * s), hy = y - (sentado ? 125 : 62) * s;
        g.fill(new Ellipse2D.Double(hx, hy, 9 * s, 9 * s));
        g.fill(new Ellipse2D.Double(hx + 20 * s, hy, 9 * s, 9 * s));
    }

    static void ferido(Graphics2D g) {
        rua(g, c(205, 193, 170), c(160, 158, 150));
        cachorroDeitado(g, 470, 560, 1.4, c(176, 126, 70), true);
    }

    static void colonia(Graphics2D g) {
        rua(g, c(172, 178, 170), c(150, 148, 140));
        g.setColor(c(120, 118, 110));
        g.fill(new RoundRectangle2D.Double(70, 330, 820, 60, 8, 8));
        gato(g, 200, 330, 1.1, c(210, 150, 80), true);
        gato(g, 420, 330, 1.0, c(60, 58, 56), true);
        gato(g, 640, 330, 1.1, c(200, 198, 190), true);
        gato(g, 300, 560, 1.3, c(120, 100, 84), false);
        gato(g, 640, 570, 1.2, c(70, 68, 66), false);
        gato(g, 820, 560, 1.0, c(210, 150, 80), true);
    }

    static void filhote(Graphics2D g) {
        rua(g, c(190, 180, 160), c(156, 152, 142));
        g.setColor(c(176, 134, 84));
        g.fill(new RoundRectangle2D.Double(250, 400, 460, 190, 10, 10));
        g.setColor(c(150, 110, 66));
        g.fill(new RoundRectangle2D.Double(250, 400, 460, 28, 10, 10));
        sombra(g, 480, 596, 520);
        Color[] cores = {c(200, 160, 100), c(70, 62, 56), c(222, 214, 200), c(160, 120, 80)};
        for (int i = 0; i < 4; i++) {
            double px = 320 + i * 110, py = 420;
            g.setColor(cores[i]);
            g.fill(new Ellipse2D.Double(px - 46, py - 54, 92, 80));
            g.setColor(cores[i].darker());
            g.fill(new Ellipse2D.Double(px - 52, py - 52, 26, 44));
            g.fill(new Ellipse2D.Double(px + 26, py - 52, 26, 44));
            g.setColor(c(30, 28, 26));
            g.fill(new Ellipse2D.Double(px - 20, py - 30, 9, 9));
            g.fill(new Ellipse2D.Double(px + 11, py - 30, 9, 9));
            g.fill(new Ellipse2D.Double(px - 8, py - 12, 16, 11));
        }
    }

    static void doente(Graphics2D g) {
        rua(g, c(180, 186, 178), c(156, 154, 146));
        double x = 480, y = 560, s = 1.35;
        sombra(g, x, y + 2, 300);
        g.setColor(c(128, 112, 96));
        g.fill(new Ellipse2D.Double(x - 80 * s, y - 120 * s, 110 * s, 120 * s));
        g.fill(new Ellipse2D.Double(x - 10 * s, y - 175 * s, 70 * s, 62 * s));
        Path2D o = new Path2D.Double();
        o.moveTo(x - 4 * s, y - 160 * s); o.lineTo(x - 2 * s, y - 205 * s); o.lineTo(x + 20 * s, y - 172 * s); o.closePath();
        o.moveTo(x + 54 * s, y - 160 * s); o.lineTo(x + 52 * s, y - 205 * s); o.lineTo(x + 30 * s, y - 172 * s); o.closePath();
        g.fill(o);
        g.setColor(c(96, 84, 72));
        for (int i = 0; i < 7; i++) {
            g.fill(new Ellipse2D.Double(x - 60 * s + RNG.nextInt(90), y - 100 * s + RNG.nextInt(90), 18, 12));
        }
        g.setColor(c(228, 214, 120));
        g.fill(new Ellipse2D.Double(x + 6 * s, y - 150 * s, 10 * s, 7 * s));
        g.fill(new Ellipse2D.Double(x + 34 * s, y - 150 * s, 10 * s, 7 * s));
        g.setColor(c(128, 112, 96));
        g.setStroke(new BasicStroke((float) (14 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine((int) (x - 60 * s), (int) (y - 10 * s), (int) (x - 160 * s), (int) (y - 6 * s));
    }

    static void maustratos(Graphics2D g) {
        rua(g, c(168, 172, 164), c(150, 146, 138));
        g.setColor(c(96, 100, 98));
        g.setStroke(new BasicStroke(6));
        for (int x = 60; x < L; x += 70) g.drawLine(x, 120, x, 470);
        g.drawLine(0, 170, L, 170);
        g.drawLine(0, 420, L, 420);
        double x = 520, y = 570;
        cachorroDeitado(g, x, y, 1.25, c(96, 78, 62), false);
        g.setColor(c(62, 62, 60));
        g.setStroke(new BasicStroke(7));
        g.drawLine((int) (x + 190), (int) (y - 70), 740, 420);
        for (int i = 0; i < 9; i++) {
            double t = i / 8.0;
            g.draw(new Ellipse2D.Double(x + 190 + (740 - x - 190) * t - 7, y - 70 + (420 - (y - 70)) * t - 4, 14, 8));
        }
        g.setColor(c(206, 196, 160));
        g.fill(new Ellipse2D.Double(190, 575, 130, 30));
    }

    static void risco(Graphics2D g) {
        rua(g, c(186, 176, 160), c(152, 148, 138));
        Color[] sacos = {c(40, 44, 42), c(62, 78, 66), c(30, 34, 36), c(88, 86, 80), c(52, 60, 70)};
        for (int i = 0; i < 16; i++) {
            double x = 230 + RNG.nextInt(480), y = 480 + RNG.nextInt(110), r = 34 + RNG.nextInt(34);
            g.setColor(sacos[RNG.nextInt(sacos.length)]);
            g.fill(new Ellipse2D.Double(x - r, y - r * 0.8, r * 2, r * 1.6));
            g.setColor(new Color(255, 255, 255, 30));
            g.fill(new Ellipse2D.Double(x - r * 0.5, y - r * 0.6, r * 0.5, r * 0.4));
        }
        g.setColor(c(150, 150, 140));
        for (int i = 0; i < 10; i++) {
            AffineTransform at = g.getTransform();
            g.translate(260 + RNG.nextInt(440), 500 + RNG.nextInt(100));
            g.rotate(RNG.nextDouble() * 3);
            g.fill(new RoundRectangle2D.Double(0, 0, 30, 18, 4, 4));
            g.setTransform(at);
        }
        g.setColor(c(86, 80, 76));
        g.fill(new Ellipse2D.Double(770, 590, 56, 26));
        g.fill(new Ellipse2D.Double(808, 586, 24, 18));
        g.setStroke(new BasicStroke(3));
        g.drawLine(764, 602, 730, 596);
    }
}
