package Main;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;
import Utils.ProcessCleaner;
import Utils.ClassPathUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * 
 */
public class Main {

    private static final String CLASSPATH = ClassPathUtils.buildClasspath();

    public static void main(String[] args) {
        List<Process> processos = new ArrayList<>();
        Process dashboardProc = null;

        try {
            //Limpar processos de execuções anteriores
            System.out.println("=".repeat(60));
            System.out.println("A verificar e limpar processos de execuções anteriores...");
            System.out.println("=".repeat(60));
            
            int processosLimpos = ProcessCleaner.limparProcessosAnteriores();
            
            if (processosLimpos > 0) {
                System.out.println("Aguardando portas ficarem disponíveis...");
                if (!ProcessCleaner.aguardarPortasDisponiveis(5000)) {
                    System.err.println("AVISO: Algumas portas ainda podem estar ocupadas.");
                }
            }
            
            System.out.println("=".repeat(60));
            System.out.println("A iniciar nova execução do sistema...");
            System.out.println("=".repeat(60));

            // 1. DASHBOARD
            System.out.println("A iniciar o Dashboard");
            dashboardProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Dashboard.DashboardMain").inheritIO().start();
            processos.add(dashboardProc);
            Thread.sleep(2500);

            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Dashboard iniciado com sucesso.");

            // 2. SAÍDA S
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "A iniciar Saída");
            Process saidaProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Saida.SaidaMain").inheritIO().start();
            processos.add(saidaProc);
            Thread.sleep(1000);
            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Saída S ativa.");

            // 3. CRUZAMENTOS (Cr1, Cr2, Cr3, Cr4, Cr5)
            String[] cruzamentos = {"Cr1", "Cr2", "Cr3", "Cr4", "Cr5"};

            for (String cruzamento : cruzamentos) {
                LogClienteDashboard.enviar(TipoLog.SISTEMA, "A iniciar Cruzamento " + cruzamento + "...");
                Process cruzProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Cruzamentos.CruzamentoMain", cruzamento).inheritIO().start();
                processos.add(cruzProc);
                Thread.sleep(1500);
                LogClienteDashboard.enviar(TipoLog.SUCESSO, "Cruzamento " + cruzamento + " ativo.");
            }

            // 4. GERADORES (todas as entradas: E1, E2, E3)
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "A iniciar geradores de veículos para todas as entradas (E1, E2, E3)");
            Process geradoresProc = new ProcessBuilder("java", "-cp", CLASSPATH, "PontosEntrada.PontosEntradasMain").inheritIO().start();
            processos.add(geradoresProc);

            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Geradores iniciados.");
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Sistema em execução");

            // Aguarda que o gerador termine
            LogClienteDashboard.enviar(TipoLog.AVISO, "Aguardando término dos geradores");
            geradoresProc.waitFor();
            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Geradores concluíram as suas operações.");

            // Aguarda tempo extra para veículos terminarem visualmente (2 minutos)
            LogClienteDashboard.enviar(TipoLog.AVISO, "Aguardando veículos terminarem de circular (120s)...");
            Thread.sleep(120_000);

            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Processos backend concluídos.");

            encerrarProcessosBackend(processos, dashboardProc);

            LogClienteDashboard.enviar(TipoLog.AVISO, "Dashboard continuará aberto até ser fechado manualmente.");

            dashboardProc.waitFor();
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Dashboard encerrado pelo utilizador.");
        } catch (Exception e) {
            LogClienteDashboard.enviar(TipoLog.ERRO, "Erro no Main: " + e.getMessage());
        } finally {
            encerrarProcessos(processos);
        }
    }
    
    /**
     * 
     * 
     * @param todos
     * @param dashboard 
     */
    private static void encerrarProcessosBackend(List<Process> todos, Process dashboard) {
        LogClienteDashboard.enviar(TipoLog.AVISO, "Encerrando processos backend");

        for (Process p : todos) {
            if (p != dashboard && p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {}
            }
        }
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Backend encerrado.");
    }

    private static void encerrarProcessos(List<Process> processos) {
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "A encerrar todos os processos.");

        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor();
                } catch (InterruptedException ignored) {}
            }
        }
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Todos os processos encerrados.");
    }
}
