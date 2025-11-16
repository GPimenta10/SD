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
        Process dashboardProc = null;

        try {
            System.out.println("\n=== INICIANDO TESTE COMPLETO: Dashboard + Cruzamentos + E3 ===\n");

            // ======================================
            // 1. DASHBOARD
            // ======================================
            System.out.println("• Iniciando Dashboard...");
            dashboardProc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "Dashboard.DashboardMain"
            ).inheritIO().start();
            processos.add(dashboardProc);
            Thread.sleep(2500);

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
            // 3. CRUZAMENTOS (Cr1, Cr2, Cr3, Cr4, Cr5)
            // ======================================
            String[] cruzamentos = {"Cr1", "Cr2", "Cr3", "Cr4", "Cr5"};

            for (String cruzamento : cruzamentos) {
                System.out.println("• Iniciando Cruzamento " + cruzamento + "...");
                Process cruzProc = new ProcessBuilder(
                        "java", "-cp", CLASSPATH,
                        "Cruzamentos.CruzamentoMain",
                        cruzamento
                ).inheritIO().start();
                processos.add(cruzProc);
                Thread.sleep(1500);
            }

            // ======================================
            // 4. GERADORES (apenas E3)
            // ======================================
            System.out.println("• Iniciando Geradores (apenas E3 via --only=E3)...");
            Process geradoresProc = new ProcessBuilder(
                    "java", "-cp", CLASSPATH,
                    "PontosEntrada.PontosEntradasMain", "--only=E3"
            ).inheritIO().start();
            processos.add(geradoresProc);

            System.out.println("\n=== SISTEMA EM EXECUÇÃO ===");
            System.out.println("Dashboard ativo, veículos a circular...");
            System.out.println("⚠️  O sistema NÃO fecha automaticamente!");
            System.out.println("📊 Analise os resultados no Dashboard.\n");

            // ✅ Aguarda que o gerador termine
            System.out.println("⏳ Aguardando geradores terminarem...");
            geradoresProc.waitFor();
            System.out.println("✓ Geradores terminaram de gerar veículos.\n");

            // ✅ Aguarda tempo extra para veículos terminarem visualmente (2 minutos)
            System.out.println("⏳ Aguardando veículos terminarem de circular (120 segundos)...");
            Thread.sleep(120_000);

            System.out.println("\n=== PROCESSOS BACKEND TERMINARAM ===");
            System.out.println("📊 Dashboard permanece aberto para análise.");
            System.out.println("❌ Feche o Dashboard manualmente quando terminar.\n");

            // ✅ Encerra apenas processos backend (não o Dashboard)
            encerrarProcessosBackend(processos, dashboardProc);

            // ✅ Mantém main thread viva (Dashboard continua aberto)
            System.out.println("⌛ Aguardando fechamento manual do Dashboard...\n");
            dashboardProc.waitFor();

            System.out.println("✓ Dashboard encerrado pelo usuário.");

        } catch (Exception e) {
            System.err.println("\nERRO: " + e.getMessage());
            e.printStackTrace();
        } finally {
            encerrarProcessos(processos);
        }
    }

    /**
     * ✅ NOVO: Encerra apenas processos backend (mantém Dashboard)
     */
    private static void encerrarProcessosBackend(List<Process> todos, Process dashboard) {
        System.out.println("Encerrando processos backend...");

        for (Process p : todos) {
            if (p != dashboard && p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {}
            }
        }

        System.out.println("✓ Processos backend encerrados.\n");
    }

    /**
     * Encerra todos os processos (incluindo Dashboard)
     */
    private static void encerrarProcessos(List<Process> processos) {
        System.out.println("Encerrando todos os processos restantes...");

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