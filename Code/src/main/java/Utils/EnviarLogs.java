package Utils;

import com.google.gson.Gson;
import Dashboard.TipoLog;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

public class EnviarLogs {

    private static final Gson gson = new Gson();
    private static String nomeProcesso = "Desconhecido"; // default

    /**
     * Define o nome do processo uma vez no arranque.
     */
    public static void definirNomeProcesso(String nome) {
        nomeProcesso = nome;
    }

    /**
     * Envia um log ao Dashboard usando o nome do processo já definido.
     */
    public static void enviar(TipoLog nivel, String mensagem) {

        try (Socket socket = new Socket("localhost", 6000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> log = new HashMap<>();
            log.put("tipo", "LOG");
            log.put("processo", nomeProcesso);  // <- automaticamente preenchido
            log.put("nivel", nivel.name());
            log.put("mensagem", mensagem);

            out.println(gson.toJson(log));

        } catch (Exception e) {
            System.err.println("[EnviarLogs] ERRO ao enviar log: " + e.getMessage());
        }
    }
}
