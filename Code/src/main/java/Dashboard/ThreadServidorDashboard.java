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
 *
 * ATUALIZADO: Processa mensagens de movimento de veículos para animação no mapa
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
                case "VEICULO_MOVIMENTO" -> processarVeiculoMovimento(obj); // NOVO
                case "ESTATISTICA" -> processarEstatisticaCruzamento(obj); // OPCIONAL
                default -> System.out.println("[DashboardServidor] Tipo desconhecido: " + tipo);
            }
        } catch (Exception e) {
            System.err.println("[DashboardServidor] Erro JSON: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Processa mensagem de veículo que saiu do sistema
     */
    private void processarVeiculoSaida(JsonObject obj) {
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        String id = conteudo.get("id").getAsString();
        String tipo = conteudo.get("tipoVeiculo").getAsString();
        String entrada = conteudo.get("entrada").getAsString();
        JsonArray caminho = conteudo.getAsJsonArray("caminho");
        double tempoTotal = conteudo.get("tempoTotal").getAsDouble();

        SwingUtilities.invokeLater(() -> {
            dashboardFrame.getPainelVeiculos().adicionarVeiculoSaiu(id, tipo, entrada, caminho, tempoTotal);
            dashboardFrame.getPainelEstatisticas().incrementarSaidas();
        });

        System.out.printf("[DashboardServidor] Veículo saiu: %s (%s) - %.2fs%n", id, tipo, tempoTotal);
    }

    /**
     * Processa mensagem de veículo gerado
     */
    private void processarVeiculoGerado(JsonObject obj) {
        String entrada = obj.get("origem").getAsString();
        SwingUtilities.invokeLater(() ->
                dashboardFrame.getPainelEstatisticas().incrementarGerado(entrada)
        );
        System.out.printf("[DashboardServidor] Veículo gerado em %s%n", entrada);
    }

    /**
     * NOVO: Processa mensagem de movimento de veículo (para animação no mapa)
     */
    private void processarVeiculoMovimento(JsonObject obj) {
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        String id = conteudo.get("id").getAsString();
        String tipo = conteudo.get("tipo").getAsString();
        String origem = conteudo.get("origem").getAsString();
        String destino = conteudo.get("destino").getAsString();

        SwingUtilities.invokeLater(() ->
                dashboardFrame.getPainelMapa().adicionarVeiculo(id, tipo, origem, destino)
        );

        System.out.printf("[DashboardServidor] Movimento: %s (%s) de %s → %s%n",
                id, tipo, origem, destino);
    }

    /**
     * Processa estatísticas de cruzamentos (estado dos semáforos)
     */
    private void processarEstatisticaCruzamento(JsonObject obj) {
        String cruzamento = obj.get("origem").getAsString();
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        if (conteudo != null && conteudo.has("estado")) {
            JsonObject estado = conteudo.getAsJsonObject("estado");

            if (estado.has("semaforos")) {
                JsonArray semaforos = estado.getAsJsonArray("semaforos");

                for (int i = 0; i < semaforos.size(); i++) {
                    JsonObject semaforo = semaforos.get(i).getAsJsonObject();

                    String nome = semaforo.get("nome").getAsString();
                    String estadoSem = semaforo.get("estado").getAsString();
                    boolean verde = "VERDE".equals(estadoSem);

                    // Extrai origem e destino do nome do semáforo
                    // Formato esperado: "Semaforo_E3->Cr3->S"
                    String[] partes = nome.split("->");
                    if (partes.length >= 3) {
                        String origem = partes[0].replace("Semaforo_", "");
                        String destino = partes[2];

                        SwingUtilities.invokeLater(() ->
                                dashboardFrame.getPainelMapa().atualizarSemaforo(cruzamento, origem, destino, verde)
                        );
                    }
                }
            }
        }

        System.out.printf("[DashboardServidor] Estatísticas de %s processadas%n", cruzamento);
    }

    public void parar() {
        ativo = false;
        interrupt();
        System.out.println("[DashboardServidor] Encerrado.");
    }
}