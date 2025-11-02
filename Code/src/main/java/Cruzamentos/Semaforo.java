package Cruzamentos;

import Dashboard.ComunicadorDashboard;
import Veiculo.Veiculo;
import java.io.PrintWriter;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Classe responsável por controlar o fluxo de veículos de uma fila.
 * O semáforo usa sincronização com wait/notify para controlar o acesso.
 */
public class Semaforo extends Thread {

    private final String nome;
    private final FilaVeiculos fila;
    private final long tempoPassagemMs;
    private final AtomicBoolean ativo = new AtomicBoolean(true);

    private volatile boolean aberto = false;
    private volatile PrintWriter socketSaida;

    private final ComunicadorDashboard dashboard = ComunicadorDashboard.getInstance();

    public Semaforo(String nome, FilaVeiculos fila, long tempoPassagemMs) {
        super(nome);
        this.nome = nome;
        this.fila = fila;
        this.tempoPassagemMs = tempoPassagemMs;
        setDaemon(true);
    }

    /** Define o socket de saída (para enviar veículos ao próximo nó). */
    public void setSocketSaida(PrintWriter out) {
        this.socketSaida = out;
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
                synchronized (this) {
                    while (!aberto && ativo.get()) {
                        wait(); // Espera até ser notificado
                    }
                }

                v = fila.removerSeDisponivel();

                if (v != null && socketSaida != null) {
                    socketSaida.println(String.format("VEICULO|%s|%s|%d",
                            v.getId(), v.getTipo(), v.getTempoChegada()));
                    socketSaida.flush();
                    Thread.sleep(tempoPassagemMs);
                } else {
                    Thread.sleep(30);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ignored) {}
        }
    }

    public String getEstatisticas() {
        return String.format("[%s] aberto=%s fila=%d", nome, aberto, fila.getTamanhoAtual());
    }

    /** Fecha o semáforo e encerra a thread. */
    public void parar() {
        ativo.set(false);
        synchronized (this) {
            notifyAll(); // acorda se estiver à espera
        }
        interrupt();
    }
}
