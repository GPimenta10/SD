package Cruzamento;

import Veiculo.Veiculo;
import OutrasClasses.ComunicadorSocket;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Cruzamento 3 - Processo independente com ServerSocket.
 *
 * Cada cruzamento gere os seus próprios semáforos (threads locais)
 * e comunica com outros processos via sockets (Entrada, Saída, Dashboard).
 */
public class Cruzamento3 {

    private final int portaEscuta;
    private final String hostSaida;
    private final int portaSaida;
    public final String hostDashboard;
    public final int portaDashboard;

    private Semaforo semaforoE3;
    private Semaforo semaforoCr2;

    private final AtomicInteger totalVeiculos = new AtomicInteger(0);
    private final AtomicInteger totalMotos = new AtomicInteger(0);
    private final AtomicInteger totalCarros = new AtomicInteger(0);
    private final AtomicInteger totalCamioes = new AtomicInteger(0);

    private static final String NOME = "Cruzamento3";

    public Cruzamento3(int portaEscuta, String hostSaida, int portaSaida,
                       String hostDashboard, int portaDashboard) {
        this.portaEscuta = portaEscuta;
        this.hostSaida = hostSaida;
        this.portaSaida = portaSaida;
        this.hostDashboard = hostDashboard;
        this.portaDashboard = portaDashboard;
    }

    /**
     * MAIN - Executa como processo independente.
     */
    public static void main(String[] args) {
        int portaEscuta = 6003;
        String hostSaida = "127.0.0.1";
        int portaSaida = 7000;
        String hostDashboard = "127.0.0.1";
        int portaDashboard = 9000;

        if (args.length >= 3) {
            portaEscuta = Integer.parseInt(args[0]);
            hostSaida = args[1];
            portaSaida = Integer.parseInt(args[2]);
        }
        if (args.length >= 5) {
            hostDashboard = args[3];
            portaDashboard = Integer.parseInt(args[4]);
        }

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      CRUZAMENTO 3 - INICIANDO      ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Escuta: porta " + portaEscuta);
        System.out.println("Saída: " + hostSaida + ":" + portaSaida);
        System.out.println("Dashboard: " + hostDashboard + ":" + portaDashboard);
        System.out.println();

        Cruzamento3 cruzamento = new Cruzamento3(portaEscuta, hostSaida, portaSaida,
                hostDashboard, portaDashboard);
        cruzamento.iniciar();
    }

    /**
     * Inicia o cruzamento: cria semáforos e inicia servidor.
     */
    public void iniciar() {
        inicializarSemaforos();
        iniciarServidor();
        iniciarEnvioPeriodicoDeEstatisticas();
    }

    /**
     * Cria e inicia as threads dos semáforos.
     */
    private void inicializarSemaforos() {
        Object sincronizador = new Object();

        semaforoE3 = new Semaforo("Cr3-E3", 5000, 1500, sincronizador, true, this);
        semaforoCr2 = new Semaforo("Cr3-Cr2", 5000, 1500, sincronizador, false, this);

        semaforoE3.setOutroSemaforo(semaforoCr2);
        semaforoCr2.setOutroSemaforo(semaforoE3);

        semaforoE3.start();
        semaforoCr2.start();

        log("✓ Semáforos iniciados");
    }

    /**
     * ServerSocket - fica à escuta eternamente.
     */
    private void iniciarServidor() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new java.net.InetSocketAddress(portaEscuta));

            log("✓ ServerSocket iniciado na porta " + portaEscuta);
            log("⏳ Aguardando conexões...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("🔌 Nova conexão recebida");

                // Processa cada conexão numa thread separada
                new Thread(() -> processarConexao(clientSocket)).start();
            }

        } catch (IOException e) {
            System.err.println("❌ Erro fatal no ServerSocket: " + e.getMessage());
            e.printStackTrace();
        } finally {
            log("🛑 Servidor do cruzamento encerrado.");
        }
    }


    /**
     * Processa uma conexão recebida (possivelmente com vários veículos).
     */
    private void processarConexao(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                try {
                    Veiculo v = (Veiculo) in.readObject();
                    receberVeiculo(v);
                } catch (EOFException eof) {
                    break; // fim normal da stream
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("⚠️ Erro ao processar conexão: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /**
     * Recebe veículo e adiciona à fila correspondente.
     */
    private void receberVeiculo(Veiculo v) {
        v.setTimestampEntradaFila(System.currentTimeMillis());
        log("🚗 Recebido: " + v.getId() + " (" + v.getTipo() + ")");

        totalVeiculos.incrementAndGet();
        switch (v.getTipo()) {
            case MOTA -> totalMotos.incrementAndGet();
            case CARRO -> totalCarros.incrementAndGet();
            case CAMIAO -> totalCamioes.incrementAndGet();
        }

        semaforoE3.adicionarVeiculo(v);

        ComunicadorSocket.enviarEventoDashboard(
                NOME, "RECEBIDO",
                v.getId() + "," + v.getTipo(),
                hostDashboard, portaDashboard
        );
    }

    /**
     * Envia veículo para o processo de saída.
     */
    public void enviarParaSaida(Veiculo v) {
        v.registrarPassagemCruzamento(NOME);

        boolean sucesso = ComunicadorSocket.enviarVeiculo(
                v, hostSaida, portaSaida, NOME
        );

        if (sucesso) {
            log("✓ Enviado para Saída: " + v.getId());
            ComunicadorSocket.enviarEventoDashboard(
                    NOME, "SAIU", v.getId(),
                    hostDashboard, portaDashboard
            );
        }
    }

    /**
     * Envia estatísticas atuais para o Dashboard.
     */
    public void enviarEstatisticas() {
        String stats = String.format(
                "total=%d,motos=%d,carros=%d,camioes=%d,filaE3=%d,filaCr2=%d",
                totalVeiculos.get(),
                totalMotos.get(),
                totalCarros.get(),
                totalCamioes.get(),
                semaforoE3.getTamanhoFila(),
                semaforoCr2.getTamanhoFila()
        );

        ComunicadorSocket.enviarEstatisticas(NOME, stats, hostDashboard, portaDashboard);
    }

    /**
     * Thread auxiliar que envia estatísticas periodicamente.
     */
    private void iniciarEnvioPeriodicoDeEstatisticas() {
        new Thread(() -> {
            while (true) {
                try {
                    enviarEstatisticas();
                    Thread.sleep(2000); // a cada 2 segundos
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "ThreadEstatisticas-Cr3").start();
    }

    private void log(String mensagem) {
        String logMsg = "[" + NOME + "] " + mensagem;
        System.out.println(logMsg);
        ComunicadorSocket.enviarParaDashboard(logMsg, hostDashboard, portaDashboard);
    }
}
