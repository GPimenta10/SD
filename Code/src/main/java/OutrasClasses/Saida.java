package OutrasClasses;

import Veiculo.Veiculo;
import Veiculo.TipoVeiculo;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Saída - Nó final do sistema (S).
 * Recebe veículos e calcula estatísticas globais por tipo.
 * Sinaliza "FIM_SISTEMA" quando não chegam veículos por um período (inatividade).
 */
public class Saida {

    private final int porta;
    private final String hostDashboard;
    private final int portaDashboard;

    // Estatísticas
    private final AtomicInteger totalVeiculos = new AtomicInteger(0);
    private final ConcurrentHashMap<TipoVeiculo, AtomicInteger> contagemPorTipo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TipoVeiculo, Long> tempoMinimo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TipoVeiculo, Long> tempoMaximo = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<TipoVeiculo, Long> tempoTotal = new ConcurrentHashMap<>();

    private static final String NOME = "Saída";

    // Deteção de fim por inatividade
    private static final long IDLE_FINAL_MS = 7000; // 7s sem veículos => considera terminado
    private volatile long lastArrivalTs = System.currentTimeMillis();
    private final AtomicBoolean fimEnviado = new AtomicBoolean(false);

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
        // Thread periódica para enviar estatísticas
        iniciarEnvioPeriodico();

        // Thread guardiã que verifica inatividade e dispara FIM_SISTEMA
        iniciarDetetorInatividade();

        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            log("✓ ServerSocket iniciado na porta " + porta);
            log("⏳ Aguardando veículos...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> processarConexao(clientSocket), "Saida-ConnHandler").start();
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
                    Object obj = in.readObject();
                    if (!(obj instanceof Veiculo v)) {
                        // ignora mensagens inesperadas
                        continue;
                    }
                    registrarSaida(v);
                } catch (EOFException eof) {
                    break; // cliente fechou a stream, segue para fechar socket
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("⚠️ Erro ao processar veículo: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private void registrarSaida(Veiculo v) {
        v.registrarSaida();
        long tempoNoSistema = v.calcularTempoNoSistema();
        long tempoEspera = v.getTempoTotalEspera();
        TipoVeiculo tipo = v.getTipo();

        // Atualiza relógio de última chegada
        lastArrivalTs = System.currentTimeMillis();

        log("🏁 Saída: " + v.getId() + " (" + v.getTipo() + ") - " +
                (tempoNoSistema / 1000.0) + "s no sistema - " +
                (tempoEspera / 1000.0) + "s de espera");

        // Evento legível para o Dashboard
        ComunicadorSocket.enviarParaDashboard(
                "[Saída] Saída: " + v.getId() + " (" + v.getTipo() + ") - " +
                        (tempoNoSistema / 1000.0) + "s no sistema - " +
                        (tempoEspera / 1000.0) + "s de espera",
                hostDashboard, portaDashboard
        );

        // Estatísticas
        totalVeiculos.incrementAndGet();
        contagemPorTipo.get(tipo).incrementAndGet();
        tempoMinimo.compute(tipo, (k, ant) -> Math.min(ant, tempoNoSistema));
        tempoMaximo.compute(tipo, (k, ant) -> Math.max(ant, tempoNoSistema));
        tempoTotal.compute(tipo, (k, ant) -> ant + tempoNoSistema);

        // Dump parcial
        if (totalVeiculos.get() % 5 == 0) {
            mostrarEstatisticas();
        }
    }

    private void mostrarEstatisticas() {
        System.out.println("\n📊 ESTATÍSTICAS GLOBAIS:");
        System.out.println("   Total: " + totalVeiculos.get() + " veículos");

        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            int count = contagemPorTipo.get(tipo).get();
            if (count > 0) {
                long min = tempoMinimo.get(tipo) == Long.MAX_VALUE ? 0 : tempoMinimo.get(tipo) / 1000;
                long max = tempoMaximo.get(tipo) / 1000;
                long avg = (tempoTotal.get(tipo) / count) / 1000;

                System.out.printf("   %s: %d (min: %ds, max: %ds, média: %ds)%n",
                        tipo, count, min, max, avg);
            }
        }
        System.out.println();
    }

    private void iniciarEnvioPeriodico() {
        Thread t = new Thread(() -> {
            while (true) {
                try {
                    enviarEstatisticasDashboard();
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "ThreadEstatisticasSaida");
        t.setDaemon(true);
        t.start();
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

    /**
     * Deteta inatividade por X ms após a última saída e envia "FIM_SISTEMA" uma vez.
     * Evita depender do total esperado por configuração.
     */
    private void iniciarDetetorInatividade() {
        Thread t = new Thread(() -> {
            // Espera pelo menos uma saída antes de considerar FIM
            long ultimaMarcada = lastArrivalTs;
            while (true) {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    return;
                }
                long agora = System.currentTimeMillis();
                // Se já houve pelo menos uma saída e passou tempo sem novas
                if (totalVeiculos.get() > 0 &&
                        (agora - lastArrivalTs) >= IDLE_FINAL_MS &&
                        fimEnviado.compareAndSet(false, true)) {
                    enviarFimSistema();
                    return; // já terminou a função
                }
                // evita loop infinito caso nunca chegue nada
                if (totalVeiculos.get() == 0 && (agora - ultimaMarcada) > (IDLE_FINAL_MS * 4)) {
                    // Nenhum veículo em muito tempo: encerra com aviso
                    if (fimEnviado.compareAndSet(false, true)) {
                        enviarFimSistema("[Saída] FIM_SISTEMA (encerrado por inatividade sem veículos)");
                    }
                    return;
                }
            }
        }, "Saida-DetetorInatividade");
        t.setDaemon(true);
        t.start();
    }

    private void enviarFimSistema() {
        enviarFimSistema("[Saída] FIM_SISTEMA");
    }

    private void enviarFimSistema(String msg) {
        try {
            // 1️⃣ Imprime no stdout (para o Main apanhar)
            System.out.println("FIM_SISTEMA");
            System.out.flush(); // <-- força envio imediato

            // 2️⃣ Envia também para o Dashboard
            try (Socket socket = new Socket(hostDashboard, portaDashboard);
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
                out.println(msg);
            }

            // 3️⃣ Espera um pouco para garantir flush antes do encerramento
            Thread.sleep(2000);

            // 4️⃣ Fecha o processo da Saída de forma limpa
            System.out.println("[Saída] ✅ Encerrando servidor de saída...");
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Erro ao enviar FIM_SISTEMA: " + e.getMessage());
        }
    }

    private void log(String msg) {
        String logMsg = "[" + NOME + "] " + msg;
        System.out.println(logMsg);

        // Envia também o log para o Dashboard (se estiver ativo)
        try (Socket socket = new Socket(hostDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(logMsg);
        } catch (IOException ignored) {
            // Ignora caso o Dashboard ainda não esteja disponível
        }
    }
}
