package Dashboard;

import javax.swing.*;
import java.awt.*;

/**
 * Painel central que desenha o mapa do sistema.
 */
public class PainelMapa extends JPanel {

    public PainelMapa() {
        setPreferredSize(new Dimension(800, 400));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Mapa do Sistema"));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        g.setColor(Color.BLACK);
        g.setFont(new Font("Arial", Font.BOLD, 14));

        // Entradas
        g.drawString("E1", 100, 50);
        g.drawString("E2", 300, 50);
        g.drawString("E3", 500, 50);

        g.drawLine(110, 60, 110, 150);
        g.drawLine(310, 60, 310, 150);
        g.drawLine(510, 60, 510, 150);

        // Cruzamentos superiores
        g.drawOval(100, 150, 30, 30); g.drawString("Cr1", 90, 200);
        g.drawOval(300, 150, 30, 30); g.drawString("Cr2", 290, 200);
        g.drawOval(500, 150, 30, 30); g.drawString("Cr3", 490, 200);

        g.drawLine(130, 165, 300, 165);
        g.drawLine(330, 165, 500, 165);

        // Cruzamentos inferiores
        g.drawOval(100, 300, 30, 30); g.drawString("Cr4", 90, 350);
        g.drawOval(300, 300, 30, 30); g.drawString("Cr5", 290, 350);

        g.drawLine(115, 180, 115, 300);
        g.drawLine(315, 180, 315, 300);

        // Saída
        g.drawString("S", 520, 350);
        g.drawLine(515, 180, 515, 350);
    }
}
