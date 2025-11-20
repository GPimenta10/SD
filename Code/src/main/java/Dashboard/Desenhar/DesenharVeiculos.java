package Dashboard.Desenhar;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Responsável pelo desenho dos veículos animados no mapa.
 */
public class DesenharVeiculos {

    private static final int TAMANHO_VEICULO = 10;

    /**
     * Desenha todos os veículos atualmente presentes no mapa.
     *
     * @param g2d
     * @param veiculos
     */
    public void desenharTodos(Graphics2D g2d, List<VeiculoNoMapa> veiculos) {
        for (VeiculoNoMapa v : veiculos) {

            Point2D pos = v.getPosicaoAtual();
            int x = (int) pos.getX() - TAMANHO_VEICULO / 2;
            int y = (int) pos.getY() - TAMANHO_VEICULO / 2;

            // Corpo do veículo
            g2d.setColor(v.getCor());
            g2d.fillOval(x, y, TAMANHO_VEICULO, TAMANHO_VEICULO);

            // Contorno
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, TAMANHO_VEICULO, TAMANHO_VEICULO);

            // Destaque se estiver parado
            if (v.isParado()) {
                g2d.setColor(Color.RED);
                g2d.drawRect(x - 2, y - 2, TAMANHO_VEICULO + 4, TAMANHO_VEICULO + 4);
            }
        }
    }
}

