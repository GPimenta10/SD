package Saida;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;
import Veiculo.Veiculo;

import com.google.gson.Gson;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cliente responsável pela comunicação entre a Saída e o Dashboard.
 *
 * Esta thread executa duas funções principais:
 *  Envia estatísticas periódicas (a cada 5 segundos) sobre veículos que saíram
 *  Envia notificações imediatas quando um veículo sai do sistema
 *
 * A comunicação é feita através de mensagens JSON enviadas via socket TCP.
 */
public class ClienteSaidaDash extends Thread {

    private static final int INTERVALO_ESTATISTICAS_MS = 5000;

    private final String ipDashboard;
    private final int portaDashboard;
    private final Saida saida;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    /**
     * Construtor da classe
     *
     * @param ipDashboard Endereço IP do Dashboard
     * @param portaDashboard Porta TCP do Dashboard
     * @param saida Instância da Saída para aceder aos dados
     * @throws IllegalArgumentException se os parâmetros forem inválidos
     */
    public ClienteSaidaDash(String ipDashboard, int portaDashboard, Saida saida) {
        super("ClienteSaidaDash");

        if (ipDashboard == null || ipDashboard.trim().isEmpty()) {
            throw new IllegalArgumentException("IP do Dashboard não pode ser null ou vazio");
        }
        if (portaDashboard < 1 || portaDashboard > 65535) {
            throw new IllegalArgumentException("Porta do Dashboard inválida");
        }
        if (saida == null) {
            throw new IllegalArgumentException("Instância de Saida não pode ser null");
        }

        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;
        this.saida = saida;
        setDaemon(true);
    }

    /**
     * Loop principal da thread.
     *
     * Envia estatísticas agregadas ao Dashboard periodicamente enquanto
     * a thread estiver ativa. O intervalo entre envios é definido por
     * INTERVALO_ESTATISTICAS_MS (5 segundos).
     */
    @Override
    public void run() {
        LogClienteDashboard.enviar(
                TipoLog.SISTEMA,
                String.format("Saída: comunicação com Dashboard iniciada em %s:%d", ipDashboard, portaDashboard)
        );

        while (ativo) {
            try {
                enviarEstatisticas();
                Thread.sleep(INTERVALO_ESTATISTICAS_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Saída: comunicação com Dashboard encerrada.");
    }

    /**
     * Envia estatísticas agregadas sobre veículos que saíram do sistema.
     *
     * Constrói e envia uma mensagem JSON com:
     *  Total de saídas registadas
     *  Lista completa de veículos que saíram
     *
     */
    private void enviarEstatisticas() {
        List<Veiculo> veiculosSaidos = saida.getVeiculosSaidos();

        Map<String, Object> conteudo = new HashMap<>();
        conteudo.put("totalSaidas", veiculosSaidos.size());
        conteudo.put("veiculos", veiculosSaidos);

        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("tipo", "ESTATISTICA_SAIDA");
        mensagem.put("origem", "Saida");
        mensagem.put("destino", "Dashboard");
        mensagem.put("conteudo", conteudo);

        enviarJson(mensagem);
    }

    /**
     * Envia notificação imediata ao Dashboard quando um veículo sai do sistema.
     *
     * Este método é chamado pela classe Saida sempre que um veículo completa
     * o seu percurso. Envia informação detalhada sobre o veículo incluindo
     * tempo total no sistema.
     *
     * @param veiculo Veículo que saiu do sistema
     * @param tempoTotal Tempo total que o veículo permaneceu no sistema (segundos)
     */
    public void enviarVeiculoSaiu(Veiculo veiculo, double tempoTotal) {
        Map<String, Object> conteudo = new HashMap<>();
        conteudo.put("id", veiculo.getId());
        conteudo.put("tipoVeiculo", veiculo.getTipo().name());
        conteudo.put("entrada", veiculo.getPontoEntrada().name());
        conteudo.put("caminho", veiculo.getCaminho());
        conteudo.put("tempoTotal", tempoTotal);
        conteudo.put("totalSaidas", saida.getVeiculosSaidos().size());

        Map<String, Object> mensagem = new HashMap<>();
        mensagem.put("tipo", "VEICULO_SAIU");
        mensagem.put("origem", "Saida");
        mensagem.put("destino", "Dashboard");
        mensagem.put("conteudo", conteudo);

        enviarJson(mensagem);

        LogClienteDashboard.enviar( TipoLog.VEICULO, String.format("Veículo %s (%s) saiu do sistema. Tempo total: %.2f s", veiculo.getId(), veiculo.getTipo(), tempoTotal));
    }

    /**
     * Serializa e envia uma mensagem JSON ao Dashboard via socket TCP.
     *
     * Cria uma nova conexão para cada mensagem (stateless) e fecha-a
     * automaticamente após o envio. Em caso de falha na comunicação,
     * regista um aviso nos logs mas não interrompe a execução.
     *
     * @param dados Map com os dados a serializar e enviar
     */
    private void enviarJson(Map<String, Object> dados) {
        String json = gson.toJson(dados);

        try (Socket socket = new Socket(ipDashboard, portaDashboard); PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(json);
        } catch (Exception e) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Saída: falha ao enviar dados ao Dashboard (" + e.getMessage() + ")"
            );
        }
    }

    /**
     * Para a thread de comunicação com o Dashboard
     */
    public void parar() {
        ativo = false;
        this.interrupt();
    }
}