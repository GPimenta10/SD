/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Desenhar;

import java.awt.geom.Point2D;
import java.util.Map;
import java.awt.*;

/**
 * Responsável por desenhar os semáforos no mapa do Dashboard.
 */
public class DesenharSemaforos {
    private static final int TAMANHO_SEMAFORO = 12;

    /**
     * Desenha todos os semáforos conforme o estado atual (VERDE/VERMELHO).
     *
     * @param g2d
     * @param gp
     * @param estadosSemaforos
     */
    public void desenharTodos(Graphics2D g2d, GestorPosicoes gp, Map<String, Boolean> estadosSemaforos) {

        for (var entry : gp.getPosicoesSemaforos().entrySet()) {
            String chave = entry.getKey();
            Point2D pos = entry.getValue();

            boolean verde = estadosSemaforos.getOrDefault(chave, false);

            int x = (int) pos.getX() - TAMANHO_SEMAFORO / 2;
            int y = (int) pos.getY() - TAMANHO_SEMAFORO / 2;

            // Fundo circular
            g2d.setColor(Color.DARK_GRAY);
            g2d.fillOval(x - 3, y - 3, TAMANHO_SEMAFORO + 6, TAMANHO_SEMAFORO + 6);

            // Luz do semáforo
            g2d.setColor(verde ? new Color(76, 175, 80) : new Color(244, 67, 54));
            g2d.fillOval(x, y, TAMANHO_SEMAFORO, TAMANHO_SEMAFORO);

            // Contorno
            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, TAMANHO_SEMAFORO, TAMANHO_SEMAFORO);
        }
    }
}

