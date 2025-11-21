import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.UIManager;

import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;
import Utils.MenuCarga;
import Utils.ProcessCleaner;

public class Main {

    private static final String CLASSPATH = buildClasspath();

    private static String buildClasspath() {
        String separator = System.getProperty("path.separator");
        String gsonPath = "lib/gson-2.13.1.jar";
        String flatLafPath = "lib/flatlaf-3.6.2.jar";
        String flatLafThemesPath = "lib/flatlaf-intellij-themes-3.6.2.jar";

        if (!new File(gsonPath).exists()) System.err.println("ERRO: Jar não encontrado: " + gsonPath);
        if (!new File(flatLafPath).exists()) System.err.println("ERRO: Jar não encontrado: " + flatLafPath);

        return String.join(separator, "target/classes", gsonPath, flatLafPath, flatLafThemesPath);
    }

    public static void main(String[] args) {
        // 0. CONFIGURAR TEMA
        try {
            FlatOneDarkIJTheme.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
        } catch (Exception ignored) {}

        List<Process> processos = new ArrayList<>();
        Process dashboardProc = null;

        try {
            // 1. LIMPEZA DE PROCESSOS
            System.out.println(">>> A INICIAR LIMPEZA...");
            int limpos = ProcessCleaner.limparProcessosAnteriores();
            if (limpos > 0) ProcessCleaner.aguardarPortasDisponiveis(5000);

            // 2. OBTER CARGA (Toda a lógica visual está agora encapsulada aqui)
            String cargaEscolhida = MenuCarga.obterCargaOuSair();

            // 3. INÍCIO DOS PROCESSOS
            
            // DASHBOARD
            System.out.println("A iniciar o Dashboard");
            dashboardProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Dashboard.DashboardMain")
                    .inheritIO().start();
            processos.add(dashboardProc);
            Thread.sleep(2500);

            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Dashboard iniciado com sucesso.");

            // SAÍDA
            Process saidaProc = new ProcessBuilder("java", "-cp", CLASSPATH, "Saida.SaidaMain")
                    .inheritIO().start();
            processos.add(saidaProc);
            Thread.sleep(1000);

            // CRUZAMENTOS
            String[] cruzamentos = {"Cr1", "Cr2", "Cr3", "Cr4", "Cr5"};
            for (String cruz : cruzamentos) {
                Process cruzProc = new ProcessBuilder("java", "-cp", CLASSPATH, 
                        "Cruzamentos.CruzamentoMain", cruz).inheritIO().start();
                processos.add(cruzProc);
                Thread.sleep(1500);
            }

            // GERADORES (Com a carga escolhida)
            Process geradoresProc = new ProcessBuilder("java", "-cp", CLASSPATH, 
                    "PontosEntrada.PontosEntradasMain", cargaEscolhida).inheritIO().start();
            processos.add(geradoresProc);

            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Geradores iniciados (Carga: " + cargaEscolhida + ").");

            // AGUARDAR FIM
            geradoresProc.waitFor();
            LogClienteDashboard.enviar(TipoLog.SUCESSO, "Geradores terminaram.");
            
            LogClienteDashboard.enviar(TipoLog.AVISO, "Aguardando escoamento (120s)...");
            Thread.sleep(120_000);

            encerrarProcessosBackend(processos, dashboardProc);
            
            LogClienteDashboard.enviar(TipoLog.AVISO, "Feche a janela do Dashboard para sair.");
            dashboardProc.waitFor();
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            encerrarProcessos(processos);
        }
    }

    private static void encerrarProcessosBackend(List<Process> todos, Process dashboard) {
        LogClienteDashboard.enviar(TipoLog.AVISO, "Encerrando processos backend");
        for (Process p : todos) {
            if (p != dashboard && p.isAlive()) {
                p.destroy();
                try { p.waitFor(); } catch (InterruptedException ignored) {}
            }
        }
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Backend encerrado.");
    }

    private static void encerrarProcessos(List<Process> processos) {
        System.out.println("[Main] A encerrar processos restantes.");
        for (Process p : processos) if (p.isAlive()) p.destroy();
    }
}