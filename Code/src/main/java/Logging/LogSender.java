package Logging;

import com.google.gson.Gson;
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

    private static final String HOST = "localhost";
    private static final int PORT = 6000;

    private LogSender() {}

    /**
     * Envia um log genérico para o Dashboard através de socket TCP.
     *
     * @param tipo       Tipo da mensagem (geralmente "LOG")
     * @param origem     Nome do processo remetente
     * @param nivel      Nível do log (INFO, ERRO, etc.)
     * @param mensagem   Conteúdo textual do log
     */
    public static void enviar(String tipo, String origem, String nivel, String mensagem) {
        try (Socket socket = new Socket(HOST, PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> json = new HashMap<>();
            json.put("tipo", tipo);
            json.put("origem", origem);
            json.put("nivel", nivel);
            json.put("mensagem", mensagem);

            out.println(gson.toJson(json));

        } catch (Exception e) {
            System.err.println("[LogSender] Erro ao enviar log: " + e.getMessage());
        }
    }
}
