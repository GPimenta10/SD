package Dashboard.Paineis;

import java.awt.*;
import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Representa um veículo animado no mapa do Dashboard.
 * Lógica original intocada. Apenas organização e estilo modernizado.
 */
class VeiculoNoMapa {

    // ===========================
    //   CONSTANTES DE MOVIMENTO
    // ===========================
    private static final int DISTANCIA_FILA = 15;
    private static final double VELOCIDADE_MOTA = 2.0;
    private static final double VELOCIDADE_CARRO = 1.5;
    private static final double VELOCIDADE_CAMIAO = 1.0;

    private static final int ESPACAMENTO_VIA_DUPLA = 10;

    // ===========================
    //   IDENTIFICAÇÃO DO VEÍCULO
    // ===========================
    private final String id;
    private final String tipo;
    private final double velocidade;
    private final long timestampEntrada;

    // ===========================
    //   ESTADO ATUAL DO VEÍCULO
    // ===========================
    private Point2D posicaoAtual;
    private Point2D origem;
    private Point2D destino;
    private Point2D origemAjustada;
    private Point2D destinoAjustado;

    private double progresso;
    private boolean parado;
    private int posicaoNaFila;

    // ===========================
    //   SEMÁFORO ASSOCIADO
    // ===========================
    private Point2D posicaoSemaforo;
    private String chaveSemaforo;

    // ===========================
    //   FILA DE SEGMENTOS
    // ===========================
    private final Queue<Segmento> filaSegmentos;

    // ===========================
    //   CLASSE DO SEGMENTO
    // ===========================
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

            this.posOrigemAjustada =
                    (posOrigemAjustada != null ? posOrigemAjustada : posOrigem);
            this.posDestinoAjustado =
                    (posDestinoAjustado != null ? posDestinoAjustado : posDestino);
        }
    }

    // ===========================
    //       CONSTRUTOR
    // ===========================
    VeiculoNoMapa(String id, String tipo, Point2D origem, Point2D destino,
                  Point2D posicaoSemaforo, String chaveSemaforo,
                  Point2D origemAjustada, Point2D destinoAjustado) {

        this.id = id;
        this.tipo = tipo;

        this.origem = origem;
        this.destino = destino;

        this.origemAjustada = (origemAjustada != null ? origemAjustada : origem);
        this.destinoAjustado = (destinoAjustado != null ? destinoAjustado : destino);

        this.posicaoSemaforo = posicaoSemaforo;
        this.chaveSemaforo = chaveSemaforo;

        this.posicaoAtual =
                new Point2D.Double(this.origemAjustada.getX(), this.origemAjustada.getY());

        this.progresso = 0.0;
        this.parado = false;
        this.posicaoNaFila = -1;

        this.filaSegmentos = new LinkedList<>();
        this.timestampEntrada = System.currentTimeMillis();

        // Velocidade baseada no tipo
        this.velocidade = switch (tipo) {
            case "MOTA" -> VELOCIDADE_MOTA;
            case "CAMIAO" -> VELOCIDADE_CAMIAO;
            default -> VELOCIDADE_CARRO;
        };
    }

    // ===========================
    //     MÉTODOS PRINCIPAIS
    // ===========================
    String getChaveSemaforo() {
        return chaveSemaforo;
    }

    void adicionarProximoSegmento(String origemId, String destinoId,
                                  Point2D posOrigem, Point2D posDestino,
                                  String chaveSemaforo, Point2D posSemaforo,
                                  Point2D posOrigemAjustada, Point2D posDestinoAjustado) {

        filaSegmentos.offer(new Segmento(
                origemId, destinoId,
                posOrigem, posDestino,
                chaveSemaforo, posSemaforo,
                posOrigemAjustada, posDestinoAjustado
        ));
    }

    void atualizar(boolean semaforoVerde, int posicaoFila) {
        this.posicaoNaFila = posicaoFila;

        // Mudança de segmento
        if (chegouAoDestino() && !filaSegmentos.isEmpty()) {
            avancarParaProximoSegmento();
        }

        // Parado na fila
        if (parado && !semaforoVerde) {
            pararNaFila();
            return;
        }

        // Movimento
        parado = false;
        progresso += velocidade / 130.0;
        if (progresso > 1.0) progresso = 1.0;

        double x = origemAjustada.getX()
                + (destinoAjustado.getX() - origemAjustada.getX()) * progresso;
        double y = origemAjustada.getY()
                + (destinoAjustado.getY() - origemAjustada.getY()) * progresso;

        posicaoAtual.setLocation(x, y);

        // Avaliar proximidade ao semáforo
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
        Segmento seg = filaSegmentos.poll();
        if (seg == null) return;

        this.origem = seg.posOrigem;
        this.destino = seg.posDestino;

        this.origemAjustada = seg.posOrigemAjustada;
        this.destinoAjustado = seg.posDestinoAjustado;

        this.chaveSemaforo = seg.chaveSemaforo;
        this.posicaoSemaforo = seg.posSemaforo;

        this.progresso = 0.0;
        this.parado = false;
    }

    private void pararNaFila() {
        parado = true;

        if (posicaoSemaforo == null) return;

        double dx = destinoAjustado.getX() - origemAjustada.getX();
        double dy = destinoAjustado.getY() - origemAjustada.getY();

        double dist = Math.hypot(dx, dy);
        if (dist == 0) return;

        double ux = dx / dist;
        double uy = dy / dist;

        double distSemaforo = origem.distance(posicaoSemaforo);
        double recuo = posicaoNaFila * DISTANCIA_FILA;

        double distanciaParada = Math.max(0, distSemaforo - 20 - recuo);

        double x = origemAjustada.getX() + ux * distanciaParada;
        double y = origemAjustada.getY() + uy * distanciaParada;

        posicaoAtual.setLocation(x, y);
        progresso = distanciaParada / dist;
    }

    // ===========================
    //        ESTADOS
    // ===========================
    boolean ultrapassouSemaforo() {
        if (posicaoSemaforo == null) return true;

        double distTotal = origem.distance(destino);
        if (distTotal == 0) return true;

        double distSemaforo = origem.distance(posicaoSemaforo);
        double progSemaforo = distSemaforo / distTotal;

        return progresso > progSemaforo + 0.05;
    }

    boolean chegouAoDestino() {
        return progresso >= 0.99;
    }

    boolean terminouTodosSegmentos() {
        return chegouAoDestino() && filaSegmentos.isEmpty();
    }

    // ===========================
    //         GETTERS
    // ===========================
    String getId() { return id; }
    String getTipo() { return tipo; }
    Point2D getPosicaoAtual() { return posicaoAtual; }
    boolean isParado() { return parado; }

    long getDwellingTimeSegundos() {
        return (System.currentTimeMillis() - timestampEntrada) / 1000;
    }

    Color getCor() {
        return switch (tipo) {
            case "MOTA" -> new Color(255, 193, 7);   // Amarelo
            case "CARRO" -> new Color(33, 150, 243); // Azul
            case "CAMIAO" -> new Color(130, 109, 56); // Vermelho
            default -> Color.GRAY;
        };
    }
}