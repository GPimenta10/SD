package Saida;

import Dashboard.Logs.TipoLog;
import Utils.EnviarLogs;
import Veiculo.Veiculo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe principal que representa o ponto de saída do sistema.
 */
public class Saida {

    private final int portaServidor;
    private final String ipDashboard;
    private final int portaDashboard;

    private final List<Veiculo> veiculosSaidos = Collections.synchronizedList(new ArrayList<>());

    private final ServidorSaida servidorSaida;
    private final ClienteSaidaDash clienteSaidaDash;

    private volatile boolean ativo = true;

    public Saida(int portaServidor, String ipDashboard, int portaDashboard) {
        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;

        this.servidorSaida = new ServidorSaida(portaServidor, this);
        this.clienteSaidaDash = new ClienteSaidaDash(ipDashboard, portaDashboard, this);
    }

    /** Inicia o servidor e a comunicação com o dashboard. */
    public void iniciar() {
        EnviarLogs.enviar(TipoLog.SISTEMA, "Saída iniciada na porta " + portaServidor + " (Dashboard: " + ipDashboard + ":" + portaDashboard + ")");

        servidorSaida.start();
        clienteSaidaDash.start();
    }

    /**
     * Regista um veículo que saiu do sistema.
     * Atualiza o tempo de saída e notifica o Dashboard.
     */
    public void registarVeiculo(Veiculo veiculo) {
        long tempoSaida = System.currentTimeMillis();
        veiculo.setTempoSaida(tempoSaida);

        long tempoTotal = veiculo.getDwellingTime();
        double tempoTotalSegundos = tempoTotal / 1000.0;

        veiculosSaidos.add(veiculo);

        EnviarLogs.enviar(TipoLog.VEICULO, "Veículo " + veiculo.getId() + " (" + veiculo.getTipo() + ") saiu do sistema. " +
                        "Tempo total: " + String.format("%.2f", tempoTotalSegundos) + " s"
        );

        clienteSaidaDash.enviarVeiculoSaiu(veiculo, tempoTotalSegundos);

        // Debug comentado (se quiseres repor no futuro):
        /*
        System.out.println("=".repeat(60));
        System.out.printf("[Saida] Veículo %s saiu do sistema%n", veiculo.getId());
        System.out.println("=".repeat(60));
        */
    }

    /** Lista imutável dos veículos já processados. */
    public List<Veiculo> getVeiculosSaidos() {
        return Collections.unmodifiableList(veiculosSaidos);
    }

    /** Para todas as threads e encerra o processo de saída. */
    public void parar() {
        ativo = false;
        servidorSaida.pararServidor();
        clienteSaidaDash.parar();

        EnviarLogs.enviar(TipoLog.SISTEMA, "Saída encerrada.");
    }
}
