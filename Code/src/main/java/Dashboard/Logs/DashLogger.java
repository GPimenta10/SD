package Dashboard.Logs;

import Dashboard.Paineis.PainelLogs;
import Logging.LogSender;

import javax.swing.SwingUtilities;

/**
 * Logger central do Dashboard.
 *
 * Responsabilidades:
 *  - Se estiver a correr dentro do Dashboard, escreve diretamente no PainelLogs.
 *  - Se for chamado por outro processo, delega o envio ao LogSender.
 *
 * Com esta refatorização, toda a lógica de envio por socket TCP foi removida
 * desta classe, evitando duplicação com LogClienteDashboard.
 */
public class DashLogger {

    private static PainelLogs painelLogs = null;
    private static String nomeProcesso = "Desconhecido";

    /**
     * Inicializado pelo próprio Dashboard no arranque.
     */
    public static void inicializar(PainelLogs painel) {
        painelLogs = painel;
        nomeProcesso = "Dashboard";
    }

    /**
     * Definir nome do processo remoto que enviará logs.
     */
    public static void definirNomeProcesso(String nome) {
        nomeProcesso = nome;
    }

    /**
     * Log universal:
     *  - Se estiver no Dashboard, escreve no painel.
     *  - Caso contrário, delega o envio para o LogSender.
     */
    public static void log(TipoLog tipo, String msg) {

        // Caso esteja no Dashboard → mostra na UI
        if (painelLogs != null) {
            System.out.println("[" + tipo + "] " + msg);

            SwingUtilities.invokeLater(() -> painelLogs.adicionarLog(tipo, msg));
            return;
        }

        // Caso contrário → enviar para Dashboard via LogSender
        LogSender.enviar(
                "LOG",
                nomeProcesso,
                tipo.name(),
                msg
        );
    }
}

