package OutrasClasses;

import Veiculo.Veiculo;
import Veiculo.TipoVeiculo;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saída - Nó final do sistema (S).
 * Recebe veículos e calcula estatísticas globais por tipo.
 */
public class Saida {

    private final int porta;
    private final String hostDashboard;
    private final int portaDashboard;

    private final AtomicInteger totalVeiculos = new AtomicInteger(0);
    private final ConcurrentHashMap<TipoVeiculo, AtomicInteger> contagemPorTipo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TipoVeiculo, Long> tempoMinimo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TipoVeiculo, Long> tempoMaximo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TipoVeiculo, Long> tempoTotal = new ConcurrentHashMap<>();

    private static final String NOME = "Saída";

    public Saida(int porta, String hostDashboard, int portaDashboard) {
        this.porta = porta;
        this.hostDashboard = hostDashboard;
        this.portaDashboard = portaDashboard;

        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            contagemPorTipo.put(tipo, new AtomicInteger(0));
            tempoMinimo.put(tipo, Long.MAX_VALUE);
            tempoMaximo.put(tipo, 0L);
            tempoTotal.put(tipo, 0L);
        }
    }

    public static void main(String[] args) {
        int porta = 7000;
        String hostDashboard = "127.0.0.1";
        int portaDashboard = 9000;

        if (args.length >= 1) porta = Integer.parseInt(args[0]);
        if (args.length >= 3) {
            hostDashboard = args[1];
            portaDashboard = Integer.parseInt(args[2]);
        }

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║         SAÍDA - INICIANDO          ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Porta: " + porta);
        System.out.println("Dashboard: " + hostDashboard + ":" + portaDashboard);
        System.out.println("─".repeat(50));

        Saida saida = new Saida(porta, hostDashboard, portaDashboard);
        saida.iniciar();
    }

    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            log("✓ ServerSocket iniciado na porta " + porta);
            log("⏳ Aguardando veículos...");

            iniciarEnvioPeriodico();

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processarConexao(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("❌ Erro fatal na Saída: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void processarConexao(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                try {
                    Veiculo v = (Veiculo) in.readObject();
                    registrarSaida(v);
                } catch (EOFException eof) {
                    break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("⚠️ Erro ao processar veículo: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private final int TOTAL_ESPERADO = 20; // ⚙️ ajusta dinamicamente conforme o número gerado pelo E3

    private void registrarSaida(Veiculo v) {
        v.registrarSaida();
        long tempoNoSistema = v.calcularTempoNoSistema();
        long tempoEspera = v.getTempoTotalEspera();
        TipoVeiculo tipo = v.getTipo();

        log("🏁 Saída: " + v.getId() + " (" + v.getTipo() + ") - " +
                (tempoNoSistema / 1000.0) + "s no sistema - " +
                (tempoEspera / 1000.0) + "s de espera");

        // ✅ Envia evento normal de saída para o Dashboard
        ComunicadorSocket.enviarParaDashboard("[Saída] Saída: " + v.getId() + " (" + v.getTipo() + ") - " +
                        (tempoNoSistema / 1000.0) + "s no sistema - " +
                        (tempoEspera / 1000.0) + "s de espera",
                hostDashboard, portaDashboard);

        // Atualiza estatísticas
        totalVeiculos.incrementAndGet();
        contagemPorTipo.get(tipo).incrementAndGet();
        tempoMinimo.compute(tipo, (k, vAntigo) -> Math.min(vAntigo, tempoNoSistema));
        tempoMaximo.compute(tipo, (k, vAntigo) -> Math.max(vAntigo, tempoNoSistema));
        tempoTotal.compute(tipo, (k, vAntigo) -> vAntigo + tempoNoSistema);

        // Exibe estatísticas parciais a cada 5 veículos
        if (totalVeiculos.get() % 5 == 0) {
            mostrarEstatisticas();
        }

        // 🏁 Quando todos os veículos esperados saírem, envia sinal de conclusão global
        if (totalVeiculos.get() >= TOTAL_ESPERADO) {
            log("✅ Todos os veículos processados. Sistema completo!");
            ComunicadorSocket.enviarParaDashboard("[Saída] FIM_SISTEMA", hostDashboard, portaDashboard);
        }
    }

    private void mostrarEstatisticas() {
        System.out.println("\n📊 ESTATÍSTICAS GLOBAIS:");
        System.out.println("   Total: " + totalVeiculos.get() + " veículos");

        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            int count = contagemPorTipo.get(tipo).get();
            if (count > 0) {
                long min = tempoMinimo.get(tipo) / 1000;
                long max = tempoMaximo.get(tipo) / 1000;
                long avg = (tempoTotal.get(tipo) / count) / 1000;

                System.out.printf("   %s: %d (min: %ds, max: %ds, média: %ds)%n",
                        tipo, count, min, max, avg);
            }
        }
        System.out.println();
    }

    private void iniciarEnvioPeriodico() {
        new Thread(() -> {
            while (true) {
                try {
                    enviarEstatisticasDashboard();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "ThreadEstatisticasSaida").start();
    }

    private void enviarEstatisticasDashboard() {
        StringBuilder sb = new StringBuilder("total=" + totalVeiculos.get());
        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            int count = contagemPorTipo.get(tipo).get();
            if (count > 0) {
                long avg = (tempoTotal.get(tipo) / count) / 1000;
                sb.append(",").append(tipo).append("=").append(count)
                        .append(" avg=").append(avg).append("s");
            }
        }
        ComunicadorSocket.enviarEstatisticas(NOME, sb.toString(), hostDashboard, portaDashboard);
    }

    private void log(String msg) {
        String logMsg = "[" + NOME + "] " + msg;
        System.out.println(logMsg);
        try (Socket socket = new Socket(hostDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(logMsg);
        } catch (IOException ignored) {}
    }
}
