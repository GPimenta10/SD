package Dashboard;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;

import javax.swing.*;

public class DashboardMain {

    public static void main(String[] args) {

        System.setProperty("flatlaf.useWindowDecorations", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");

        FlatLaf.registerCustomDefaultsSource("Dashboard.Themes");
        FlatOneDarkIJTheme.setup();

        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame();
            ServidorDashboard servidor = new ServidorDashboard(6000, frame);
            servidor.start();

            frame.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[DashboardMain] Encerrando Dashboard...");
                servidor.parar();
            }));
        });
    }
}
