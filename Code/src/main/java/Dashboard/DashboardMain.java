package Dashboard;

import Dashboard.Estatisticas.GestorEstatisticas;
import Utils.ConfigLoader;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.formdev.flatlaf.FlatLaf;

import com.google.gson.JsonObject;

import javax.swing.SwingUtilities;

/**
 * 
 * 
 * 
 */
public class DashboardMain {
    public static void main(String[] args) {

        System.setProperty("flatlaf.useWindowDecorations", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");

        FlatLaf.registerCustomDefaultsSource("Dashboard.Themes");
        FlatOneDarkIJTheme.setup();

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {

            GestorEstatisticas gestor = new GestorEstatisticas();

            DashboardFrame frame = new DashboardFrame(gestor);

            JsonObject config = ConfigLoader.carregarDashboard();
            String ip = config.has("ipServidor") ? config.get("ipServidor").getAsString() : "localhost";
            int porta = config.get("portaServidor").getAsInt();

            ServidorDashboard servidor = new ServidorDashboard(ip, porta, frame, gestor);

            servidor.start();
            frame.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[DashboardMain] Encerrando Dashboard...");
                servidor.onEncerramento();
            }));
        });
    }
}
