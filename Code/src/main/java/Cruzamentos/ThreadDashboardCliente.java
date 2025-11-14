package Cruzamentos;

import Rede.Cliente;
import Rede.Mensagem;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Thread responsável por enviar periodicamente o estado do cruzamento ao Dashboard.
 */
public class ThreadDashboardCliente extends Thread {

    private final Cruzamento cruzamento;
    private final Cliente clienteDashboard;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    // Intervalo entre atualizações (em ms)
    private static final long INTERVALO_ENVIO_MS = 1000;

    /**
     * Construtor do cliente do Dashboard.
     *
     * @param ipDashboard IP do Dashboard
     * @param portaDashboard Porta TCP do Dashboard
     * @param cruzamento Referência ao cruzamento atual
     */
    public ThreadDashboardCliente(String ipDashboard, int portaDashboard, Cruzamento cruzamento) {
        super("DashboardCliente-" + cruzamento.getNomeCruzamento());
        this.cruzamento = cruzamento;
        this.clienteDashboard = new Cliente(ipDashboard, portaDashboard);
    }

    @Override
    public void run() {
        System.out.printf("[DashboardCliente %s] Iniciado. Enviando estatísticas para Dashboard...%n",
                cruzamento.getNomeCruzamento());

        while (ativo) {
            try {
                // Gera JSON com o estado atual do cruzamento
                String estatisticasJson = cruzamento.gerarEstatisticasJSON();

                // Cria a mensagem de tipo "ESTATISTICA"
                Map<String, Object> conteudo = new HashMap<>();
                conteudo.put("estado", gson.fromJson(estatisticasJson, Object.class));

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
                System.err.printf("[DashboardCliente %s] Erro ao enviar estatísticas: %s%n",
                        cruzamento.getNomeCruzamento(), e.getMessage());
            }
        }

        System.out.printf("[DashboardCliente %s] Encerrado.%n", cruzamento.getNomeCruzamento());
    }

    /**
     * Encerra o envio de dados para o Dashboard.
     */
    public void parar() {
        ativo = false;
        interrupt();
    }
}
