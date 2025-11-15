package Dashboard;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Queue;

class VeiculoNoMapa {  // ✅ package-private (não precisa ser public)

    private static final int DISTANCIA_FILA = 15;
    private static final double VELOCIDADE_MOTA = 2.0;
    private static final double VELOCIDADE_CARRO = 1.5;
    private static final double VELOCIDADE_CAMIAO = 1.0;

    // === Identificação ===
    private final String id;
    private final String tipo;
    private final double velocidade;

    // === Posição atual ===
    private Point2D posicaoAtual;
    private Point2D origem;
    private Point2D destino;
    private double progresso;
    private boolean parado;
    private int posicaoNaFila;

    // === Semáforo do segmento atual ===
    private Point2D posicaoSemaforo;
    private String chaveSemaforo;

    // === ✅ NOVO: Fila de segmentos pendentes ===
    private final Queue<Segmento> filaSegmentos;

    // === Classe interna para representar um segmento ===
    private static class Segmento {
        final String origemId;
        final String destinoId;
        final Point2D posOrigem;
        final Point2D posDestino;
        final String chaveSemaforo;
        final Point2D posSemaforo;

        Segmento(String origemId, String destinoId, Point2D posOrigem,
                 Point2D posDestino, String chaveSemaforo, Point2D posSemaforo) {
            this.origemId = origemId;
            this.destinoId = destinoId;
            this.posOrigem = posOrigem;
            this.posDestino = posDestino;
            this.chaveSemaforo = chaveSemaforo;
            this.posSemaforo = posSemaforo;
        }
    }

    // === Construtor ===
    VeiculoNoMapa(String id, String tipo, Point2D origem, Point2D destino,
                  Point2D posicaoSemaforo, String chaveSemaforo) {
        this.id = id;
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
        this.posicaoSemaforo = posicaoSemaforo;
        this.chaveSemaforo = chaveSemaforo;
        this.posicaoAtual = new Point2D.Double(origem.getX(), origem.getY());
        this.progresso = 0.0;
        this.parado = false;
        this.posicaoNaFila = -1;
        this.filaSegmentos = new LinkedList<>();

        // Define velocidade baseada no tipo
        this.velocidade = switch (tipo) {
            case "MOTA" -> VELOCIDADE_MOTA;
            case "CAMIAO" -> VELOCIDADE_CAMIAO;
            default -> VELOCIDADE_CARRO;
        };
    }

    String getChaveSemaforo() {
        return chaveSemaforo;
    }

    void adicionarProximoSegmento(String origemId, String destinoId,
                                  Point2D posOrigem, Point2D posDestino,
                                  String chaveSemaforo, Point2D posSemaforo) {
        filaSegmentos.offer(new Segmento(origemId, destinoId, posOrigem,
                posDestino, chaveSemaforo, posSemaforo));
    }

    // === Atualização da posição (chamada pelo timer) ===
    void atualizar(boolean semaforoVerde, int posicaoFila) {
        this.posicaoNaFila = posicaoFila;

        // Se chegou ao destino E tem próximo segmento → avança
        if (chegouAoDestino() && !filaSegmentos.isEmpty()) {
            avancarParaProximoSegmento();
        }

        // Se está parado e semáforo vermelho → mantém parado
        if (parado && !semaforoVerde) {
            pararNaFila();
            return;
        }

        // Movimento normal
        parado = false;
        progresso += velocidade / 130.0;
        if (progresso > 1.0) progresso = 1.0;

        double x = origem.getX() + (destino.getX() - origem.getX()) * progresso;
        double y = origem.getY() + (destino.getY() - origem.getY()) * progresso;
        posicaoAtual.setLocation(x, y);

        // Verifica se deve parar no semáforo vermelho
        if (!semaforoVerde && posicaoSemaforo != null && posicaoNaFila >= 0) {
            double distSemaforo = origem.distance(posicaoSemaforo);
            double recuo = posicaoNaFila * DISTANCIA_FILA;
            double distanciaParada = Math.max(0, distSemaforo - 30 - recuo);
            double distPercorrida = origem.distance(posicaoAtual);

            if (distPercorrida >= distanciaParada) {
                pararNaFila();
            }
        }
    }

    private void avancarParaProximoSegmento() {
        Segmento proximo = filaSegmentos.poll();
        if (proximo == null) return;

        this.origem = proximo.posOrigem;
        this.destino = proximo.posDestino;
        this.chaveSemaforo = proximo.chaveSemaforo;
        this.posicaoSemaforo = proximo.posSemaforo;
        this.progresso = 0.0;
        this.parado = false;

        System.out.printf("[VeiculoNoMapa] %s avançou: %s → %s%n",
                id, proximo.origemId, proximo.destinoId);
    }

    // === Para o veículo na fila ===
    private void pararNaFila() {
        parado = true;
        if (posicaoSemaforo == null) return;

        double dx = destino.getX() - origem.getX();
        double dy = destino.getY() - origem.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) return;

        double ux = dx / dist;
        double uy = dy / dist;

        double distSemaforo = origem.distance(posicaoSemaforo);
        double recuo = posicaoNaFila * DISTANCIA_FILA;
        double distanciaParada = Math.max(0, distSemaforo - 30 - recuo);

        double x = origem.getX() + ux * distanciaParada;
        double y = origem.getY() + uy * distanciaParada;

        posicaoAtual.setLocation(x, y);
        progresso = distanciaParada / dist;
    }

    // === Verifica se ultrapassou o semáforo ===
    boolean ultrapassouSemaforo() {
        if (posicaoSemaforo == null) return true;
        double distanciaTotal = origem.distance(destino);
        if (distanciaTotal == 0) return true;
        double distanciaAteSemaforo = origem.distance(posicaoSemaforo);
        double progressoAteSemaforo = distanciaAteSemaforo / distanciaTotal;
        return progresso > progressoAteSemaforo + 0.05;
    }

    // === Verifica se chegou ao destino (deste segmento) ===
    boolean chegouAoDestino() {
        return progresso >= 0.99; // margem de erro
    }

    // === Verifica se terminou TODOS os segmentos ===
    boolean terminouTodosSegmentos() {
        return chegouAoDestino() && filaSegmentos.isEmpty();
    }

    // === Getters ===
    String getId() { return id; }
    Point2D getPosicaoAtual() { return posicaoAtual; }
    boolean isParado() { return parado; }

    Color getCor() {
        return switch (tipo) {
            case "MOTA" -> new Color(255, 193, 7);
            case "CARRO" -> new Color(33, 150, 243);
            case "CAMIAO" -> new Color(244, 67, 54);
            default -> Color.GRAY;
        };
    }
}