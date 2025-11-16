package Saida;

import com.google.gson.Gson;
import Veiculo.Veiculo;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread responsável por enviar periodicamente as estatísticas
 * da saída (veículos removidos do sistema) ao Dashboard.
 * Também envia mensagens imediatas sempre que um novo veículo sai.
 */
public class SaidaComunicDash extends Thread {

    private final String ipDashboard;
    private final int portaDashboard;
    private final Saida saida;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    public SaidaComunicDash(String ipDashboard, int portaDashboard, Saida saida) {
        super("SaidaComunicDash");
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;
        this.saida = saida;
        setDaemon(true);
    }

    @Override
    public void run() {
        System.out.printf("[SaidaComunicDash] Iniciado. Enviando dados para %s:%d...%n",
                ipDashboard, portaDashboard);

        while (ativo) {
            try {
                enviarEstatisticas(); // envia resumo periódico
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("[SaidaComunicDash] Encerrado.");
    }

    /**
     * Envia o resumo periódico com o total de veículos saídos.
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
        System.out.printf("[SaidaComunicDash] Estatísticas enviadas (%d veículos).%n", veiculosSaidos.size());
    }


    /**
     * Envia uma mensagem imediata ao dashboard quando um veículo sai.
     *
     * @param veiculo veículo que saiu
     * @param tempoTotal tempo total no sistema (em segundos)
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

        System.out.printf(
                "[SaidaComunicDash] Enviado veículo %s (%s) ao Dashboard. Tempo total: %.2f s%n",
                veiculo.getId(), veiculo.getTipo(), tempoTotal
        );
    }

    /**
     * Envia qualquer mapa convertido em JSON ao dashboard.
     */
    private void enviarJson(Map<String, Object> dados) {
        String json = gson.toJson(dados);
        try (Socket socket = new Socket(ipDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
             out.println(json);
        } catch (Exception e) {
            System.err.println("[SaidaComunicDash] Falha ao enviar dados: " + e.getMessage());
        }
    }

    /** Encerra a thread de comunicação de forma segura. */
    public void parar() {
        ativo = false;
        this.interrupt();
    }
}
