package Cruzamento;

import Collections.Queue.LinkedQueue;
import Collections.Exceptions.EmptyCollectionException;
import Veiculo.Veiculo;
import Veiculo.TipoVeiculo;
import OutrasClasses.ComunicadorSocket;

/**
 * Semáforo - Thread que controla fluxo de veículos num cruzamento.
 * Responsável por gerir fila, alternar estados e comunicar com o dashboard.
 */
public class Semaforo extends Thread {

    private final String nome;
    private final Cruzamento3 cruzamento;

    private final long tempoVerdeMs;
    private final long tempoVermelhoMs;
    private final long tempoPassagemBase;

    private volatile boolean verde;
    private volatile boolean executando = true;

    private final Object controlador;
    private Semaforo outroSemaforo;

    private final LinkedQueue<Veiculo> fila;
    private final int capacidadeMaxima;

    private int tamanhoMaximoFila = 0;
    private long somaFila = 0;
    private long contagemMedicoes = 0;
    private int totalVeiculosProcessados = 0;

    private static final double MULTIPLICADOR_MOTO = 0.5;
    private static final double MULTIPLICADOR_CARRO = 1.0;
    private static final double MULTIPLICADOR_CAMIAO = 2.0;

    public Semaforo(String nome, long tempoVerdeMs, long tempoVermelhoMs,
                    Object controlador, boolean iniciarVerde, Cruzamento3 cruzamento) {
        this(nome, tempoVerdeMs, tempoVermelhoMs, controlador, iniciarVerde, cruzamento, 10, 500);
    }

    public Semaforo(String nome, long tempoVerdeMs, long tempoVermelhoMs,
                    Object controlador, boolean iniciarVerde, Cruzamento3 cruzamento,
                    int capacidadeMaxima, long tempoPassagemBase) {
        super("Semaforo-" + nome);
        this.nome = nome;
        this.tempoVerdeMs = tempoVerdeMs;
        this.tempoVermelhoMs = tempoVermelhoMs;
        this.controlador = controlador;
        this.verde = iniciarVerde;
        this.cruzamento = cruzamento;
        this.capacidadeMaxima = capacidadeMaxima;
        this.tempoPassagemBase = tempoPassagemBase;
        this.fila = new LinkedQueue<>();
    }

    public void setOutroSemaforo(Semaforo outro) {
        this.outroSemaforo = outro;
    }

    @Override
    public void run() {
        log("Thread iniciada (estado inicial: " + (verde ? "VERDE" : "VERMELHO") + ")");

        try {
            while (executando && !isInterrupted()) {
                if (verde) {
                    cicloVerde();
                } else {
                    aguardarVerde();
                }
            }
        } catch (InterruptedException e) {
            log("Interrompido");
            Thread.currentThread().interrupt();
        }

        log("Thread encerrada");
    }

    /**
     * Ciclo de funcionamento enquanto o semáforo está verde.
     * Processa veículos e depois muda para vermelho.
     */
    private void cicloVerde() throws InterruptedException {
        log("🟢 VERDE (" + tempoVerdeMs + "ms)");

        long fimVerde = System.currentTimeMillis() + tempoVerdeMs;

        while (System.currentTimeMillis() < fimVerde && executando) {
            processarProximoVeiculo();
        }

        if (!fila.isEmpty()) {
            log("⚠️ Fim do verde com " + fila.size() + " veículos ainda na fila");
        }

        // Passa a vermelho
        synchronized (controlador) {
            verde = false;
            controlador.notifyAll(); // avisa o outro semáforo
        }

        log("🔴 VERMELHO (" + tempoVermelhoMs + "ms)");

        // Espera durante o vermelho, mas responde rapidamente a pedidos de paragem
        long tempoRestante = tempoVermelhoMs;
        while (tempoRestante > 0 && executando) {
            long passo = Math.min(200, tempoRestante);
            Thread.sleep(passo);
            tempoRestante -= passo;
        }
    }

