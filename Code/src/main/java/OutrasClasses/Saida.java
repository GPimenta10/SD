package OutrasClasses;

import Dashboard.ComunicadorDashboard;
import Veiculo.TipoVeiculo;
import java.io.*;
import java.net.*;

/**
 * Processo que representa o ponto de saída (S).
 * Recebe veículos finalizados e delega o cálculo de estatísticas e comunicação.
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
            System.out.printf("[Saída] Processo iniciado na porta %d%n", portaEntrada);

            while (executando) {
                Socket clienteSocket = serverSocket.accept();
                new Thread(() -> processarCliente(clienteSocket)).start();
            }

        } catch (IOException e) {
            System.err.printf("[Saída] Erro ao iniciar: %s%n", e.getMessage());
        } finally {
            finalizar();
        }
    }

    private void processarCliente(Socket socket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = reader.readLine()) != null) {
                processarVeiculo(linha.trim());
            }
        } catch (IOException e) {
            System.err.printf("[Saída] Erro ao processar cliente: %s%n", e.getMessage());
        }
    }

    private void processarVeiculo(String mensagem) {
        // Formato esperado: VEICULO|id|tipo|tempoChegadaSistema
        String[] partes = mensagem.split("\\|");
        if (partes.length < 4 || !partes[0].equals("VEICULO")) return;

        String id = partes[1];
        TipoVeiculo tipo = TipoVeiculo.valueOf(partes[2]);
        long tempoChegada = Long.parseLong(partes[3]);
        long tempoSaida = System.currentTimeMillis();

        long dwelling = tempoSaida - tempoChegada;
        estatisticas.registrarVeiculo(tipo, dwelling);

        System.out.printf("[Saída] Veículo %s (%s) finalizado - %.2fs%n", id, tipo, dwelling / 1000.0);

        // Envia info individual e total para o dashboard
        ComunicadorDashboard.getInstance().enviar(String.format(
                "[Saída] id=%s tipo=%s percurso=E3->Cr3->S tempo=%.2fs",
                id, tipo, dwelling / 1000.0
        ));
        ComunicadorDashboard.getInstance().enviar("[Saída_Total] " + estatisticas.getTotalVeiculos());
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
