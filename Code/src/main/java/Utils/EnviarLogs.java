package Utils;

import com.google.gson.Gson;
import Dashboard.Logs.TipoLog;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilitário para envio de logs para o Dashboard centralizado.
 *
 * Esta classe fornece funcionalidades para enviar mensagens de log
 * de diferentes processos para um servidor Dashboard através de socket TCP.
 * Os logs são serializados em JSON
 *
 */
public class EnviarLogs {

    private static final Gson gson = new Gson();
    private static final String DASHBOARD_HOST = "localhost";
    private static final int DASHBOARD_PORT = 6000;
    private static String nomeProcesso = "Desconhecido";

    /**
     * Define o nome do processo que está a enviar logs.
     * Este método deve ser chamado uma vez no arranque da aplicação
     * para identificar corretamente a origem dos logs no Dashboard.
     *
     * @param nome Nome identificador do processo
     * @throws IllegalArgumentException se o nome for null ou vazio
     */
    public static void definirNomeProcesso(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do processo não pode ser null ou vazio");
        }
        nomeProcesso = nome.trim();
    }

    /**
     * Envia uma mensagem de log para o Dashboard.
     *
     * A conexão é automaticamente fechada após o envio.
     * Em caso de erro na comunicação, imprime mensagem de erro no stderr mas não
     * interrompe a execução do programa.
     *
     * @param nivel Nível do log (INFO, AVISO, ERRO)
     * @param mensagem Texto descritivo do evento a registar
     * @throws IllegalArgumentException Se nivel ou mensagem forem null
     */
    public static void enviar(TipoLog nivel, String mensagem) {
        if (nivel == null) {
            throw new IllegalArgumentException("Nível do log não pode ser null");
        }
        if (mensagem == null) {
            throw new IllegalArgumentException("Mensagem do log não pode ser null");
        }

        try (Socket socket = new Socket(DASHBOARD_HOST, DASHBOARD_PORT);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> log = new HashMap<>();
            log.put("tipo", "LOG");
            log.put("processo", nomeProcesso);
            log.put("nivel", nivel.name());
            log.put("mensagem", mensagem);

            out.println(gson.toJson(log));
        } catch (Exception e) {
            System.err.println("[EnviarLogs] ERRO ao enviar log para Dashboard: " + e.getMessage());
        }
    }
}