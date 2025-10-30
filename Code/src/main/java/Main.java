import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) throws Exception {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║  SISTEMA DE TRÁFEGO URBANO - SD    ║");
        System.out.println("╚════════════════════════════════════╝\n");

        long inicio = System.currentTimeMillis();

        String classpath = System.getProperty("java.class.path");
        String javaHome = System.getProperty("java.home");
        String javaBin = javaHome + File.separator + "bin" + File.separator + "java";

        System.out.println("📂 Classpath: " + classpath);
        System.out.println("☕ Java: " + javaBin + "\n");

        int totalVeiculos = 50;
        int intervalo = 500;

        // 1️⃣ Dashboard
        System.out.println("Lançando Dashboard...");
        Process pDash = new ProcessBuilder(javaBin, "-cp", classpath, "Dashboard.Dashboard", "9000")
                .inheritIO().start();
        aguardar(1200);

        // 2️⃣ Saída
        System.out.println("Lançando Saída...");
        ProcessBuilder pbSaida = new ProcessBuilder(javaBin, "-cp", classpath,
                "OutrasClasses.Saida", "7000", "127.0.0.1", "9000");
        pbSaida.redirectErrorStream(true);
        Process pSaida = pbSaida.start();
        BufferedReader saidaReader = new BufferedReader(new InputStreamReader(pSaida.getInputStream()));
        aguardar(1000);

        // 3️⃣ Cruzamento3
        System.out.println("🚀 Lançando Cruzamento3...");
        Process pCr3 = new ProcessBuilder(javaBin, "-cp", classpath, "Cruzamento.Cruzamento3")
                .inheritIO().start();
        aguardar(1500);

        // 4️⃣ Controlador de Entradas (gera E1, E2 e E3 dentro dele)
        System.out.printf("🚀 Lançando ControladorEntradas... (%d veículos totais, %d ms intervalo)%n",
                totalVeiculos, intervalo);

        Process pEntradas = new ProcessBuilder(javaBin, "-cp", classpath,
                "EntradaVeiculos.ControladorEntradas", "127.0.0.1",
                String.valueOf(totalVeiculos), String.valueOf(intervalo))
                .inheritIO().start();

        System.out.println("\n✅ Todos os processos lançados!");
        System.out.println("Pressione CTRL+C para terminar manualmente...\n");

        // Aguarda entradas terminarem
        pEntradas.waitFor();
        System.out.println("\nTodas as entradas concluíram o envio de veículos.");
        System.out.println("Aguardando que todos cheguem à saída...");

        // 🔄 Monitoriza a saída até todos os veículos chegarem
        boolean todosChegaram = false;
        String linha;
        while ((linha = saidaReader.readLine()) != null) {
            if (linha.contains("FIM_SISTEMA")) {
                todosChegaram = true;
                System.out.println("🏁 Todos os veículos realmente saíram do sistema!");
                break;
            }
        }

        if (!todosChegaram) {
            System.out.println("⚠️ Timeout: nem todos os veículos chegaram à saída (possível bloqueio).");
        }

        aguardar(2000);

        // 🛑 Encerrar processos (excepto Dashboard)
        System.out.println("🛑 Encerrando sistema...");
        encerrarProcesso(pCr3, "Cruzamento3");
        encerrarProcesso(pSaida, "Saída");

        System.out.println("✅ Dashboard permanece aberto para visualização dos resultados.");
        System.out.println("Feche a janela do Dashboard manualmente quando quiser.\n");

        long duracao = System.currentTimeMillis() - inicio;
        System.out.printf("🕒 Simulação completa em %.2f segundos.%n", duracao / 1000.0);
    }

    private static void aguardar(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private static void encerrarProcesso(Process p, String nome) {
        try {
            if (p != null && p.isAlive()) {
                System.out.println("Encerrando " + nome + "...");
                p.destroy();
                if (!p.waitFor(2000, TimeUnit.MILLISECONDS)) {
                    p.destroyForcibly();
                    System.out.println("⚠️ " + nome + " forçado a terminar.");
                }
            }
        } catch (Exception e) {
            System.err.println("Erro ao encerrar " + nome + ": " + e.getMessage());
        }
    }
}
