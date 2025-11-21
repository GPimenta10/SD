package Dashboard;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import com.google.gson.JsonObject;

import Dashboard.Estatisticas.GestorEstatisticas;
import Utils.ConfigLoader;
import Utils.ProcessCleaner;

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
            
            // ============================================================
            // NOVO: Configurar encerramento total ao fechar a janela
            // ============================================================
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent e) {
                    System.out.println("[Dashboard] A encerrar todo o sistema...");
                    
                    // Encerrar servidor do Dashboard
                    servidor.onEncerramento();
                    
                    // Terminar todos os processos nas portas do sistema
                    ProcessCleaner.terminarTodosProcessosSistema();
                    
                    // Fechar a janela
                    frame.dispose();
                    
                    // Terminar a JVM (mata o processo do IDE também)
                    System.exit(0);
                }
            });
            // ============================================================
            
            frame.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("[DashboardMain] Encerrando Dashboard...");
                servidor.onEncerramento();
            }));
        });
    }
}