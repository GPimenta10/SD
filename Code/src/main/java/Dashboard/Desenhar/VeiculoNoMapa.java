package Dashboard.Desenhar;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Representa o estado de um veículo no mapa.
 * Velocidade ajustada para garantir visibilidade mesmo com carga alta.
 */
public class VeiculoNoMapa {

    // Constantes de movimento - VELOCIDADES REDUZIDAS para melhor visualização
    private static final int DISTANCIA_FILA = 15;
    private static final double VELOCIDADE_MOTA = 2.5;   // Era 6.0
    private static final double VELOCIDADE_CARRO = 2.0;  // Era 4.5
    private static final double VELOCIDADE_CAMIAO = 1.5; // Era 3.0
    private static final double DIVISOR_VELOCIDADE = 100.0;
    private static final double PROGRESSO_MAXIMO = 1.0;
    private static final double LIMIAR_CHEGADA = 0.99;
    
    // Tempo mínimo que um veículo deve estar visível (ms)
    private static final long TEMPO_MINIMO_VISIVEL_MS = 800;

    // Estado do veículo
    private final String id;
    private final String tipo;
    private final double velocidade;
    private final long timestampCriacao;

    private Point2D posicaoAtual;
    private Point2D origem;
    private Point2D destino;
    private Point2D origemAjustada;
    private Point2D destinoAjustado;

    private double progresso;
    private boolean parado;
    private int posicaoNaFila;

    // Semáforo
    private Point2D posicaoSemaforo;
    private String chaveSemaforo;

    // Trajeto
    private final Queue<Segmento> filaSegmentos;
    private final List<String> caminhoPercorrido = new ArrayList<>();
    
    // Controlo de tempo por segmento
    private long timestampInicioSegmento;

    private static class Segmento {
        final String origemId;
        final String destinoId;
        final Point2D posOrigem;
        final Point2D posDestino;
        final String chaveSemaforo;
        final Point2D posSemaforo;
        final Point2D posOrigemAjustada;
        final Point2D posDestinoAjustado;

        Segmento(String origemId, String destinoId, Point2D posOrigem, Point2D posDestino,
                 String chaveSemaforo, Point2D posSemaforo, Point2D posOrigemAjustada,
                 Point2D posDestinoAjustado) {
            this.origemId = origemId;
            this.destinoId = destinoId;
            this.posOrigem = posOrigem;
            this.posDestino = posDestino;
            this.chaveSemaforo = chaveSemaforo;
            this.posSemaforo = posSemaforo;
            this.posOrigemAjustada = (posOrigemAjustada != null ? posOrigemAjustada : posOrigem);
            this.posDestinoAjustado = (posDestinoAjustado != null ? posDestinoAjustado : posDestino);
        }
    }

    public VeiculoNoMapa(String id, String tipo, Point2D origem, Point2D destino,
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

        this.posicaoAtual = new Point2D.Double(this.origemAjustada.getX(), this.origemAjustada.getY());
        this.progresso = 0.0;
        this.parado = false;
        this.posicaoNaFila = -1;
        this.filaSegmentos = new LinkedList<>();
        this.timestampCriacao = System.currentTimeMillis();
        this.timestampInicioSegmento = System.currentTimeMillis();

        this.velocidade = calcularVelocidade(tipo);
        caminhoPercorrido.add(origem.toString());
    }

    private double calcularVelocidade(String tipo) {
        return switch (tipo) {
            case "MOTA" -> VELOCIDADE_MOTA;
            case "CAMIAO" -> VELOCIDADE_CAMIAO;
            default -> VELOCIDADE_CARRO;
        };
    }

    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public Point2D getPosicaoAtual() { return posicaoAtual; }
    public boolean isParado() { return parado; }
    public List<String> getCaminhoPercorrido() { return new ArrayList<>(caminhoPercorrido); }

    public boolean ultrapassouSemaforo() {
        return CalculadoraMovimento.ultrapassouSemaforo(origem, destino, posicaoSemaforo, progresso);
    }

    public boolean terminouTodosSegmentos() {
        if (!chegouAoDestino()) return false;
        if (!filaSegmentos.isEmpty()) return false;
        
        // Garantir tempo mínimo de visualização
        long tempoDecorrido = System.currentTimeMillis() - timestampInicioSegmento;
        return tempoDecorrido >= TEMPO_MINIMO_VISIVEL_MS;
    }

    public String getChaveSemaforo() { return chaveSemaforo; }

    public void adicionarProximoSegmento(String origemId, String destinoId, Point2D posOrigem,
                                         Point2D posDestino, String chaveSemaforo, Point2D posSemaforo,
                                         Point2D posOrigemAjustada, Point2D posDestinoAjustado) {
        filaSegmentos.offer(new Segmento(
                origemId, destinoId, posOrigem, posDestino,
                chaveSemaforo, posSemaforo, posOrigemAjustada, posDestinoAjustado
        ));
        caminhoPercorrido.add(destinoId);
    }

    public void atualizar(boolean semaforoVerde, int posicaoFila) {
        this.posicaoNaFila = posicaoFila;

        if (chegouAoDestino() && !filaSegmentos.isEmpty()) {
            // Verificar tempo mínimo antes de avançar
            long tempoNoSegmento = System.currentTimeMillis() - timestampInicioSegmento;
            if (tempoNoSegmento >= TEMPO_MINIMO_VISIVEL_MS) {
                avancarParaProximoSegmento();
            }
        }

        if (parado && !semaforoVerde) {
            aplicarPosicaoParagem();
            return;
        }

        parado = false;
        progresso += velocidade / DIVISOR_VELOCIDADE;

        if (progresso > PROGRESSO_MAXIMO) {
            progresso = PROGRESSO_MAXIMO;
        }

        posicaoAtual = CalculadoraMovimento.calcularPosicaoInterpolada(origemAjustada, destinoAjustado, progresso);

        if (!semaforoVerde && posicaoSemaforo != null && posicaoNaFila >= 0) {
            boolean deveParar = CalculadoraMovimento.devePararNaFila(
                    origem, posicaoSemaforo, posicaoAtual, posicaoNaFila, DISTANCIA_FILA
            );
            if (deveParar) {
                aplicarPosicaoParagem();
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
        this.timestampInicioSegmento = System.currentTimeMillis();
    }

    private void aplicarPosicaoParagem() {
        parado = true;
        this.progresso = CalculadoraMovimento.calcularProgressoParagem(
                origem, posicaoSemaforo, origemAjustada, destinoAjustado, posicaoNaFila, DISTANCIA_FILA
        );
        posicaoAtual = CalculadoraMovimento.calcularPosicaoInterpolada(origemAjustada, destinoAjustado, progresso);
    }

    private boolean chegouAoDestino() {
        return progresso >= LIMIAR_CHEGADA;
    }
}