package Dashboard.Desenhar;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Responsável pelo desenho dos veículos animados no mapa.
 */
public class DesenharVeiculos {
    private static final int TAMANHO_VEICULO = 10;
    private static final Color COR_MOTA = new Color(255, 193, 7);   
    private static final Color COR_CARRO = new Color(33, 150, 243); 
    private static final Color COR_CAMIAO = new Color(130, 109, 56); 
    private static final Color COR_PADRAO = Color.GRAY;

    /**
     *  Desenha todos os veículos atualmente presentes no mapa.
     * 
     * @param g2d
     * @param veiculos 
     */
    public void desenharTodos(Graphics2D g2d, List<VeiculoNoMapa> veiculos) {
        for (VeiculoNoMapa v : veiculos) {

            Point2D pos = v.getPosicaoAtual();
            int x = (int) pos.getX() - TAMANHO_VEICULO / 2;
            int y = (int) pos.getY() - TAMANHO_VEICULO / 2;

            // Corpo do veículo (decidimos a cor aqui baseada no tipo)
            g2d.setColor(obterCorPorTipo(v.getTipo()));
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
    
    /**
     * 
     * 
     * @param tipo
     * @return 
     */
    private Color obterCorPorTipo(String tipo) {
        if (tipo == null) return COR_PADRAO;
        return switch (tipo.toUpperCase()) {
            case "MOTA" -> COR_MOTA;
            case "CARRO" -> COR_CARRO;
            case "CAMIAO", "CAMIÃO" -> COR_CAMIAO;
            default -> COR_PADRAO;
        };
    }
}