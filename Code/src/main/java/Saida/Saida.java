package Saida;

import Dashboard.Logs.TipoLog;
import Utils.EnviarLogs;
import Veiculo.Veiculo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa o ponto de saída do sistema de tráfego.
 *
 * Esta classe é responsável por:
 *  Receber veículos que completam o percurso no sistema
 *  Registar o tempo de saída e calcular o tempo de permanência
 *  Comunicar com o Dashboard para reportar estatísticas
 *  Manter histórico de todos os veículos que saíram
 *
 * A Saída opera como um componente independente com servidor próprio
 * que aceita conexões de cruzamentos finais (Cr3 e Cr5).
 */
public class Saida {

    private final int portaServidor;
    private final String ipDashboard;
    private final int portaDashboard;

    private final ServidorSaida servidorSaida;
    private final ClienteSaidaDash clienteSaidaDash;

    private final List<Veiculo> veiculosSaidos = Collections.synchronizedList(new ArrayList<>());

    /**
     * Construtor da classe
     *
     * @param portaServidor Porta TCP onde o servidor da Saída irá escutar
     * @param ipDashboard Endereço IP do Dashboard
     * @param portaDashboard Porta TCP do Dashboard
     * @throws IllegalArgumentException se os parâmetros forem inválidos
     */
    public Saida(int portaServidor, String ipDashboard, int portaDashboard) {
        if (portaServidor < 1 || portaServidor > 65535) {
            throw new IllegalArgumentException("Porta do servidor inválida");
        }
        if (portaDashboard < 1 || portaDashboard > 65535) {
            throw new IllegalArgumentException("Porta do Dashboard inválida");
        }
        if (ipDashboard == null || ipDashboard.trim().isEmpty()) {
            throw new IllegalArgumentException("IP do Dashboard não pode ser null ou vazio");
        }

        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;

        this.servidorSaida = new ServidorSaida(portaServidor, this);
        this.clienteSaidaDash = new ClienteSaidaDash(ipDashboard, portaDashboard, this);
    }

    /**
     * Retorna a lista de veículos que já saíram do sistema.
     * A lista retornada é imutável para prevenir modificações externas.
     *
     * @return Lista não-modificável de veículos que saíram
     */
    public List<Veiculo> getVeiculosSaidos() {
        return Collections.unmodifiableList(veiculosSaidos);
    }

    /**
     * Inicia o servidor da Saída e a comunicação com o Dashboard.
     *
     * Arranca duas threads:
     *  Servidor TCP para receber veículos dos cruzamentos
     *  Cliente para enviar estatísticas ao Dashboard
     */
    public void iniciar() {
        EnviarLogs.enviar(
                TipoLog.SISTEMA,
                String.format("Saída iniciada na porta %d (Dashboard: %s:%d)",
                        portaServidor, ipDashboard, portaDashboard)
        );

        servidorSaida.start();
        clienteSaidaDash.start();
    }

    /**
     * Regista um veículo que saiu do sistema.
     *
     * Operações realizadas:
     *  Define o tempo de saída do veículo
     *  Calcula o tempo de permanência no sistema
     *  Adiciona à lista de veículos saídos
     *  Envia log do evento
     *  Notifica o Dashboard com as estatísticas
     *
     *
     * @param veiculo Veículo que completou o percurso e saiu do sistema
     */
    public void registarVeiculo(Veiculo veiculo) {
        long tempoSaida = System.currentTimeMillis();
        veiculo.setTempoSaida(tempoSaida);

        long tempoTotal = tempoSaida - veiculo.getTempoChegada();
        double tempoTotalSegundos = tempoTotal / 1000.0;

        veiculosSaidos.add(veiculo);

        EnviarLogs.enviar(TipoLog.VEICULO, String.format("Veículo %s (%s) saiu do sistema. Tempo total: %.2f s",
                        veiculo.getId(), veiculo.getTipo(), tempoTotalSegundos)
        );

        clienteSaidaDash.enviarVeiculoSaiu(veiculo, tempoTotalSegundos);
    }

    /**
     * Para o servidor e encerra todas as threads associadas.
     */
    public void parar() {
        servidorSaida.pararServidor();
        clienteSaidaDash.parar();
        EnviarLogs.enviar(TipoLog.SISTEMA, "Saída encerrada.");
    }
}