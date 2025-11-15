package Cruzamentos;

import Veiculo.Veiculo;

public class Semaforo extends Thread {

    private final int semaforoId;
    private final MonitorSemaforos monitorSemaforos;
    private final long duracaoSinalVerdeMs;
    private final String origem; // NOVO

    private FilaVeiculos filaVeiculos;
    private final Cruzamento cruzamentoAtual;

    private boolean estadoVerde;
    private volatile boolean semaforoAtivo = true;

    public Semaforo(int semaforoId, String origem, MonitorSemaforos monitorSemaforos, long duracaoSinalVerdeMs, FilaVeiculos filaVeiculos, Cruzamento  cruzamentoAtual) {
        this.semaforoId = semaforoId;
        this.monitorSemaforos = monitorSemaforos;
        this.duracaoSinalVerdeMs = duracaoSinalVerdeMs;
        this.filaVeiculos = filaVeiculos;
        this.origem = origem; // NOVO
        this.cruzamentoAtual = cruzamentoAtual;

        setDaemon(true);
    }

    public int getIdSemaforo() {
        return semaforoId;
    }

    public String getOrigem() {
        return origem;
    }

    public FilaVeiculos getFilaVeiculos() {
        return filaVeiculos;
    }

    public boolean isVerde() {
        return estadoVerde;
    }

    public int getTamanhoFila() {
        return filaVeiculos.getTamanhoAtual();
    }

    @Override
    public void run() {
        try {
            while (semaforoAtivo) {
                // Espera pela vez deste semáforo
                monitorSemaforos.esperarVez(semaforoId);
                if (!semaforoAtivo) break;

                // --- SINAL VERDE ---
                estadoVerde = true;
                System.out.println("\nSemaforo " + semaforoId + " = VERDE");

                long inicioVerde = System.currentTimeMillis();

                while (semaforoAtivo && (System.currentTimeMillis() - inicioVerde) < duracaoSinalVerdeMs) {
                    Veiculo veiculo = filaVeiculos.removerSeDisponivel();

                    if (veiculo != null) {
                        System.out.println("[Semaforo " + semaforoId + "] Veiculo passou: " + veiculo.getId());

                        // Envia para o próximo cruzamento, se existir
                        if (cruzamentoAtual != null) {
                            cruzamentoAtual.enviarVeiculoAposPassarSemaforo(veiculo, filaVeiculos);
                        }

                        try { Thread.sleep(300); }
                        catch (InterruptedException e) { break; }
                    } else {
                        // Se não há veículos, aguarda um pouco
                        try { Thread.sleep(100); }
                        catch (InterruptedException e) { break; }
                    }
                }

                // --- SINAL VERMELHO ---
                System.out.println("Semaforo " + semaforoId + " = VERMELHO");
                estadoVerde = false;

                Thread.sleep(2000);

                // Passa para o próximo semáforo
                monitorSemaforos.proximaVez();
            }

        } catch (InterruptedException e) {
            // ignora, é esperado durante parar()
        }
        System.out.println("Semáforo " + semaforoId + " terminado");
    }

    public void pararSemaforo() {
        semaforoAtivo = false;
        interrupt();
        monitorSemaforos.acordarTodos();
    }
}
