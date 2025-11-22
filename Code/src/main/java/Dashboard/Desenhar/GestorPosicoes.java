/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Desenhar;

import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Responsável por armazenar e calcular todas as posições geométricas
 * utilizadas no PainelMapa (origens, destinos, semáforos, ajustes, etc.).
 */
public class GestorPosicoes {
    private final Map<String, Point2D> posicoes = new HashMap<>();
    private final Map<String, Point2D> posicoesSemaforos = new HashMap<>();

    private static final int ESPACAMENTO_VIA_DUPLA = 10;
    
    /**
     * 
     */
    public GestorPosicoes() {
        inicializarPosicoes();
        inicializarPosicoesSemaforos();
    }
    
    /**
     * 
     * 
     * @return 
     */
    public Map<String, Point2D> getPosicoes() {
        return posicoes;
    }
    
    /**
     * 
     * 
     * @return 
     */
    public Map<String, Point2D> getPosicoesSemaforos() {
        return posicoesSemaforos;
    }
    
    /**
     * @param origem
     * @param destino
     * @return 
     */
    public Point2D[] calcularPosicoesAjustadas(String origem, String destino) {
        Point2D p1 = posicoes.get(origem);
        Point2D p2 = posicoes.get(destino);

        if (p1 == null || p2 == null) return new Point2D[]{p1, p2};

        boolean isBidirecional =
                (origem.equals("Cr1") && destino.equals("Cr2")) ||
                        (origem.equals("Cr2") && destino.equals("Cr1")) ||
                        (origem.equals("Cr2") && destino.equals("Cr3")) ||
                        (origem.equals("Cr3") && destino.equals("Cr2"));

        if (!isBidirecional) return new Point2D[]{p1, p2};

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dist = Math.sqrt(dx*dx + dy*dy);

        double perpX = -dy / dist * ESPACAMENTO_VIA_DUPLA;
        double perpY = dx / dist * ESPACAMENTO_VIA_DUPLA;

        return new Point2D[]{
                new Point2D.Double(p1.getX() + perpX, p1.getY() + perpY),
                new Point2D.Double(p2.getX() + perpX, p2.getY() + perpY)
        };
    }
    
    /**
     * 
     * 
     */
    private void inicializarPosicoes() {
        int espacoH = 225;
        int espacoV = 175;
        int margemX = 50;
        int margemY = 60;

        posicoes.put("E1", new Point2D.Double(margemX, margemY));
        posicoes.put("E2", new Point2D.Double(margemX + espacoH, margemY));
        posicoes.put("E3", new Point2D.Double(margemX + 2 * espacoH, margemY));

        posicoes.put("Cr1", new Point2D.Double(margemX, margemY + espacoV));
        posicoes.put("Cr2", new Point2D.Double(margemX + espacoH, margemY + espacoV));
        posicoes.put("Cr3", new Point2D.Double(margemX + 2 * espacoH, margemY + espacoV));

        posicoes.put("Cr4", new Point2D.Double(margemX, margemY + 2 * espacoV));
        posicoes.put("Cr5", new Point2D.Double(margemX + espacoH, margemY + 2 * espacoV));
        posicoes.put("S",   new Point2D.Double(margemX + 2 * espacoH, margemY + 2 * espacoV));
    }
    
    /**
     * 
     * 
     */
    private void inicializarPosicoesSemaforos() {
        posicoesSemaforos.put("Cr1_E1-Cr1",  new Point2D.Double(35, 200));
        posicoesSemaforos.put("Cr1_Cr2-Cr1", new Point2D.Double(90, 210));

        posicoesSemaforos.put("Cr2_E2-Cr2",  new Point2D.Double(260, 200));
        posicoesSemaforos.put("Cr2_Cr1-Cr2", new Point2D.Double(233, 258));
        posicoesSemaforos.put("Cr2_Cr3-Cr2", new Point2D.Double(315, 210));

        posicoesSemaforos.put("Cr3_E3-Cr3",  new Point2D.Double(488, 200));
        posicoesSemaforos.put("Cr3_Cr2-Cr3", new Point2D.Double(458, 258));

        posicoesSemaforos.put("Cr4_Cr1-Cr4", new Point2D.Double(35, 375));

        posicoesSemaforos.put("Cr5_Cr2-Cr5", new Point2D.Double(260, 378));
        posicoesSemaforos.put("Cr5_Cr4-Cr5", new Point2D.Double(233, 425));
    }
}


