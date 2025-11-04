package Cruzamentos;

import Dashboard.ComunicadorDashboard;
import Veiculo.Veiculo;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe responsável por controlar o fluxo de veículos de uma fila.
 *
 * ALTERAÇÃO: Usa ObjectOutputStream para enviar veículos serializados
 * ao próximo nó, preservando todo o estado do veículo.
 */
public class Semaforo extends Thread {

    private final String nome;
    private final FilaVeiculos fila;
    private final long tempoPassagemMs;
    private final AtomicBoolean ativo = new AtomicBoolean(true);

    private volatile boolean aberto = false;
    private volatile ObjectOutputStream socketSaida;

    private final ComunicadorDashboard dashboard = ComunicadorDashboard.getInstance();

    public Semaforo(String nome, FilaVeiculos fila, long tempoPassagemMs) {
        super(nome);
        this.nome = nome;
        this.fila = fila;
        this.tempoPassagemMs = tempoPassagemMs;
        setDaemon(true);
    }

    /**
     * Define o socket de saída para enviar veículos ao próximo nó.
     */
    public void setSocketSaida(ObjectOutputStream oos) {
        this.socketSaida = oos;
    }

    /** Abre o semáforo (verde) e notifica threads à espera. */
    public synchronized void abrir() {
        if (!aberto) {
            aberto = true;
            dashboard.enviar("[Semaforo] " + nome + "=VERDE");
            notifyAll();
        }
    }

    /** Fecha o semáforo (vermelho). */
    public synchronized void fechar() {
        if (aberto) {
            aberto = false;
            dashboard.enviar("[Semaforo] " + nome + "=VERMELHO");
        }
    }

    @Override
    public void run() {
        while (ativo.get()) {
            try {
                Veiculo v;

                // Espera até o semáforo abrir
                synchronized (this) {
                    while (!aberto && ativo.get()) {
                        wait();
                    }
                }

                // Remove veículo da fila (não bloqueante)
                v = fila.removerSeDisponivel();

                if (v != null) {
                    // ✅ IMPORTANTE: Simula tempo de passagem ANTES de enviar
                    Thread.sleep(tempoPassagemMs);

                    // Verifica se tem socket de saída configurado
                    if (socketSaida != null) {
                        // Avança o veículo no caminho
                        v.avancarCaminho();

                        // Log antes de enviar
                        System.out.printf("[%s] 🚗 Veículo %s atravessou (%.1fs) -> enviando para %s%n",
                                nome, v.getId(), tempoPassagemMs/1000.0,
                                v.chegouAoDestino() ? "SAÍDA" : v.getProximoNo());

                        // Envia objeto serializado completo
                        synchronized (socketSaida) {
                            socketSaida.writeObject(v);
                            socketSaida.flush();
                        }

                    } else {
                        System.err.printf("[%s] ⚠️ Socket de saída não configurado para veículo %s%n",
                                nome, v.getId());
                    }

                } else {
                    // Fila vazia, aguarda um pouco
                    Thread.sleep(30);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                System.err.printf("[%s] ❌ Erro ao processar veículo: %s%n", nome, e.getMessage());
                e.printStackTrace();
            }
        }

        System.out.printf("[%s] Thread do semáforo encerrada%n", nome);
    }

    public String getEstatisticas() {
        return String.format("[%s] aberto=%s fila=%d", nome, aberto, fila.getTamanhoAtual());
    }

    /** Fecha o semáforo e encerra a thread. */
    public void parar() {
        ativo.set(false);
        synchronized (this) {
            notifyAll();
        }
        interrupt();
    }
}