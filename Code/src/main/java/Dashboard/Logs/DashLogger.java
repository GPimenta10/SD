package Dashboard.Logs;

import Dashboard.Paineis.PainelLogs;
import com.google.gson.Gson;

import javax.swing.SwingUtilities;
import java.io.PrintWriter;
import java.net.Socket;

public class DashLogger {

    private static PainelLogs painelLogs = null;
    private static final String IP_DASHBOARD = "localhost";
    private static final int PORTA_DASHBOARD = 6000;

    private static String nomeProcesso = "Desconhecido";

    private static final Gson gson = new Gson();

    /**
     * Apenas o Dashboard chama isto no arranque.
     */
    public static void inicializar(PainelLogs painel) {
        painelLogs = painel;
        nomeProcesso = "Dashboard";
    }

    /**
     * Cada processo (Cruzamento, Entrada, Saída) deve definir o próprio nome.
     */
    public static void definirNomeProcesso(String nome) {
        nomeProcesso = nome;
    }

    /**
     * Log universal:
     *  - Se estiver no Dashboard → escreve no painel
     *  - Se estiver noutro processo → envia JSON para o Dashboard
     */
    public static void log(TipoLog tipo, String msg) {

        // Caso 1 → Dashboard
        if (painelLogs != null) {
            System.out.println("[" + tipo + "] " + msg);

            SwingUtilities.invokeLater(() -> painelLogs.adicionarLog(tipo, msg));
            return;
        }

        // Caso 2 → Processo remoto: enviar para Dashboard
        try (Socket socket = new Socket(IP_DASHBOARD, PORTA_DASHBOARD);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            var json = new java.util.HashMap<String, Object>();
            json.put("tipo", "LOG");
            json.put("nivel", tipo.name());
            json.put("origem", nomeProcesso);
            json.put("mensagem", msg);

            out.println(gson.toJson(json));
        } catch (Exception e) {
            System.err.println("[FALHA ENVIO LOG] " + msg + " | Erro: " + e.getMessage());
        }
    }
}
