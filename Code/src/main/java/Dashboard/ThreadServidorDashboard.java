package Dashboard;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor que recebe mensagens TCP dos cruzamentos e da saída.
 * Atualiza a interface gráfica em tempo real.
 */
public class ThreadServidorDashboard extends Thread {

    private final int porta;
    private final DashboardFrame dashboardFrame;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    public ThreadServidorDashboard(int porta, DashboardFrame dashboardFrame) {
        this.porta = porta;
        this.dashboardFrame = dashboardFrame;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            System.out.println("[DashboardServidor] A escutar na porta " + porta + "...");

            while (ativo) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarCliente(socket)).start();
            }
        } catch (Exception e) {
            if (ativo) System.err.println("[DashboardServidor] Erro: " + e.getMessage());
        }
    }

    private void processarCliente(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = in.readLine()) != null) {
                processarMensagem(linha);
            }
        } catch (Exception e) {
            System.err.println("[DashboardServidor] Erro no cliente: " + e.getMessage());
        }
    }

    private void processarMensagem(String json) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            String tipo = obj.get("tipo").getAsString();

            switch (tipo) {
                case "VEICULO_SAIU" -> processarVeiculoSaida(obj);
                case "VEICULO_GERADO" -> processarVeiculoGerado(obj);
                default -> System.out.println("[DashboardServidor] Tipo desconhecido: " + tipo);
            }
        } catch (Exception e) {
            System.err.println("[DashboardServidor] Erro JSON: " + e.getMessage());
        }
    }

    private void processarVeiculoSaida(JsonObject obj) {
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        String id = conteudo.get("id").getAsString();
        String tipo = conteudo.get("tipoVeiculo").getAsString();
        String entrada = conteudo.get("entrada").getAsString();
        JsonArray caminho = conteudo.getAsJsonArray("caminho");
        double tempoTotal = conteudo.get("tempoTotal").getAsDouble();

        dashboardFrame.getPainelVeiculos().adicionarVeiculoSaiu(id, tipo, entrada, caminho, tempoTotal);
        dashboardFrame.getPainelEstatisticas().incrementarSaidas();
    }


    private void processarVeiculoGerado(JsonObject obj) {
        String entrada = obj.get("origem").getAsString();
        SwingUtilities.invokeLater(() -> dashboardFrame.getPainelEstatisticas().incrementarGerado(entrada));
    }

    public void parar() {
        ativo = false;
        interrupt();
        System.out.println("[DashboardServidor] Encerrado.");
    }
}
