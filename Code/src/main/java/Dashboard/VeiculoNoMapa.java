package Dashboard;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Queue;

class VeiculoNoMapa {

    private static final int DISTANCIA_FILA = 15;
    private static final double VELOCIDADE_MOTA = 2.0;
    private static final double VELOCIDADE_CARRO = 1.5;
    private static final double VELOCIDADE_CAMIAO = 1.0;
    private static final int ESPACAMENTO_VIA_DUPLA = 10;

    // Identificação
    private final String id;
    private final String tipo;
    private final double velocidade;
    private final long timestampEntrada; // NOVO: guarda quando o veículo entrou

    // Posição atual
    private Point2D posicaoAtual;
    private Point2D origem;
    private Point2D destino;
    private double progresso;
    private boolean parado;
    private int posicaoNaFila;

    // Posições ajustadas para vias bidirecionais
    private Point2D origemAjustada;
    private Point2D destinoAjustado;

    // Semáforo do segmento atual
    private Point2D posicaoSemaforo;
    private String chaveSemaforo;

    // Fila de segmentos pendentes
    private final Queue<Segmento> filaSegmentos;

    // Classe interna para representar um segmento
    private static class Segmento {
        final String origemId;
        final String destinoId;
        final Point2D posOrigem;
        final Point2D posDestino;
        final String chaveSemaforo;
        final Point2D posSemaforo;
        final Point2D posOrigemAjustada;
        final Point2D posDestinoAjustado;

        Segmento(String origemId, String destinoId, Point2D posOrigem,
                 Point2D posDestino, String chaveSemaforo, Point2D posSemaforo,
                 Point2D posOrigemAjustada, Point2D posDestinoAjustado) {
            this.origemId = origemId;
            this.destinoId = destinoId;
            this.posOrigem = posOrigem;
            this.posDestino = posDestino;
            this.chaveSemaforo = chaveSemaforo;
            this.posSemaforo = posSemaforo;
            this.posOrigemAjustada = posOrigemAjustada != null ? posOrigemAjustada : posOrigem;
            this.posDestinoAjustado = posDestinoAjustado != null ? posDestinoAjustado : posDestino;
        }
    }

    // === Construtor ===
    VeiculoNoMapa(String id, String tipo, Point2D origem, Point2D destino,
                  Point2D posicaoSemaforo, String chaveSemaforo,
                  Point2D origemAjustada, Point2D destinoAjustado) {
        this.id = id;
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
        this.origemAjustada = origemAjustada != null ? origemAjustada : origem;
        this.destinoAjustado = destinoAjustado != null ? destinoAjustado : destino;
        this.posicaoSemaforo = posicaoSemaforo;
        this.chaveSemaforo = chaveSemaforo;
        this.posicaoAtual = new Point2D.Double(this.origemAjustada.getX(), this.origemAjustada.getY());
        this.progresso = 0.0;
        this.parado = false;
        this.posicaoNaFila = -1;
        this.filaSegmentos = new LinkedList<>();
        this.timestampEntrada = System.currentTimeMillis(); // NOVO: regista entrada

        this.velocidade = switch (tipo) {
            case "MOTA" -> VELOCIDADE_MOTA;
            case "CAMIAO" -> VELOCIDADE_CAMIAO;
            default -> VELOCIDADE_CARRO;
        };
    }

    String getChaveSemaforo() {
        return chaveSemaforo;
    }

    void adicionarProximoSegmento(String origemId, String destinoId, Point2D posOrigem,
                                  Point2D posDestino, String chaveSemaforo, Point2D posSemaforo,
                                  Point2D posOrigemAjustada, Point2D posDestinoAjustado) {
        filaSegmentos.offer(new Segmento(origemId, destinoId, posOrigem, posDestino,
                chaveSemaforo, posSemaforo,
                posOrigemAjustada, posDestinoAjustado));
    }

    void atualizar(boolean semaforoVerde, int posicaoFila) {
        this.posicaoNaFila = posicaoFila;

        if (chegouAoDestino() && !filaSegmentos.isEmpty()) {
            avancarParaProximoSegmento();
        }

        if (parado && !semaforoVerde) {
            pararNaFila();
            return;
        }

        parado = false;
        progresso += velocidade / 130.0;
        if (progresso > 1.0) progresso = 1.0;

        // Usa posições ajustadas para o movimento
        double x = origemAjustada.getX() + (destinoAjustado.getX() - origemAjustada.getX()) * progresso;
        double y = origemAjustada.getY() + (destinoAjustado.getY() - origemAjustada.getY()) * progresso;
        posicaoAtual.setLocation(x, y);

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
        this.origemAjustada = proximo.posOrigemAjustada;
        this.destinoAjustado = proximo.posDestinoAjustado;
        this.chaveSemaforo = proximo.chaveSemaforo;
        this.posicaoSemaforo = proximo.posSemaforo;
        this.progresso = 0.0;
        this.parado = false;
    }

    private void pararNaFila() {
        parado = true;
        if (posicaoSemaforo == null) return;

        double dx = destinoAjustado.getX() - origemAjustada.getX();
        double dy = destinoAjustado.getY() - origemAjustada.getY();
        double dist = Math.sqrt(dx * dx + dy * dy);
        if (dist == 0) return;

        double ux = dx / dist;
        double uy = dy / dist;

        double distSemaforo = origem.distance(posicaoSemaforo);
        double recuo = posicaoNaFila * DISTANCIA_FILA;
        double distanciaParada = Math.max(0, distSemaforo - 30 - recuo);

        double x = origemAjustada.getX() + ux * distanciaParada;
        double y = origemAjustada.getY() + uy * distanciaParada;

        posicaoAtual.setLocation(x, y);
        progresso = distanciaParada / dist;
    }

    boolean ultrapassouSemaforo() {
        if (posicaoSemaforo == null) return true;
        double distanciaTotal = origem.distance(destino);
        if (distanciaTotal == 0) return true;
        double distanciaAteSemaforo = origem.distance(posicaoSemaforo);
        double progressoAteSemaforo = distanciaAteSemaforo / distanciaTotal;
        return progresso > progressoAteSemaforo + 0.05;
    }

    boolean chegouAoDestino() {
        return progresso >= 0.99;
    }

    boolean terminouTodosSegmentos() {
        return chegouAoDestino() && filaSegmentos.isEmpty();
    }

    // === Getters ===
    String getId() { return id; }
    String getTipo() { return tipo; } // NOVO: getter para tipo
    Point2D getPosicaoAtual() { return posicaoAtual; }
    boolean isParado() { return parado; }
    long getTimestampEntrada() { return timestampEntrada; } // NOVO: getter para timestamp

    // NOVO: calcula dwelling time em segundos
    long getDwellingTimeSegundos() {
        return (System.currentTimeMillis() - timestampEntrada) / 1000;
    }

    Color getCor() {
        return switch (tipo) {
            case "MOTA" -> new Color(255, 193, 7);
            case "CARRO" -> new Color(33, 150, 243);
            case "CAMIAO" -> new Color(244, 67, 54);
            default -> Color.GRAY;
        };
    }
}