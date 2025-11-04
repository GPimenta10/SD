import PontosEntrada.GeradorVeiculos;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe Main - Orquestrador da Simulação de Tráfego Urbano
 * Gerencia a inicialização e finalização de todos os processos usando ProcessBuilder.
 *
 * Versão Simplificada: apenas E3 → Cr3 → S
 */
public class Main {

    // Configurações de portas
    private static final int PORTA_CR3 = 8003;
    private static final int PORTA_SAIDA = 9100;

    // Configurações de simulação
    private static final int NUM_VEICULOS = 11;
    private static final GeradorVeiculos.ModoGeracao MODO_GERACAO = GeradorVeiculos.ModoGeracao.FIXO;
    private static final long INTERVALO_FIXO_MS = 2000; // 2 segundos entre veículos
    private static final double TAXA_POISSON = 0.5;     // 0.5 veículos/segundo (alternativa)

    // Controla quais entradas estão ativas
    private static final boolean ATIVAR_E1 = false;
    private static final boolean ATIVAR_E2 = false;
    private static final boolean ATIVAR_E3 = true;

    private static final List<Process> processos = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════╗");
        System.out.println("║   SIMULADOR DE TRÁFEGO URBANO DISTRIBUÍDO      ║");
        System.out.println("║        Sistemas Distribuídos 2025/2026         ║");
        System.out.println("╚════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Versão Simplificada: E3 → Cr3 → S");
        System.out.println();

        Runtime.getRuntime().addShutdownHook(new Thread(Main::finalizarSimulacao));

        try {
            executarComProcessos();
        } catch (Exception e) {
            System.err.println("Erro durante a simulação: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void executarComProcessos() throws Exception {
        // 0. Dashboard
        System.out.println("[Main] Iniciando Dashboard Swing...");
        Process dashboard = iniciarProcesso("Dashboard.DashboardUI");
        processos.add(dashboard);
        Thread.sleep(2000);

        // 1. Saída
        System.out.println("[Main] Iniciando processo Saída (porta " + PORTA_SAIDA + ")...");
        Process saida = iniciarProcesso("OutrasClasses.Saida", String.valueOf(PORTA_SAIDA));
        processos.add(saida);
        Thread.sleep(2000);

        // 2. Cruzamento Cr3
        System.out.println("[Main] Iniciando processo Cruzamento Cr3 (porta " + PORTA_CR3 + ")...");
        Process cr3 = iniciarProcesso("Cruzamentos.Cruzamento", "Cr3", String.valueOf(PORTA_CR3));
        processos.add(cr3);
        Thread.sleep(2000);

        // 3. Geradores de veículos
        if (ATIVAR_E1) {
            System.out.println("[Main] (Desativado neste cenário) Gerador E1.");
        }
        if (ATIVAR_E2) {
            System.out.println("[Main] (Desativado neste cenário) Gerador E2.");
        }
        if (ATIVAR_E3) {
            System.out.println("[Main] Iniciando Gerador de Veículos (E3)...\n");
            executarGerador("E3", PORTA_CR3);
        }

        System.out.println("\n[Main] Aguardando processamento final dos veículos (15s)...");
        Thread.sleep(15000);

        System.out.println("[Main] Finalizando processos...");
        finalizarProcessos();

        System.out.println("\n[Main] Simulação concluída!");
    }

    private static Process iniciarProcesso(String classe, String... args) throws IOException {
        String classpath = detectarClasspath();

        List<String> comando = new ArrayList<>();
        comando.add("java");
        comando.add("-cp");
        comando.add(classpath);
        comando.add(classe);
        for (String arg : args) comando.add(arg);

        System.out.println("[Main] Comando: " + String.join(" ", comando));

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.inheritIO();
        return pb.start();
    }

    private static String detectarClasspath() {
        String cp = System.getProperty("java.class.path");
        if (cp.contains("target/classes") || cp.contains("out/production") || cp.contains("bin"))
            return cp;

        File target = new File("target/classes");
        if (target.exists()) return target.getAbsolutePath();
        File bin = new File("bin");
        if (bin.exists()) return bin.getAbsolutePath();

        return ".";
    }

    private static void executarGerador(String entrada, int porta) throws Exception {
        String classpath = detectarClasspath();
        List<String> comando = new ArrayList<>(List.of(
                "java", "-cp", classpath,
                "PontosEntrada.GeradorVeiculos",
                entrada,
                String.valueOf(porta),
                MODO_GERACAO.toString()
        ));

        comando.add(MODO_GERACAO == GeradorVeiculos.ModoGeracao.FIXO
                ? String.valueOf(INTERVALO_FIXO_MS)
                : String.valueOf(TAXA_POISSON));

        comando.add(String.valueOf(NUM_VEICULOS));

        System.out.println("[Main] Comando gerador: " + String.join(" ", comando));

        ProcessBuilder pb = new ProcessBuilder(comando);
        pb.inheritIO();
        Process proc = pb.start();
        proc.waitFor();
    }

    private static void finalizarProcessos() {
        for (Process p : processos) {
            if (p.isAlive()) {
                p.destroy();
                try {
                    p.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
                } catch (InterruptedException ignored) {}
                if (p.isAlive()) p.destroyForcibly();
            }
        }
        processos.clear();
    }

    private static void finalizarSimulacao() {
        System.out.println("\n[Main] Finalizando simulação...");
        finalizarProcessos();
    }
}
