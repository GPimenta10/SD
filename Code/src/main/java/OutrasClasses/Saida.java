package OutrasClasses;

import Dashboard.ComunicadorDashboard;
import Veiculo.Veiculo;
import java.io.*;
import java.net.*;

/**
 * Processo que representa o ponto de saída (S).
 *
 * ALTERAÇÃO: Recebe objetos Veiculo serializados com todo o histórico
 * (caminho completo, tempos, etc) para estatísticas precisas.
 */
public class Saida {

    private final int portaEntrada;
    private ServerSocket serverSocket;
    private volatile boolean executando;

    private final EstatisticasSaida estatisticas;

    public Saida(int portaEntrada) {
        this.portaEntrada = portaEntrada;
        this.executando = true;
        this.estatisticas = new EstatisticasSaida();
    }

    public void iniciar() {
        try {
            serverSocket = new ServerSocket(portaEntrada);
            System.out.printf("[Saída] Processo iniciado na porta %d [MODO SERIALIZADO]%n", portaEntrada);

            while (executando) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(() -> processarCliente(clienteSocket)).start();
            }

        } catch (IOException e) {
            if (executando) {
                System.err.printf("[Saída] Erro ao iniciar: %s%n", e.getMessage());
            }
        } finally {
            finalizar();
        }
    }

    /**
     * Processa conexão de um cliente (cruzamento enviando veículos).
     */
    private void processarCliente(Socket socket) {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            System.out.println("[Saída] Cliente conectado [MODO SERIALIZADO]");

            while (true) {
                try {
                    // Recebe objeto Veiculo serializado
                    Veiculo veiculo = (Veiculo) ois.readObject();
                    processarVeiculo(veiculo);

                } catch (EOFException e) {
                    // Cliente fechou conexão normalmente
                    System.out.println("[Saída] Cliente desconectado");
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.printf("[Saída] Classe Veiculo não encontrada: %s%n", e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.printf("[Saída] Erro ao processar cliente: %s%n", e.getMessage());
        }
    }

    /**
     * Processa veículo que chegou à saída.
     * IMPORTANTE: Agora recebe o objeto completo com todo o histórico!
     */
    private void processarVeiculo(Veiculo veiculo) {
        // Marca tempo de saída
        long tempoSaida = System.currentTimeMillis();
        veiculo.setTempoSaida(tempoSaida);

        // Calcula dwelling time (tempo total no sistema)
        long dwelling = veiculo.getDwellingTime();

        // Registra nas estatísticas
        estatisticas.registrarVeiculo(veiculo.getTipo(), dwelling);

        // Log detalhado com caminho completo e timestamp
        System.out.printf("[Saída] ✅ Veículo %s (%s) CHEGOU À SAÍDA - %.2fs | Caminho: %s | Posição: %d/%d%n",
                veiculo.getId(),
                veiculo.getTipo(),
                dwelling / 1000.0,
                veiculo.getCaminho(),
                veiculo.getIndiceCaminhoAtual(),
                veiculo.getCaminho().size());

        // Envia informação para o dashboard
        String caminhoStr = String.join("->", veiculo.getCaminho());
        ComunicadorDashboard.getInstance().enviar(String.format(
                "[Saída] id=%s tipo=%s percurso=%s->%s tempo=%.2fs",
                veiculo.getId(),
                veiculo.getTipo(),
                veiculo.getPontoEntrada(),
                caminhoStr,
                dwelling / 1000.0
        ));

        // Atualiza contador total no dashboard
        int totalAtual = estatisticas.getTotalVeiculos();
        ComunicadorDashboard.getInstance().enviar("[Saída_Total] " + totalAtual);

        System.out.printf("[Saída] 📊 Total de veículos que saíram: %d%n", totalAtual);
    }

    public void finalizar() {
        executando = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {}
        System.out.println("[Saída] Processo finalizado");
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("Uso: java OutrasClasses.Saida <porta>");
            System.exit(1);
        }
        int porta = Integer.parseInt(args[0]);
        Saida saida = new Saida(porta);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            saida.estatisticas.imprimirFinais();
            saida.finalizar();
        }));

        saida.iniciar();
    }
}