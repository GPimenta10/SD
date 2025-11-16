package Dashboard;


/**
 * Processo independente para o Dashboard.
 */
public class DashboardMain {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Dashboard");
        System.out.println("=".repeat(60));

        javax.swing.SwingUtilities.invokeLater(() -> {
            DashboardFrame frame = new DashboardFrame();
            ThreadServidorDashboard servidor = new ThreadServidorDashboard(6000, frame);
            servidor.start();
            frame.setVisible(true);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("\n[DashboardMain] Encerrando Dashboard...");
                servidor.parar();
            }));
        });
    }
}

