package Cruzamento;

import Veiculo.Veiculo;
import OutrasClasses.ComunicadorSocket;
import java.io.*;
import java.net.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class CruzamentoBase {

    protected final int portaEscuta;
    protected final String hostSaida;
    protected final int portaSaida;
    protected final String hostDashboard;
    protected final int portaDashboard;
    protected final String nome;

    protected Semaforo[] semaforos;
    protected volatile boolean ativo = true;

    protected final AtomicInteger totalVeiculos = new AtomicInteger(0);
    protected final AtomicInteger totalMotos = new AtomicInteger(0);
    protected final AtomicInteger totalCarros = new AtomicInteger(0);
    protected final AtomicInteger totalCamioes = new AtomicInteger(0);

    public CruzamentoBase(String nome, int portaEscuta,
                          String hostSaida, int portaSaida,
                          String hostDashboard, int portaDashboard) {
        this.nome = nome;
        this.portaEscuta = portaEscuta;
        this.hostSaida = hostSaida;
        this.portaSaida = portaSaida;
        this.hostDashboard = hostDashboard;
        this.portaDashboard = portaDashboard;
    }

    // ---------- MÉTODOS ABSTRATOS (cada cruzamento define os seus semáforos) ----------
    protected abstract void inicializarSemaforos();

    // ---------- LÓGICA COMUM ----------
    public void iniciar() {
        inicializarSemaforos();
        iniciarServidor();
        iniciarEnvioPeriodicoDeEstatisticas();
    }

    protected void iniciarServidor() {
        try (ServerSocket serverSocket = new ServerSocket()) {
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(portaEscuta));

            log("🟢 Servidor ativo na porta " + portaEscuta);

            while (ativo) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarConexao(socket)).start();
            }

            log("✅ Servidor encerrado com segurança.");
        } catch (BindException e) {
            log("❌ Porta " + portaEscuta + " já está em uso. Tente novamente.");
        } catch (IOException e) {
            log("❌ Erro no servidor: " + e.getMessage());
        }
    }

    protected void processarConexao(Socket socket) {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (true) {
                Veiculo v = (Veiculo) in.readObject();
                receberVeiculo(v);
            }
        } catch (EOFException eof) {
            // fim normal
        } catch (IOException | ClassNotFoundException e) {
            log("⚠️ Erro ao processar conexão: " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    protected void receberVeiculo(Veiculo v) {
        v.setTimestampEntradaFila(System.currentTimeMillis());
        log("🚗 Recebido: " + v.getId() + " (" + v.getTipo() + ")");

        totalVeiculos.incrementAndGet();
        switch (v.getTipo()) {
            case MOTA -> totalMotos.incrementAndGet();
            case CARRO -> totalCarros.incrementAndGet();
            case CAMIAO -> totalCamioes.incrementAndGet();
        }

        // Por defeito, adiciona sempre ao primeiro semáforo
        semaforos[0].adicionarVeiculo(v);

        ComunicadorSocket.enviarEventoDashboard(
                nome, "RECEBIDO",
                v.getId() + "," + v.getTipo(),
                hostDashboard, portaDashboard
        );
    }

    public void enviarParaSaida(Veiculo v) {
        v.registrarPassagemCruzamento(nome);
        boolean sucesso = ComunicadorSocket.enviarVeiculo(
                v, hostSaida, portaSaida, nome
        );
        if (sucesso) {
            log("✓ Enviado para Saída: " + v.getId());
            ComunicadorSocket.enviarEventoDashboard(
                    nome, "SAIU", v.getId(),
                    hostDashboard, portaDashboard
            );
        }
    }

    public void enviarEstatisticas() {
        int fila1 = (semaforos.length > 0) ? semaforos[0].getTamanhoFila() : 0;
        int fila2 = (semaforos.length > 1) ? semaforos[1].getTamanhoFila() : 0;

        String stats = String.format(
                "total=%d,motos=%d,carros=%d,camioes=%d,fila1=%d,fila2=%d",
                totalVeiculos.get(), totalMotos.get(), totalCarros.get(),
                totalCamioes.get(), fila1, fila2
        );

        ComunicadorSocket.enviarEstatisticas(nome, stats, hostDashboard, portaDashboard);
    }

    protected void iniciarEnvioPeriodicoDeEstatisticas() {
        new Thread(() -> {
            while (ativo) {
                try {
                    enviarEstatisticas();
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }, "ThreadEstatisticas-" + nome).start();
    }

    protected void log(String msg) {
        String logMsg = "[" + nome + "] " + msg;
        System.out.println(logMsg);
        ComunicadorSocket.enviarParaDashboard(logMsg, hostDashboard, portaDashboard);
    }
}
