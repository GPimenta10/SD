package Saida;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;
import Veiculo.Veiculo;

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

    private final String ipServidor;
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
    public Saida(String ipServidor, int portaServidor, String ipDashboard, int portaDashboard) {
        if (portaServidor < 1 || portaServidor > 65535) {
            throw new IllegalArgumentException("Porta do servidor inválida");
        }
        if (portaDashboard < 1 || portaDashboard > 65535) {
            throw new IllegalArgumentException("Porta do Dashboard inválida");
        }
        if (ipDashboard == null || ipDashboard.trim().isEmpty()) {
            throw new IllegalArgumentException("IP do Dashboard não pode ser null ou vazio");
        }

        this.ipServidor = (ipServidor == null || ipServidor.isEmpty()) ? "localhost" : ipServidor;
        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;

        // Passamos o IP para o servidor
        this.servidorSaida = new ServidorSaida(this.ipServidor, portaServidor, this);
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
        LogClienteDashboard.enviar(
                TipoLog.SISTEMA,
                String.format("Saída iniciada em %s:%d (Dashboard: %s:%d)",
                        ipServidor, portaServidor, ipDashboard, portaDashboard)
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

        LogClienteDashboard.enviar(TipoLog.VEICULO, String.format("Veículo %s (%s) saiu do sistema. Tempo total: %.2f s",
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
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Saída encerrada.");
    }
}