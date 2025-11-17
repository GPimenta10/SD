import Dashboard.Logs.TipoLog;
import Utils.EnviarLogs;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Main {

    private static final String CLASSPATH = buildClasspath();

    private static String buildClasspath() {
        String separator = System.getProperty("path.separator");
        String m2Repo = Paths.get(System.getProperty("user.home"), ".m2", "repository").toString();

        // Caminhos das dependências
        String gsonPath = Paths.get(m2Repo, "com", "google", "code", "gson", "gson", "2.10.1", "gson-2.10.1.jar").toString();
        String flatLafPath = Paths.get(m2Repo, "com", "formdev", "flatlaf", "3.6.2", "flatlaf-3.6.2.jar").toString();
        String flatLafThemesPath = Paths.get(m2Repo, "com", "formdev", "flatlaf-intellij-themes", "3.4", "flatlaf-intellij-themes-3.4.jar").toString();

        return String.join(separator,
                "target/classes",
                gsonPath,
                flatLafPath,
                flatLafThemesPath
        );
    }

    public static void main(String[] args) {

        List<Process> processos = new ArrayList<>();
        Process dashboardProc = null;

        try {
            // 1. DASHBOARD
            System.out.println("A iniciar o Dashboard");
            dashboardProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Dashboard.DashboardMain").inheritIO().start();
            processos.add(dashboardProc);
            Thread.sleep(2500);

            EnviarLogs.enviar(TipoLog.SUCESSO, "Dashboard iniciado com sucesso.");

            // 2. SAÍDA S
            EnviarLogs.enviar(TipoLog.SISTEMA, "A iniciar Saída");
            Process saidaProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Saida.SaidaMain").inheritIO().start();
            processos.add(saidaProc);
            Thread.sleep(1000);
            EnviarLogs.enviar(TipoLog.SUCESSO, "Saída S ativa.");

            // 3. CRUZAMENTOS (Cr1, Cr2, Cr3, Cr4, Cr5)
            String[] cruzamentos = {"Cr1", "Cr2", "Cr3", "Cr4", "Cr5"};

            for (String cruzamento : cruzamentos) {
                EnviarLogs.enviar(TipoLog.SISTEMA, "A iniciar Cruzamento " + cruzamento + "...");
                Process cruzProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Cruzamentos.CruzamentoMain", cruzamento).inheritIO().start();
                processos.add(cruzProc);
                Thread.sleep(1500);
                EnviarLogs.enviar(TipoLog.SUCESSO, "Cruzamento " + cruzamento + " ativo.");
            }

            // 4. GERADORES (apenas E3)
            EnviarLogs.enviar(TipoLog.SISTEMA, "A iniciar geradores de veículos");
            Process geradoresProc = new ProcessBuilder("java", "-cp", CLASSPATH, "PontosEntrada.PontosEntradasMain", "--only=E1,E2,E3").inheritIO().start();
            processos.add(geradoresProc);

            EnviarLogs.enviar(TipoLog.SUCESSO, "Geradores iniciados.");
            EnviarLogs.enviar(TipoLog.SISTEMA, "Sistema em execução");

            // Aguarda que o gerador termine
            EnviarLogs.enviar(TipoLog.AVISO, "Aguardando término dos geradores");
            geradoresProc.waitFor();
            EnviarLogs.enviar(TipoLog.SUCESSO, "Geradores concluíram as suas operações.");

            // Aguarda tempo extra para veículos terminarem visualmente (2 minutos)
            EnviarLogs.enviar(TipoLog.AVISO, "Aguardando veículos terminarem de circular (120s)...");
            Thread.sleep(120_000);

            EnviarLogs.enviar(TipoLog.SISTEMA, "Processos backend concluídos.");

            encerrarProcessosBackend(processos, dashboardProc);

            EnviarLogs.enviar(TipoLog.AVISO, "Dashboard continuará aberto até ser fechado manualmente.");

            dashboardProc.waitFor();
            EnviarLogs.enviar(TipoLog.SISTEMA, "Dashboard encerrado pelo utilizador.");
        } catch (Exception e) {
            EnviarLogs.enviar(TipoLog.ERRO, "Erro no Main: " + e.getMessage());
        } finally {
            encerrarProcessos(processos);
        }
    }

    /**
     * Encerra apenas processos backend (mantém Dashboard)
     */
    private static void encerrarProcessosBackend(List<Process> todos, Process dashboard) {
        EnviarLogs.enviar(TipoLog.AVISO, "Encerrando processos backend");

        for (Process p : todos) {
            if (p != dashboard && p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
        }
        EnviarLogs.enviar(TipoLog.SUCESSO, "Backend encerrado.");
    }

    /**
     * Encerra todos os processos (incluindo Dashboard)
     */
    private static void encerrarProcessos(List<Process> processos) {
        EnviarLogs.enviar(TipoLog.SISTEMA, "A encerrar todos os processos.");

        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
        }
        EnviarLogs.enviar(TipoLog.SISTEMA, "Todos os processos encerrados.");
    }
}