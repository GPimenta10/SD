package Dashboard.Desenhar;

import java.awt.geom.Point2D;

/**
 * Classe utilitária responsável pelos cálculos geométricos de movimento.
 * Extraída de VeiculoNoMapa para desacoplamento.
 */
public class CalculadoraMovimento {

    /**
     * Calcula a posição atual baseada na interpolação linear entre origem e destino.
     */
    public static Point2D calcularPosicaoInterpolada(Point2D origem, Point2D destino, double progresso) {
        double x = origem.getX() + (destino.getX() - origem.getX()) * progresso;
        double y = origem.getY() + (destino.getY() - origem.getY()) * progresso;
        return new Point2D.Double(x, y);
    }

    /**
     * Calcula o progresso necessário para parar na fila antes do semáforo.
     */
    public static double calcularProgressoParagem(Point2D origemLogica, Point2D semaforo, 
                                                  Point2D origemAjustada, Point2D destinoAjustado,
                                                  int posicaoNaFila, int distanciaFila) {
        if (semaforo == null) return 0.0;

        // Distância total visual do segmento
        double distTotal = origemAjustada.distance(destinoAjustado);
        if (distTotal == 0) return 0.0;

        // Distância lógica até o semáforo (usada para calcular onde parar)
        double distSemaforo = origemLogica.distance(semaforo);
        double recuo = posicaoNaFila * distanciaFila;

        // 20 é uma margem de segurança visual antes do semáforo
        double distanciaParada = Math.max(0, distSemaforo - 20 - recuo);

        // O progresso é a razão entre a distância de parada e a distância total
        return distanciaParada / distTotal;
    }

    /**
     * Verifica se a distância percorrida excede o ponto de paragem calculado.
     */
    public static boolean devePararNaFila(Point2D origemLogica, Point2D semaforo, Point2D posicaoAtual,
                                          int posicaoNaFila, int distanciaFila) {
        if (semaforo == null) return false;

        double distSemaforo = origemLogica.distance(semaforo);
        double recuo = posicaoNaFila * distanciaFila;

        // 30 é a margem de deteção (um pouco maior que a margem de paragem visual de 20)
        double distanciaParada = Math.max(0, distSemaforo - 30 - recuo);
        double distPercorrida = origemLogica.distance(posicaoAtual);

        return distPercorrida >= distanciaParada;
    }

    /**
     * Verifica se o veículo já passou fisicamente a posição do semáforo.
     */
    public static boolean ultrapassouSemaforo(Point2D origemLogica, Point2D destinoLogico, 
                                              Point2D semaforo, double progressoAtual) {
        if (semaforo == null) return true;

        double distTotal = origemLogica.distance(destinoLogico);
        if (distTotal == 0) return true;

        double distSemaforo = origemLogica.distance(semaforo);
        double progSemaforo = distSemaforo / distTotal;

        // Margem de 0.05 para garantir que não pára exatamente "em cima" da linha
        return progressoAtual > progSemaforo + 0.05;
    }
}