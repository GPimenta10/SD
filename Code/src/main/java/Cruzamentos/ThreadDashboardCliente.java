package Cruzamentos;

import java.util.HashMap;
import java.util.Map;

import com.google.gson.Gson;

import Rede.Cliente;
import Rede.Mensagem;

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

        try {
            // Pequeno atraso inicial para dar tempo ao Dashboard de se inicializar
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        while (ativo) {
            try {
                // Obtém o mapa de estatísticas diretamente do cruzamento
                Map<String, Object> estadoCruzamento = cruzamento.gerarEstatisticas();

                // Cria a mensagem de tipo "ESTATISTICA"
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
