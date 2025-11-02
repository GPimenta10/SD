package Dashboard;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Responsável por enviar mensagens ao Dashboard.
 */
public class ComunicadorDashboard {
    private static final String HOST = "localhost";
    private static final int PORTA = 9000;

    private static final ComunicadorDashboard INSTANCE = new ComunicadorDashboard(HOST, PORTA);

    private final String host;
    private final int porta;

    private ComunicadorDashboard(String host, int porta) {
        this.host = host;
        this.porta = porta;
    }

    public static ComunicadorDashboard getInstance() {
        return INSTANCE;
    }

    public void enviar(String mensagem) {
        try (Socket socket = new Socket(host, porta);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(mensagem);
        } catch (IOException e) {
            System.err.printf("[Dashboard] Falha ao enviar (%s): %s%n", mensagem, e.getMessage());
        }
    }
}

