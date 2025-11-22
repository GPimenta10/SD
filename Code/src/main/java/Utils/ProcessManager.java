package Utils;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Classe responsável pela gestão de processos do sistema.
 * Permite iniciar, encerrar e aguardar processos Java.
 */
public class ProcessManager {

    private static final String CLASSPATH = ClassPath.buildClasspath();
    private static final String[] CRUZAMENTOS = {"Cr1", "Cr2", "Cr3", "Cr4", "Cr5"};

    private static final int DELAY_DASHBOARD = 2500;
    private static final int DELAY_SAIDA = 1000;
    private static final int DELAY_CRUZAMENTO = 1500;
    private static final int DELAY_FINALIZACAO = 120_000;

    private final List<Process> processos = new ArrayList<>();
    private Process dashboardProc;

    /**
     * Inicia todos os processos do sistema.
     *
     * @param carga Carga selecionada
     * @param cenario Cenário selecionado
     */
    public void iniciarTodos(String carga, String cenario) throws Exception {
        iniciarDashboard();
        iniciarSaida();
        iniciarCruzamentos();
        iniciarGeradores(carga, cenario);
    }

    /**
     * Aguarda finalização dos veículos e encerra o backend.
     */
    public void aguardarFinalizacao() throws Exception {
        LogClienteDashboard.enviar(TipoLog.AVISO, "Aguardando veículos terminarem de circular (120s)...");
        Thread.sleep(DELAY_FINALIZACAO);

        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Processos backend concluídos.");
        encerrarBackend();

        LogClienteDashboard.enviar(TipoLog.AVISO, "Dashboard continuará aberto até ser fechado manualmente.");
        dashboardProc.waitFor();
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Dashboard encerrado pelo utilizador.");
    }

    /**
     * Encerra todos os processos do sistema.
     */
    public void encerrarTodos() {
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "A encerrar todos os processos.");

        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                aguardarProcesso(p);
            }
        }
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Todos os processos encerrados.");
    }

    private void iniciarDashboard() throws Exception {
        System.out.println("A iniciar o Dashboard");
        dashboardProc = iniciarProcesso("Dashboard.DashboardMain");
        Thread.sleep(DELAY_DASHBOARD);
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Dashboard iniciado com sucesso.");
    }

    private void iniciarSaida() throws Exception {
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "A iniciar Saída");
        iniciarProcesso("Saida.SaidaMain");
        Thread.sleep(DELAY_SAIDA);
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Saída S ativa.");
    }

    private void iniciarCruzamentos() throws Exception {
        for (String cruzamento : CRUZAMENTOS) {
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "A iniciar Cruzamento " + cruzamento + "...");
            iniciarProcesso("Cruzamentos.CruzamentoMain", cruzamento);
            Thread.sleep(DELAY_CRUZAMENTO);
            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Cruzamento " + cruzamento + " ativo.");
        }
    }

    private void iniciarGeradores(String carga, String cenario) throws Exception {
        LogClienteDashboard.enviar(TipoLog.SISTEMA,
                "A iniciar geradores (Carga: " + carga + ", Cenário: " + cenario + ")");

        Process geradoresProc = iniciarProcesso("PontosEntrada.PontosEntradaMain", carga, cenario);

        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Geradores iniciados.");
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Sistema em execução");

        LogClienteDashboard.enviar(TipoLog.AVISO, "Aguardando término dos geradores");
        geradoresProc.waitFor();
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Geradores concluíram as suas operações.");
    }

    private void encerrarBackend() {
        LogClienteDashboard.enviar(TipoLog.AVISO, "Encerrando processos backend");

        for (Process p : processos) {
            if (p != dashboardProc && p.isAlive()) {
                p.destroy();
                aguardarProcesso(p);
            }
        }
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Backend encerrado.");
    }

    private Process iniciarProcesso(String classe, String... args) throws Exception {
        List<String> comando = new ArrayList<>();
        comando.add("java");
        comando.add("-Dfile.encoding=UTF-8");
        comando.add("-cp");
        comando.add(CLASSPATH);
        comando.add(classe);

        for (String arg : args) {
            comando.add(arg);
        }

        Process proc = new ProcessBuilder(comando).inheritIO().start();
        processos.add(proc);
        return proc;
    }

    private void aguardarProcesso(Process p) {
        try {
            p.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}