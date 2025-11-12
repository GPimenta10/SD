package Saida;

import Veiculo.Veiculo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Classe principal que representa o ponto de saída do sistema.
 * Responsável por receber veículos vindos dos cruzamentos finais
 * (Cr3 e Cr5), registar a sua chegada e comunicar as estatísticas
 * ao Dashboard.
 */
public class Saida {

    private final int portaServidor;
    private final String ipDashboard;
    private final int portaDashboard;

    private final List<Veiculo> veiculosSaidos = Collections.synchronizedList(new ArrayList<>());

    private final ThreadServidorSaida threadServidorSaida;
    private final SaidaComunicDash saidaComunicDash;

    private volatile boolean ativo = true;

    /**
     * Construtor da Saída.
     * @param portaServidor Porta TCP de receção de veículos
     * @param ipDashboard IP do Dashboard
     * @param portaDashboard Porta TCP do Dashboard
     */
    public Saida(int portaServidor, String ipDashboard, int portaDashboard) {
        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;

        this.threadServidorSaida = new ThreadServidorSaida(portaServidor, this);
        this.saidaComunicDash = new SaidaComunicDash(ipDashboard, portaDashboard, this);
    }

    /** Inicia o servidor e a comunicação com o dashboard. */
    public void iniciar() {
        System.out.printf("[Saida] Iniciada na porta %d. Dashboard: %s:%d%n",
                portaServidor, ipDashboard, portaDashboard);
        threadServidorSaida.start();
        saidaComunicDash.start();
    }

    /**
     * Regista um veículo que saiu do sistema.
     * Atualiza o tempo de saída e armazena para estatísticas.
     */
    public void registarVeiculo(Veiculo veiculo) {
        long tempoSaida = System.currentTimeMillis();
        veiculo.setTempoSaida(tempoSaida);

        long tempoTotal = veiculo.getDwellingTime();

        veiculosSaidos.add(veiculo);
        System.out.printf("[Saida] Veículo %s (%s) saiu do sistema. Tempo total: %.2f s%n",
                veiculo.getId(),
                veiculo.getTipo(),
                tempoTotal / 1000.0);
    }

    /** Lista imutável dos veículos já processados. */
    public List<Veiculo> getVeiculosSaidos() {
        return Collections.unmodifiableList(veiculosSaidos);
    }

    /** Para todas as threads e encerra o processo de saída. */
    public void parar() {
        ativo = false;
        threadServidorSaida.pararServidor();
        saidaComunicDash.parar();
        System.out.println("[Saida] Encerrada.");
    }
}
