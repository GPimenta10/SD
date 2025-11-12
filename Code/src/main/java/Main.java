import java.io.IOException;

/**
 * Orquestrador da Simulação de Tráfego Urbano.
 * Inicia Dashboard, Saída, Cruzamentos e Geradores como processos independentes.
 *
 * Neste teste: E3 -> Cr3 -> Saída
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("TESTE: E3 -> Cr3 -> S (Processos Independentes)");
        System.out.println("=".repeat(60));

        try {
            // === 1. Iniciar o DASHBOARD ===
            System.out.println("[SISTEMA] A iniciar processo Dashboard...");
            Process dashboardProc = new ProcessBuilder(
                    "java", "-cp", "target/classes;C:\\Users\\Gabriel\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.10.1\\gson-2.10.1.jar",
                    "Dashboard.DashboardMain"
            ).inheritIO().start();

            Thread.sleep(2000); // pequeno delay para garantir que o Dashboard sobe primeiro

            // === 2. Iniciar o processo de SAÍDA ===
            System.out.println("[SISTEMA] A iniciar processo Saída...");
            Process saidaProc = new ProcessBuilder(
                    "java", "-cp", "target/classes;C:\\Users\\Gabriel\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.10.1\\gson-2.10.1.jar",
                    "Saida.SaidaMain"
            ).inheritIO().start();

            Thread.sleep(1000);

            // === 3. Iniciar o processo de CRUZAMENTO ===
            System.out.println("[SISTEMA] A iniciar processo Cruzamento...");
            Process cruzamentoProc = new ProcessBuilder(
                    "java", "-cp", "target/classes;C:\\Users\\Gabriel\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.10.1\\gson-2.10.1.jar",
                    "Cruzamentos.CruzamentoMain"
            ).inheritIO().start();

            Thread.sleep(1000);

            // === 4. Iniciar o processo de GERADOR DE VEÍCULOS ===
            System.out.println("[SISTEMA] A iniciar processo Gerador de Veículos...");
            Process geradorProc = new ProcessBuilder(
                    "java", "-cp", "target/classes;C:\\Users\\Gabriel\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.10.1\\gson-2.10.1.jar",
                    "PontosEntrada.ProcessMainE3"
            ).inheritIO().start();

            System.out.println("\n[SISTEMA] Todos os processos iniciados com sucesso!");
            System.out.println("[SISTEMA] Simulação em execução...\n");

            // Mantém o orquestrador ativo enquanto os outros correm
            Thread.sleep(30000);

            // === Encerrar tudo ===
            System.out.println("\n[SISTEMA] Parando todos os processos...");
            geradorProc.destroy();
            cruzamentoProc.destroy();
            saidaProc.destroy();
            dashboardProc.destroy();

            System.out.println("[SISTEMA] Simulação encerrada.");

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
