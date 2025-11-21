/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Desenhar;

import java.awt.geom.Point2D;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

/**
 * Representa o estado de um veículo no mapa.
 * Foca-se exclusivamente em dados de posição e movimento.
 * A lógica matemática é delegada à classe utilitária CalculadoraMovimento.
 * A lógica visual (cores) foi movida para DesenharVeiculos.
 */
public class VeiculoNoMapa {

    //   CONSTANTES DE VELOCIDADE
    private static final int DISTANCIA_FILA = 15;
    private static final double VELOCIDADE_MOTA = 2.0;
    private static final double VELOCIDADE_CARRO = 1.5;
    private static final double VELOCIDADE_CAMIAO = 1.0;

    //   ESTADO
    private final String id;
    private final String tipo;
    private final double velocidade;
    private final long timestampEntrada;

    private Point2D posicaoAtual;

    // Posições lógicas (Nós do grafo: E1, Cr1, etc.) e ajustadas (visuais)
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

    /**
     * Classe Interna
     */
    private static class Segmento {
        final String origemId;
        final String destinoId;
        final Point2D posOrigem;
        final Point2D posDestino;
        final String chaveSemaforo;
        final Point2D posSemaforo;
        final Point2D posOrigemAjustada;
        final Point2D posDestinoAjustado;

        Segmento(String origemId, String destinoId, Point2D posOrigem, Point2D posDestino, String chaveSemaforo, Point2D posSemaforo, Point2D posOrigemAjustada, 
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

    /**
     * Construtor da classe
     * 
     * @param id
     * @param tipo
     * @param origem
     * @param destino
     * @param posicaoSemaforo
     * @param chaveSemaforo
     * @param origemAjustada
     * @param destinoAjustado 
     */
    public VeiculoNoMapa(String id, String tipo, Point2D origem, Point2D destino, Point2D posicaoSemaforo, String chaveSemaforo, Point2D origemAjustada, Point2D destinoAjustado) {
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
        this.timestampEntrada = System.currentTimeMillis();

        this.velocidade = switch (tipo) {
            case "MOTA" -> VELOCIDADE_MOTA;
            case "CAMIAO" -> VELOCIDADE_CAMIAO;
            default -> VELOCIDADE_CARRO;
        };

        caminhoPercorrido.add(origem.toString());
    }
    
    /**
     * 
     * 
     * @return 
     */
    public String getId() {
        return id; 
    }
    
    /**
     * 
     * 
     * @return 
     */
    public String getTipo() {
        return tipo; 
    }
    
    /**
     * 
     * 
     * @return 
     */
    public Point2D getPosicaoAtual() { 
        return posicaoAtual; 
    }
    
    /**
     * 
     * 
     * @return 
     */
    public boolean isParado() {
        return parado;
    }
    
    /**
     * 
     * @return 
     */
    public List<String> getCaminhoPercorrido() {
        return new ArrayList<>(caminhoPercorrido);
    }
    
    /**
     * 
     * @return 
     */
    public boolean ultrapassouSemaforo() {
        return CalculadoraMovimento.ultrapassouSemaforo(origem, destino, posicaoSemaforo, progresso);
    }
    
    /**
     * 
     * 
     * @return 
     */
    public boolean terminouTodosSegmentos() {
        return chegouAoDestino() && filaSegmentos.isEmpty();
    }
    
    /**
     * 
     * 
     * @return 
     */
    public String getChaveSemaforo() {
        return chaveSemaforo;
    }
    
    /**
     * 
     * 
     * @param origemId
     * @param destinoId
     * @param posOrigem
     * @param posDestino
     * @param chaveSemaforo
     * @param posSemaforo
     * @param posOrigemAjustada
     * @param posDestinoAjustado 
     */
    public void adicionarProximoSegmento(String origemId, String destinoId, Point2D posOrigem, Point2D posDestino, String chaveSemaforo, Point2D posSemaforo,
                                         Point2D posOrigemAjustada, Point2D posDestinoAjustado) {
        filaSegmentos.offer(new Segmento(
                origemId, destinoId, posOrigem, posDestino,
                chaveSemaforo, posSemaforo, posOrigemAjustada, posDestinoAjustado
        ));
        caminhoPercorrido.add(destinoId);
    }
    
    /**
     * 
     * 
     * @param semaforoVerde
     * @param posicaoFila 
     */
    public void atualizar(boolean semaforoVerde, int posicaoFila) {
        this.posicaoNaFila = posicaoFila;

        // 1. Verifica se mudamos de segmento
        if (chegouAoDestino() && !filaSegmentos.isEmpty()) {
            avancarParaProximoSegmento();
        }

        // 2. Se estivermos parados e o sinal continuar vermelho/bloqueado
        if (parado && !semaforoVerde) {
            aplicarPosicaoParagem();
            return;
        }

        // 3. Movimento Normal
        parado = false;
        progresso += velocidade / 130.0;
        if (progresso > 1.0) progresso = 1.0;

        // Delega cálculo da posição
        posicaoAtual = CalculadoraMovimento.calcularPosicaoInterpolada(origemAjustada, destinoAjustado, progresso);

        // 4. Verificar se deve parar na fila do semáforo
        if (!semaforoVerde && posicaoSemaforo != null && posicaoNaFila >= 0) {
            boolean deveParar = CalculadoraMovimento.devePararNaFila(
                    origem, posicaoSemaforo, posicaoAtual, posicaoNaFila, DISTANCIA_FILA
            );

            if (deveParar) {
                aplicarPosicaoParagem();
            }
        }
    }
    
    /**
     * 
     * 
     */
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
    
    /**
     * 
     */
    private void aplicarPosicaoParagem() {
        parado = true;
        // Delega cálculo do ponto exato de paragem
        this.progresso = CalculadoraMovimento.calcularProgressoParagem(
                origem, posicaoSemaforo, origemAjustada, destinoAjustado, posicaoNaFila, DISTANCIA_FILA
        );
        posicaoAtual = CalculadoraMovimento.calcularPosicaoInterpolada(origemAjustada, destinoAjustado, progresso);
    }
    
    /**
     * 
     * 
     * @return 
     */
    private boolean chegouAoDestino() {
        return progresso >= 0.99;
    }
}
