package Cruzamentos;

import Veiculo.Veiculo;

/**
 * Classe responsável por controlar um semáforo associado a uma fila.
 * Funciona em ciclo: VERDE durante X ms → VERMELHO durante Y ms → passa ao próximo semáforo.
 *
 */
public class Semaforo extends Thread {
    private final int semaforoId;
    private final MonitorSemaforos monitorSemaforos;
    private final long duracaoSinalVerdeMs;
    private final long duracaoSinalVermelhoMs = 4000;
    private final String origem;

    private final FilaVeiculos filaVeiculos;
    private final Cruzamento cruzamentoAtual;

    private boolean estadoVerde = false;
    private volatile boolean semaforoAtivo = true;

    /**
     * Construtor da classe
     *
     * @param semaforoId  Identificador do semáforo
     * @param origem Identificador de qual entrada este semáforo controla a fila (Por exemplo E3 ou Cr1)
     * @param monitorSemaforos Coordena qual semáforo está verde
     * @param duracaoSinalVerdeMs Tempo de sinal verde
     * @param filaVeiculos  Fila associada
     * @param cruzamentoAtual Cruzamento ao qual pertence
     */
    public Semaforo(int semaforoId, String origem, MonitorSemaforos monitorSemaforos, long duracaoSinalVerdeMs, FilaVeiculos filaVeiculos,
                    Cruzamento cruzamentoAtual) {

        this.semaforoId = semaforoId;
        this.monitorSemaforos = monitorSemaforos;
        this.duracaoSinalVerdeMs = duracaoSinalVerdeMs;
        this.filaVeiculos = filaVeiculos;
        this.origem = origem;
        this.cruzamentoAtual = cruzamentoAtual;

        setDaemon(true);
    }

    /**
     * Obter o ID do semaforo
     *
     * @return ID Semaforo
     */
    public int getIdSemaforo() {
        return semaforoId;
    }

    /**
     * Obter a direção física de onde os veículos entram neste cruzamento
     *
     * @return Identificador de qual entrada este semáforo controla a fila (Por exemplo E3 ou Cr1)
     */
    public String getOrigem() {
        return origem;
    }

    /**
     * Obter a cor do semaforo
     *
     * @return True se estiver verde, false se estiver Vermelho
     */
    public boolean isVerde() {
        return estadoVerde;
    }

    /**
     * Obter tamanho da fila
     *
     * @return O Tamnho atual da fila (nº de veiculos)
     */
    public int getTamanhoFila() {
        return filaVeiculos.getTamanhoAtual();
    }

    /**
     *
     */
    @Override
    public void run() {
        try {
            while (semaforoAtivo) {
                // Espera até ser a vez deste semáforo ficar VERDE
                monitorSemaforos.esperarVez(semaforoId);

                if (!semaforoAtivo) {
                    break;
                }

                estadoVerde = true;

                // System.out.println("[Semaforo " + semaforoId + "] VERDE");

                long inicioVerde = System.currentTimeMillis();

                while (semaforoAtivo && (System.currentTimeMillis() - inicioVerde) < duracaoSinalVerdeMs) {
                    Veiculo veiculo = filaVeiculos.removerSeDisponivel();

                    if (veiculo != null) {
                        // System.out.println("[Semaforo " + semaforoId + "] Veículo passou: " + veiculo.getId());

                        // Envia o veículo para o próximo cruzamento
                        cruzamentoAtual.enviarVeiculoAposPassarSemaforo(veiculo, filaVeiculos);

                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ignored) { break; }
                    } else {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) { break; }
                    }
                }

                estadoVerde = false;

                // System.out.println("[Semaforo " + semaforoId + "] VERMELHO");

                try { Thread.sleep(duracaoSinalVermelhoMs); }
                catch (InterruptedException ignored) { }

                // Passa o controlo ao próximo semáforo
                monitorSemaforos.proximaVez();
            }
        } catch (InterruptedException ignored) {
            // ignorar interrupções quando o semáforo é encerrado
        }

        // System.out.println("[Semaforo " + semaforoId + "] Terminado.");
    }

    /**
     * Para o semáforo de forma segura.
     */
    public void pararSemaforo() {
        semaforoAtivo = false;
        interrupt();
        monitorSemaforos.acordarTodos();
    }
}
