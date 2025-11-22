package Utils;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;

import javax.swing.UIManager;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;

/**
 * Classe responsável pela configuração do tema visual da aplicação.
 */
public class ConfigTheme {

    private static final int BORDER_RADIUS = 10;

    /**
     * Configura o tema FlatLaf OneDark.
     */
    public static void configurar() {
        try {
            FlatOneDarkIJTheme.setup();
            UIManager.put("Button.arc", BORDER_RADIUS);
            UIManager.put("Component.arc", BORDER_RADIUS);
        } catch (Exception e) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Não foi possível aplicar tema: " + e.getMessage());
        }
    }
}