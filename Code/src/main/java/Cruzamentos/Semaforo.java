package Cruzamentos;

import Veiculo.Veiculo;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe que representa um Semáforo controlado por um monitor partilhado.
 * Apenas um semáforo por cruzamento pode estar verde de cada vez.
 * As threads sincronizam-se entre si usando wait() e notifyAll().
 *
 * CORREÇÃO: Agora envia veículos ao próximo nó após passarem
 */
public class Semaforo extends Thread {

    private final String nome;
    private final FilaVeiculos fila;
    private final long tempoVerdeMs;
    private final Cruzamento cruzamento;  // NOVO: referência ao cruzamento

    // Controle de sincronização
    private final Object lock;
    private final int id;
    private final int totalSemaforos;
    private final AtomicInteger semaforoAtivo;

    private final AtomicBoolean ativo = new AtomicBoolean(true);

    /**
     * Construtor do semáforo coordenado.
     *
     * @param nome Nome do semáforo
     * @param fila Fila associada
     * @param tempoVerdeMs Tempo de verde (ms)
     * @param lock Objeto partilhado de sincronização
     * @param id Identificador (0, 1, 2, ...)
     * @param totalSemaforos Número total de semáforos no cruzamento
     * @param semaforoAtivo Referência ao índice do semáforo atualmente ativo
     * @param cruzamento Referência ao cruzamento (para enviar veículos)
     */
    public Semaforo(String nome, FilaVeiculos fila, long tempoVerdeMs,
                    Object lock, int id, int totalSemaforos,
                    AtomicInteger semaforoAtivo, Cruzamento cruzamento) {
        super(nome);
        this.nome = nome;
        this.fila = fila;
        this.tempoVerdeMs = tempoVerdeMs;
        this.lock = lock;
        this.id = id;
        this.totalSemaforos = totalSemaforos;
        this.semaforoAtivo = semaforoAtivo;
        this.cruzamento = cruzamento;  // NOVO
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (ativo.get()) {
                synchronized (lock) {
                    // Espera até ser a sua vez de ficar verde
                    while (ativo.get() && semaforoAtivo.get() != id) {
                        lock.wait();
                    }

                    if (!ativo.get()) break;

                    // --- Semáforo fica verde ---
                    System.out.println("[Semaforo] " + nome + " = VERDE");

                    long inicio = System.currentTimeMillis();
                    while (System.currentTimeMillis() - inicio < tempoVerdeMs && ativo.get()) {
                        Veiculo v = fila.removerSeDisponivel();

                        if (v != null) {
                            System.out.printf("[Semaforo %s] Veículo passou: %s%n",
                                    nome, v.getId());

                            // NOVO: Envia o veículo ao próximo nó
                            cruzamento.enviarVeiculoAposPassarSemaforo(v, fila);

                            Thread.sleep(200); // tempo entre veículos (t_sem)
                        } else {
                            Thread.sleep(50); // espera curta se fila estiver vazia
                        }
                    }

                    // --- Semáforo muda para vermelho ---
                    System.out.println("[Semaforo] " + nome + " = VERMELHO");

                    // Atualiza o próximo semáforo e acorda todos
                    semaforoAtivo.set((id + 1) % totalSemaforos);
                    lock.notifyAll();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("[Semaforo " + nome + "] interrompido.");
        }
    }

    /** Para a thread de forma segura */
    public void parar() {
        ativo.set(false);
        synchronized (lock) {
            lock.notifyAll();
        }
        interrupt();
        System.out.println("[Semaforo] " + nome + " encerrado.");
    }

    // === Getters ===
    public String getNome() { return nome; }
    public int getTamanhoFila() { return fila.getTamanhoAtual(); }
}