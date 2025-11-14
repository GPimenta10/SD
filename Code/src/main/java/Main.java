import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String CLASSPATH =
            "target/classes;C:\\Users\\Gabriel\\.m2\\repository\\com\\google\\code\\gson\\gson\\2.10.1\\gson-2.10.1.jar";

    public static void main(String[] args) {

        List<Process> processos = new ArrayList<>();

        try {
            System.out.println("\n=== INICIANDO TESTE COMPLETO: Dashboard + E3 → Cr3 → S ===\n");

            // ======================================
            // 1. DASHBOARD
            // ======================================
            System.out.println("• Iniciando Dashboard...");
            Process dashboardProc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Dashboard.DashboardMain"
            ).inheritIO().start();
            processos.add(dashboardProc);
            Thread.sleep(2500); // tempo para abrir a janela do Swing

            // ======================================
            // 2. SAÍDA S
            // ======================================
            System.out.println("• Iniciando Saída S...");
            Process saidaProc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Saida.SaidaMain"
            ).inheritIO().start();
            processos.add(saidaProc);
            Thread.sleep(1000);

            // ======================================
            // 3. CRUZAMENTO CR3
            // ======================================
            System.out.println("• Iniciando Cruzamento Cr3...");
            Process cr3Proc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Cruzamentos.CruzamentoMain",
                    "Cr3"
            ).inheritIO().start();
            processos.add(cr3Proc);
            Thread.sleep(1500);

            // ======================================
            // 4. GERADOR DE E3
            // ======================================
            System.out.println("• Iniciando Gerador E3...");
            Process geradorE3Proc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "PontosEntrada.ProcessMainE3"
            ).inheritIO().start();
            processos.add(geradorE3Proc);

            System.out.println("\n=== SISTEMA EM EXECUÇÃO ===");
            System.out.println("Dashboard ativo, veículos a circular...");
            System.out.println("Pressiona CTRL+C para sair mais cedo.\n");

            Thread.sleep(30_000); // 30 segundos de teste

        } catch (Exception e) {
            System.err.println("\nERRO: " + e.getMessage());
            e.printStackTrace();
        } finally {
            encerrarProcessos(processos);
        }
    }

    private static void encerrarProcessos(List<Process> processos) {
        System.out.println("\nEncerrando processos...");

        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {}
            }
        }

        System.out.println("✓ Todos os processos encerrados.\n");
    }
}
