import Logging.LogClienteDashboard;
import Dashboard.MenuInicial;
import Dashboard.Logs.TipoLog;
import Utils.ConfigTheme;
import Utils.ProcessManager;
import Utils.ProcessCleaner;

/**
 * Classe principal que orquestra o arranque do sistema de tráfego urbano.
 */
public class Main {

    public static void main(String[] args) {
        ConfigTheme.configurar();
        ProcessCleaner.limparProcessosAnteriores();

        String[] config = MenuInicial.obterConfiguracoes();
        ProcessManager gestor = new ProcessManager();

        try {
            gestor.iniciarTodos(config[0], config[1]);
            gestor.aguardarFinalizacao();
        } catch (Exception e) {
            LogClienteDashboard.enviar(TipoLog.ERRO, "Erro no Main: " + e.getMessage());
        } finally {
            gestor.encerrarTodos();
        }
    }
}