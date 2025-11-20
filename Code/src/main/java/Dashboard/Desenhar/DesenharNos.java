package Dashboard.Desenhar;

import java.awt.*;
import java.awt.geom.Point2D;

/**
 * Responsável pelo desenho dos nós no mapa (entradas, cruzamentos e saída).
 */
public class DesenharNos {

    private static final int LARGURA_CRUZAMENTO = 60;
    private static final int ALTURA_CRUZAMENTO = 40;

    /**
     * Desenha todos os nós do mapa (E1, E2, Cr1, Cr2, ..., S).
     *
     * @param g2d
     * @param gp
     */
    public void desenharTodos(Graphics2D g2d, GestorPosicoes gp) {

        // Entradas (Verde)
        desenharNo(g2d, gp, "E1", new Color(76,175,80));
        desenharNo(g2d, gp, "E2", new Color(76,175,80));
        desenharNo(g2d, gp, "E3", new Color(76,175,80));

        // Cruzamentos (Azul)
        Color azul = new Color(33,150,243);
        desenharNo(g2d, gp, "Cr1", azul);
        desenharNo(g2d, gp, "Cr2", azul);
        desenharNo(g2d, gp, "Cr3", azul);
        desenharNo(g2d, gp, "Cr4", azul);
        desenharNo(g2d, gp, "Cr5", azul);

        // Saída (Vermelho)
        desenharNo(g2d, gp, "S", new Color(244,67,54));
    }

    /**
     *
     *
     * @param g2d
     * @param gp
     * @param id
     * @param cor
     */
    private void desenharNo(Graphics2D g2d, GestorPosicoes gp, String id, Color cor) {
        Point2D pos = gp.getPosicoes().get(id);
        if (pos == null) return;

        int x = (int) pos.getX() - LARGURA_CRUZAMENTO / 2;
        int y = (int) pos.getY() - ALTURA_CRUZAMENTO / 2;

        // Fundo
        g2d.setColor(cor);
        g2d.fillRoundRect(x, y, LARGURA_CRUZAMENTO, ALTURA_CRUZAMENTO, 10, 10);

        // Contorno
        g2d.setColor(cor.darker());
        g2d.drawRoundRect(x, y, LARGURA_CRUZAMENTO, ALTURA_CRUZAMENTO, 10, 10);

        // Texto (ID)
        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(id);
        int h = fm.getAscent();

        g2d.drawString(id, x + (LARGURA_CRUZAMENTO - w) / 2,
                y + (ALTURA_CRUZAMENTO + h) / 2 - 3);
    }
}
