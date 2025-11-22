package Dashboard.Desenhar;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import java.awt.*;

/**
 *
 *
 */
public class DesenharLegenda {

    /**
     *
     *
     * @param g2d
     * @param painel
     */
    public static void desenharLegenda(Graphics2D g2d, JPanel painel) {
        g2d.setFont(DashboardUIUtils.FONTE_CONSOLE.deriveFont(Font.BOLD, 13f));

        g2d.setColor(UIManager.getColor("Label.foreground"));

        int largura = painel.getWidth();
        int altura = painel.getHeight();
        int margemInferior = 20;

        int y = altura - margemInferior;

        int espacamento = 90;

        String txtLegenda = "Legenda:";
        int larguraTextoLegenda = g2d.getFontMetrics().stringWidth(txtLegenda);

        int larguraLegenda = larguraTextoLegenda + 20 + 3 * espacamento;

        int x = (largura - larguraLegenda) / 2;

        g2d.drawString(txtLegenda, x, y);

        int xItens = x + larguraTextoLegenda + 20;

        desenharItemLegenda(g2d, xItens, y, new Color(255, 193, 7), "Mota");
        desenharItemLegenda(g2d, xItens + espacamento, y, new Color(33, 150, 243), "Carro");
        desenharItemLegenda(g2d, xItens + espacamento * 2, y, new Color(130, 109, 56), "Camião");
    }

    /**
     *
     *
     * @param g2d
     * @param x
     * @param y
     * @param cor
     * @param label
     */
    private static void desenharItemLegenda(Graphics2D g2d, int x, int y, Color cor, String label) {
        g2d.setColor(UIManager.getColor("Panel.background"));
        g2d.fillRect(x - 2, y - 12, 20, 20);

        // Ícone colorido (veículo)
        g2d.setColor(cor);
        g2d.fillOval(x, y - 6, 8, 8);

        // Texto da legenda com estilo moderno
        g2d.setColor(UIManager.getColor("Label.foreground"));
        g2d.drawString(label, x + 12, y);
    }
}