    /**
     * Processa o próximo veículo da fila.
     */
    private void processarProximoVeiculo() throws InterruptedException {
        Veiculo v;
        synchronized (this) {
            try {
                v = fila.dequeue();
            } catch (EmptyCollectionException e) {
                return;
            }
        }

        long tempoNaFila = System.currentTimeMillis() - v.getTimestampEntradaFila();
        v.adicionarEspera(tempoNaFila);

        log("🚗 " + v.getId() + " (" + v.getTipo() + ") esperou " + tempoNaFila + "ms");

        v.registrarPassagemSemaforo(nome);
        cruzamento.enviarParaSaida(v);

        Thread.sleep(calcularTempoPassagem(v.getTipo()));

        totalVeiculosProcessados++;
        atualizarEstatisticas();
    }

    /**
     * Tempo de passagem variável por tipo de veículo.
     */
    private long calcularTempoPassagem(TipoVeiculo tipo) {
        return switch (tipo) {
            case MOTA -> (long) (tempoPassagemBase * MULTIPLICADOR_MOTO);
            case CARRO -> (long) (tempoPassagemBase * MULTIPLICADOR_CARRO);
            case CAMIAO -> (long) (tempoPassagemBase * MULTIPLICADOR_CAMIAO);
        };
    }

    /**
     * Espera até que este semáforo fique verde.
     */
    private void aguardarVerde() throws InterruptedException {
        synchronized (controlador) {
            while (!verde && executando) {
                controlador.wait();
            }
        }
    }

    /**
     * Torna este semáforo verde e notifica os outros.
     */
    public void tornarVerde() {
        synchronized (controlador) {
            if (!verde) {
                verde = true;
                controlador.notifyAll();
            }
        }
    }

    /**
     * Adiciona veículo à fila.
     */
    public synchronized boolean adicionarVeiculo(Veiculo v) {
        if (fila.size() >= capacidadeMaxima) {
            log("❌ FILA CHEIA - Veículo " + v.getId() + " rejeitado");
            return false;
        }

        v.setTimestampEntradaFila(System.currentTimeMillis());
        fila.enqueue(v);

        int tamanhoAtual = fila.size();
        if (tamanhoAtual > tamanhoMaximoFila) tamanhoMaximoFila = tamanhoAtual;

        somaFila += tamanhoAtual;
        contagemMedicoes++;

        log("➕ " + v.getId() + " entrou na fila (" + tamanhoAtual + "/" + capacidadeMaxima + ")");
        return true;
    }

    /**
     * Atualiza estatísticas e envia para o Dashboard.
     */
    private void atualizarEstatisticas() {
        if (totalVeiculosProcessados % 5 == 0) {
            String stats = String.format(
                    "%s: processados=%d,fila_atual=%d,fila_max=%d,fila_media=%.1f,estado=%s",
                    nome,
                    totalVeiculosProcessados,
                    fila.size(),
                    tamanhoMaximoFila,
                    calcularMediaFila(),
                    verde ? "VERDE" : "VERMELHO"
            );

            ComunicadorSocket.enviarEstatisticas(
                    "Semaforo-" + nome,
                    stats,
                    cruzamento.hostDashboard,
                    cruzamento.portaDashboard
            );
        }
    }

    /**
     * Para a execução da thread de forma segura.
     */
    public void parar() {
        executando = false;
        interrupt();
        synchronized (controlador) {
            controlador.notifyAll();
        }
    }

    // ===== Getters =====
    public synchronized int getTamanhoFila() { return fila.size(); }
    public int getMaximoFila() { return tamanhoMaximoFila; }
    public double calcularMediaFila() { return contagemMedicoes > 0 ? (double) somaFila / contagemMedicoes : 0.0; }
    public int getTotalProcessados() { return totalVeiculosProcessados; }
    public boolean isVerde() { return verde; }
    public String getNome() { return nome; }

    private void log(String mensagem) {
        String msg = "[Semáforo " + nome + "] " + mensagem;
        System.out.println(msg);
        ComunicadorSocket.enviarParaDashboard(msg, cruzamento.hostDashboard, cruzamento.portaDashboard);
    }

    @Override
    public String toString() {
        return String.format(
                "Semaforo[%s, estado=%s, fila=%d/%d, processados=%d, max=%d, media=%.1f]",
                nome,
                verde ? "VERDE" : "VERMELHO",
                fila.size(),
                capacidadeMaxima,
                totalVeiculosProcessados,
                tamanhoMaximoFila,
                calcularMediaFila()
        );
    }
}
