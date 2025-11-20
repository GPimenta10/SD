package Dashboard.Desenhar;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Responsável por desenhar todas as vias (ligações) no PainelMapa.
 */
public class DesenharVias {

    private static final int ESPACAMENTO_VIA_DUPLA = 10;

    /**
     * Desenha todas as vias principais do mapa.
     *
     * @param g2d
     * @param gp
     */
    public void desenharTodas(Graphics2D g2d, GestorPosicoes gp) {
        g2d.setStroke(new BasicStroke(2f));
        g2d.setColor(Color.DARK_GRAY);

        // Vias simples
        desenharViaSimples(g2d, gp, "E1", "Cr1");
        desenharViaSimples(g2d, gp, "E2", "Cr2");
        desenharViaSimples(g2d, gp, "E3", "Cr3");
        desenharViaSimples(g2d, gp, "Cr1", "Cr4");
        desenharViaSimples(g2d, gp, "Cr2", "Cr5");
        desenharViaSimples(g2d, gp, "Cr3", "S");
        desenharViaSimples(g2d, gp, "Cr4", "Cr5");
        desenharViaSimples(g2d, gp, "Cr5", "S");

        // Vias bidirecionais
        desenharViaBidirecional(g2d, gp, "Cr1", "Cr2");
        desenharViaBidirecional(g2d, gp, "Cr2", "Cr3");
    }

    /**
     *
     *
     * @param g2d
     * @param gp
     * @param origem
     * @param destino
     */
    private void desenharViaSimples(Graphics2D g2d, GestorPosicoes gp, String origem, String destino) {
        Point2D p1 = gp.getPosicoes().get(origem);
        Point2D p2 = gp.getPosicoes().get(destino);

        if (p1 == null || p2 == null) return;

        g2d.drawLine((int) p1.getX(), (int) p1.getY(), (int) p2.getX(), (int) p2.getY());
    }

    /**
     *
     *
     * @param g2d
     * @param gp
     * @param n1
     * @param n2
     */
    private void desenharViaBidirecional(Graphics2D g2d, GestorPosicoes gp, String n1, String n2) {
        Point2D p1 = gp.getPosicoes().get(n1);
        Point2D p2 = gp.getPosicoes().get(n2);
        if (p1 == null || p2 == null) return;

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        double perpX = -dy / dist * ESPACAMENTO_VIA_DUPLA;
        double perpY = dx / dist * ESPACAMENTO_VIA_DUPLA;

        Point2D a1 = new Point2D.Double(p1.getX() + perpX, p1.getY() + perpY);
        Point2D a2 = new Point2D.Double(p2.getX() + perpX, p2.getY() + perpY);
        g2d.drawLine((int) a1.getX(), (int) a1.getY(), (int) a2.getX(), (int) a2.getY());

        Point2D b1 = new Point2D.Double(p1.getX() - perpX, p1.getY() - perpY);
        Point2D b2 = new Point2D.Double(p2.getX() - perpX, p2.getY() - perpY);
        g2d.drawLine((int) b1.getX(), (int) b1.getY(), (int) b2.getX(), (int) b2.getY());
    }
}
