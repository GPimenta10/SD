package Cruzamentos;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;
import Rede.Mensagem;
import Rede.Cliente;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread responsável por enviar periodicamente o estado do cruzamento para o Dashboard.
 * O envio é contínuo enquanto o cruzamento estiver ativo.
 */
public class ClienteCruzamentoDashboard extends Thread {
    private static final long INTERVALO_KEEPALIVE_MS = 2000;

    private final Cruzamento cruzamento;
    private final Cliente clienteDashboard;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    // Frequência de envio das estatísticas ao Dashboard
    private static final long INTERVALO_ENVIO_MS = 1000;

    /**
     * Construtor do cliente do Dashboard.
     *
     * @param ipDashboard     IP do Dashboard
     * @param portaDashboard  Porta TCP do Dashboard
     * @param cruzamento      Referência ao cruzamento associado a este cliente
     */
    public ClienteCruzamentoDashboard(String ipDashboard, int portaDashboard, Cruzamento cruzamento) {
        super("DashboardCliente-" + cruzamento.getNomeCruzamento());
        this.cruzamento = cruzamento;
        this.clienteDashboard = new Cliente(ipDashboard, portaDashboard);
    }

    /**
     * Execução base da thread
     */
    @Override
    public void run() {
        try {
            Thread.sleep(INTERVALO_KEEPALIVE_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (ativo) {
            try {
                // Obtém o estado detalhado do cruzamento
                Map<String, Object> estadoCruzamento = cruzamento.gerarEstatisticas();

                // Prepara mensagem JSON
                Map<String, Object> conteudo = new HashMap<>();
                conteudo.put("estado", estadoCruzamento);

                Mensagem mensagem = new Mensagem(
                        "ESTATISTICA",
                        cruzamento.getNomeCruzamento(),
                        "Dashboard",
                        conteudo
                );

                clienteDashboard.enviarMensagem(mensagem);

                Thread.sleep(INTERVALO_ENVIO_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                LogClienteDashboard.enviar(TipoLog.ERRO,"[" + cruzamento.getNomeCruzamento() + "] Erro ao enviar estatísticas: " + e.getMessage());
            }
        }
    }

    /**
     * Encerra o envio periódico de dados ao Dashboard.
     */
    public void parar() {
        ativo = false;
        interrupt();
    }
}

