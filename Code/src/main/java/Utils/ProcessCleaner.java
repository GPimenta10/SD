package Utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashSet;
import java.util.Set;

/**
 * Utilitário para limpar processos Java de execuções anteriores
 * que estejam a ocupar as portas do sistema de tráfego.
 */
public class ProcessCleaner {
    // Portas utilizadas pelo sistema
    private static final int[] PORTAS_SISTEMA = {5001, 5002, 5003, 5004, 5005, 5999, 6000};

    // PID do processo atual (para não o matar)
    private static final long PID_ATUAL = ProcessHandle.current().pid();

    /**
     * Termina todos os processos Java que estejam a usar as portas do sistema,
     * exceto o processo atual.
     *
     * @return número de processos terminados
     */
    public static int limparProcessosAnteriores() {
        System.out.println("[ProcessCleaner] A verificar processos anteriores (PID atual: " + PID_ATUAL + ")...");

        Set<Long> pidsParaTerminar = new HashSet<>();

        for (int porta : PORTAS_SISTEMA) {
            Long pid = obterPidPorPorta(porta);
            if (pid != null && pid != PID_ATUAL) {
                pidsParaTerminar.add(pid);
            }
        }

        if (pidsParaTerminar.isEmpty()) {
            System.out.println("[ProcessCleaner] Nenhum processo anterior encontrado.");
            return 0;
        }

        System.out.println("[ProcessCleaner] Encontrados " + pidsParaTerminar.size() + " processos a terminar: " + pidsParaTerminar);

        int terminados = 0;
        for (Long pid : pidsParaTerminar) {
            if (terminarProcesso(pid)) {
                terminados++;
            }
        }

        // Aguardar um pouco para as portas ficarem disponíveis
        try {
            Thread.sleep(1500);
        } catch (InterruptedException ignored) {}

        System.out.println("[ProcessCleaner] " + terminados + " processos terminados com sucesso.");
        return terminados;
    }

    /**
     * Obtém o PID do processo que está a usar uma porta específica.
     * Funciona em Windows e Linux/Mac.
     *
     * @param porta
     * @return
     */
    private static Long obterPidPorPorta(int porta) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                // Windows: netstat -ano | findstr :PORTA
                pb = new ProcessBuilder("cmd", "/c",
                        "netstat -ano | findstr LISTENING | findstr :" + porta);
            } else {
                // Linux/Mac: lsof -i :PORTA -t
                pb = new ProcessBuilder("sh", "-c",
                        "lsof -i :" + porta + " -t 2>/dev/null | head -1");
            }

            pb.redirectErrorStream(true);
            Process proc = pb.start();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(proc.getInputStream()))) {

                String linha = reader.readLine();
                if (linha == null || linha.trim().isEmpty()) {
                    return null;
                }

                if (os.contains("win")) {
                    // Windows: formato "TCP 0.0.0.0:5001 0.0.0.0:0 LISTENING 12345"
                    String[] partes = linha.trim().split("\\s+");
                    if (partes.length >= 5) {
                        return Long.valueOf(partes[partes.length - 1]);
                    }
                } else {
                    // Linux/Mac: apenas o PID
                    return Long.valueOf(linha.trim());
                }
            }

            proc.waitFor();
        } catch (IOException | InterruptedException | NumberFormatException e) {}

        return null;
    }

    /**
     * Termina um processo pelo PID.
     *
     * @param pid
     * @return
     */
    private static boolean terminarProcesso(long pid) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("taskkill", "/F", "/PID", String.valueOf(pid));
            } else {
                pb = new ProcessBuilder("kill", "-9", String.valueOf(pid));
            }

            Process proc = pb.start();
            int exitCode = proc.waitFor();

            if (exitCode == 0) {
                System.out.println("[ProcessCleaner] Processo " + pid + " terminado.");
                return true;
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("[ProcessCleaner] Erro ao terminar processo " + pid + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Verifica se uma porta específica está disponível.
     *
     * @param porta
     * @return
     */
    public static boolean portaDisponivel(int porta) {
        try (java.net.ServerSocket ss = new java.net.ServerSocket(porta)) {
            ss.setReuseAddress(true);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Aguarda até todas as portas do sistema ficarem disponíveis.
     *
     * @param timeoutMs tempo máximo de espera em milissegundos
     * @return true se todas as portas ficaram disponíveis
     */
    public static boolean aguardarPortasDisponiveis(long timeoutMs) {
        long inicio = System.currentTimeMillis();

        while (System.currentTimeMillis() - inicio < timeoutMs) {
            boolean todasDisponiveis = true;

            for (int porta : PORTAS_SISTEMA) {
                if (!portaDisponivel(porta)) {
                    todasDisponiveis = false;
                    break;
                }
            }

            if (todasDisponiveis) {
                return true;
            }

            try {
                Thread.sleep(200);
            } catch (InterruptedException ignored) {}
        }

        return false;
    }

    /**
     * Termina TODOS os processos do sistema (incluindo o atual).
     * Usado quando o Dashboard é fechado para encerrar tudo.
     */
    public static void terminarTodosProcessosSistema() {
        System.out.println("[ProcessCleaner] A terminar todos os processos do sistema...");

        Set<Long> pidsParaTerminar = new HashSet<>();

        for (int porta : PORTAS_SISTEMA) {
            Long pid = obterPidPorPorta(porta);
            if (pid != null && pid != PID_ATUAL) {
                pidsParaTerminar.add(pid);
            }
        }

        // Terminar processos filhos primeiro
        for (Long pid : pidsParaTerminar) {
            terminarProcesso(pid);
        }

        // Tentar encontrar e terminar o processo pai (Main.java no IDE)
        try {
            ProcessHandle.current().parent().ifPresent(parent -> {
                String cmd = parent.info().command().orElse("");
                // Verificar se o pai é um processo Java (IDE a correr Main)
                if (cmd.toLowerCase().contains("java")) {
                    System.out.println("[ProcessCleaner] A terminar processo pai (IDE): " + parent.pid());
                    parent.destroyForcibly();
                }
            });
        } catch (Exception e) {
            System.err.println("[ProcessCleaner] Não foi possível terminar processo pai: " + e.getMessage());
        }

        System.out.println("[ProcessCleaner] Todos os processos terminados.");
    }
}
