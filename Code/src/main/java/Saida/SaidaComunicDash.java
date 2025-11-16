package Saida;

import Dashboard.DashLogger;
import Dashboard.TipoLog;
import Utils.EnviarLogs;
import Veiculo.Veiculo;

import com.google.gson.Gson;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Thread responsável por enviar periodicamente as estatísticas
 * da saída (veículos removidos do sistema) ao Dashboard.
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
       EnviarLogs.enviar(TipoLog.SISTEMA, "Saída: comunicação com Dashboard iniciada em " + ipDashboard + ":" + portaDashboard);

        while (ativo) {
            try {
                enviarEstatisticas();
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

       EnviarLogs.enviar(TipoLog.SISTEMA, "Saída: comunicação com Dashboard encerrada.");
    }

    /**
     * Envia o resumo periódico com o total de veículos saídos.
     * (Este evento é silencioso — não é enviado para o DashLogger)
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

        // Debug opcional (comentado):
        // System.out.printf("[SaidaComunicDash] Estatísticas enviadas (%d veículos).%n", veiculosSaidos.size());
    }


    /**
     * Envia uma mensagem imediata ao Dashboard quando um veículo sai.
     * (Este evento deve ser visível visivelmente no PainelLogs)
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

       EnviarLogs.enviar(TipoLog.VEICULO, "Veículo " + veiculo.getId() + " (" + veiculo.getTipo() +") saiu do sistema. Tempo total: "
                + String.format("%.2f", tempoTotal) + " s");
    }


    /**
     * Envia qualquer mapa convertido em JSON ao Dashboard.
     */
    private void enviarJson(Map<String, Object> dados) {
        String json = gson.toJson(dados);

        try (Socket socket = new Socket(ipDashboard, portaDashboard);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(json);
        } catch (Exception e) {
           EnviarLogs.enviar(TipoLog.AVISO, "Saída: falha ao enviar dados ao Dashboard (" + e.getMessage() + ")");
        }
    }

    /** Encerra a thread de comunicação de forma segura. */
    public void parar() {
        ativo = false;
        this.interrupt();
    }
}
