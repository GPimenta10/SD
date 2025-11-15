import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String CLASSPATH = buildClasspath();

    private static String buildClasspath() {
        
        String separator = System.getProperty("path.separator");
        String m2Repo = Paths.get(System.getProperty("user.home"), ".m2", "repository").toString();
        String gsonPath = Paths.get(m2Repo, "com", "google", "code", "gson", "gson", "2.10.1", "gson-2.10.1.jar").toString();
        return String.join(separator, "target/classes", gsonPath);
    }

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
            // CRUZAMENTO CR1
            // ======================================
            System.out.println("• Iniciando Cruzamento Cr1...");
            Process cr1Proc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Cruzamentos.CruzamentoMain",
                    "Cr1"
            ).inheritIO().start();
            processos.add(cr1Proc);
            Thread.sleep(1500);

            // ======================================
            // CRUZAMENTO CR2
            // ======================================
            System.out.println("• Iniciando Cruzamento Cr2...");
            Process cr2Proc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Cruzamentos.CruzamentoMain",
                    "Cr2"
            ).inheritIO().start();
            processos.add(cr2Proc);
            Thread.sleep(1500);

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
            // CRUZAMENTO CR4
            // ======================================
            System.out.println("• Iniciando Cruzamento Cr4...");
            Process cr4Proc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Cruzamentos.CruzamentoMain",
                    "Cr4"
            ).inheritIO().start();
            processos.add(cr4Proc);
            Thread.sleep(1500);

            // ======================================
            // CRUZAMENTO CR5
            // ======================================
            System.out.println("• Iniciando Cruzamento Cr5...");
            Process cr5Proc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Cruzamentos.CruzamentoMain",
                    "Cr5"
            ).inheritIO().start();
            processos.add(cr5Proc);
            Thread.sleep(1500);

            // ======================================
            // 4. GERADORES via config (apenas E3 para este cenário) "PontosEntrada.PontosEntradasMain", "--only=E1"
            // ======================================
            System.out.println("• Iniciando Geradores (apenas E3 via --only=E3)...");
            Process geradoresProc = new ProcessBuilder(
                "java", "-cp", CLASSPATH,
                "PontosEntrada.PontosEntradasMain", "--only=E3"
            ).inheritIO().start();
            processos.add(geradoresProc);

            System.out.println("\n=== SISTEMA EM EXECUÇÃO ===");
            System.out.println("Dashboard ativo, veículos a circular...");
            System.out.println("Pressiona CTRL+C para sair mais cedo.\n");

            Thread.sleep(60_000); // 30 segundos de teste

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
