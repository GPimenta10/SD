package Logging;

import Utils.ConfigLoader;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Classe de baixo nível responsável por enviar logs para o Dashboard via TCP.
 *
 * Esta classe contém toda a lógica comum de serialização e envio, permitindo
 * que tanto LogClienteDashboard como DashLogger a utilizem sem duplicação.
 *
 * Responsabilidades:
 *   Construir o JSON do log
 *   Abrir socket e enviar para o Dashboard
 *   Lidar com erros de forma segura sem interromper o programa
 */
public class LogSender {

    private static final Gson gson = new Gson();

    private static String host = null;
    private static int port = -1;

    private LogSender() {}

    /**
     * Carrega configuração do Dashboard (lazy loading)
     */
    private static void carregarConfiguracao() {
        if (host == null) {
            try {
                JsonObject config = ConfigLoader.carregarDashboard();
                host = config.get("ipServidor").getAsString();
                port = config.get("portaServidor").getAsInt();
            } catch (Exception e) {
                // Fallback para valores padrão
                host = "localhost";
                port = 6000;
            }
        }
    }

    /**
     * Envia um log genérico para o Dashboard através de socket TCP.
     *
     * @param tipo       Tipo da mensagem (geralmente "LOG")
     * @param origem     Nome do processo remetente
     * @param nivel      Nível do log (INFO, ERRO, etc.)
     * @param mensagem   Conteúdo textual do log
     */
    public static void enviar(String tipo, String origem, String nivel, String mensagem) {
        carregarConfiguracao();

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> json = new HashMap<>();
            json.put("tipo", tipo);
            json.put("processo", origem);
            json.put("origem", origem);
            json.put("nivel", nivel);
            json.put("mensagem", mensagem);

            out.println(gson.toJson(json));

        } catch (Exception e) {
            System.err.println("[LogSender] Erro ao enviar log: " + e.getMessage());
        }
    }
}