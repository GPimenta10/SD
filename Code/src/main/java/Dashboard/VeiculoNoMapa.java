package Dashboard;

import java.awt.*;
import java.awt.geom.Point2D;

public class VeiculoNoMapa {
    private static final int DISTANCIA_FILA = 15;
    String id;
    String tipo;
    Point2D posicaoAtual;
    Point2D origem;
    Point2D destino;
    Point2D posicaoSemaforo; // onde deve parar se vermelho
    String chaveSemaforo; // identificador do semáforo a verificar
    double progresso;
    double velocidade;
    boolean parado;
    int posicaoNaFila = -1;

    VeiculoNoMapa(String id, String tipo, Point2D origem, Point2D destino, Point2D posicaoSemaforo, String chaveSemaforo) {
        this.id = id;
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
        this.posicaoSemaforo = posicaoSemaforo;
        this.chaveSemaforo = chaveSemaforo;
        this.posicaoAtual = new Point2D.Double(origem.getX(), origem.getY());
        this.progresso = 0.0;
        this.parado = false;

        switch (tipo) {
            case "MOTA" -> this.velocidade = 2.0;
            case "CARRO" -> this.velocidade = 1.5;
            case "CAMIAO" -> this.velocidade = 1.0;
            default -> this.velocidade = 1.5;
        }
    }

    /**
     *
     *
     * @param semaforoVerde
     * @param posicaoFila
     */
    void atualizar(boolean semaforoVerde, int posicaoFila) {
        this.posicaoNaFila = posicaoFila;

        double distTotal = origem.distance(destino);

        // Se já está parado na fila e o semáforo continua vermelho, mantém-se lá
        if (parado && !semaforoVerde) {
            pararNaFila();
            return;
        }

        // 1) Avança normalmente
        parado = false;
        progresso += velocidade / 130.0;
        if (progresso >= 1.0) progresso = 1.0;

        double x = origem.getX() + (destino.getX() - origem.getX()) * progresso;
        double y = origem.getY() + (destino.getY() - origem.getY()) * progresso;
        posicaoAtual.setLocation(x, y);

        // 2) Se o semáforo está vermelho, ver se JÁ CHEGOU à posição onde devia parar
        if (!semaforoVerde && posicaoSemaforo != null && posicaoNaFila >= 0) {

            double distSemaforo = origem.distance(posicaoSemaforo);
            double recuo = posicaoNaFila * DISTANCIA_FILA;
            double distanciaParada = distSemaforo - 30 - recuo;

            // Evita valor negativo por segurança
            if (distanciaParada < 0) distanciaParada = 0;

            double distPercorrida = origem.distance(posicaoAtual);

            // Só quando já percorreu ATÉ (ou para além) do ponto de paragem é que o encostamos na fila
            if (distPercorrida >= distanciaParada) {
                pararNaFila();  // reposiciona exatamente na fila
            }
        }
    }
    /**
     * Para o veículo na fila, ao lado do semáforo
     *
     */
    private void pararNaFila() {
        parado = true;

        if (posicaoSemaforo == null) return;

        // Calcula direção da via
        double dx = destino.getX() - origem.getX();
        double dy = destino.getY() - origem.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);

        // Vetor unitário da direção
        double ux = dx / dist;
        double uy = dy / dist;

        // Posição do semáforo na via (projetada)
        double distSemaforo = origem.distance(posicaoSemaforo);

        // Recua conforme posição na fila
        double recuo = posicaoNaFila * DISTANCIA_FILA;
        double distanciaParada = distSemaforo - 30 - recuo;

        double x = origem.getX() + ux * distanciaParada;
        double y = origem.getY() + uy * distanciaParada;

        posicaoAtual.setLocation(x, y);
        progresso = distanciaParada / dist;
    }

    /**
     *
     *
     * @return
     */
    boolean ultrapassouSemaforo() {
        if (posicaoSemaforo == null) return true;
        double distanciaTotal = origem.distance(destino);
        double distanciaAteSemaforo = origem.distance(posicaoSemaforo);
        double progressoAteSemaforo = distanciaAteSemaforo / distanciaTotal;
        return progresso > progressoAteSemaforo + 0.05;
    }

    /**
     *
     *
     * @return
     */
    boolean chegouAoDestino() {
        return progresso >= 1.0;
    }

    /**
     *
     *
     * @return
     */
    Color getCor() {
        return switch (tipo) {
            case "MOTA" -> new Color(255, 193, 7);
            case "CARRO" -> new Color(33, 150, 243);
            case "CAMIAO" -> new Color(244, 67, 54);
            default -> Color.GRAY;
        };
    }
}