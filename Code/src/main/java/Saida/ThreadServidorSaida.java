package Saida;

import Rede.Mensagem;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP da Saída.
 * Recebe mensagens JSON do tipo "VEICULO" enviadas pelos cruzamentos finais
 * (ex.: Cr3 e Cr5) e delega o registo à classe Saida.
 */
public class ThreadServidorSaida extends Thread {

    private final int portaServidor;
    private final Saida saida;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    public ThreadServidorSaida(int portaServidor, Saida saida) {
        super("ThreadServidorSaida");
        this.portaServidor = portaServidor;
        this.saida = saida;
        setDaemon(true);
    }

    @Override
    public void run() {
        System.out.printf("[ThreadServidor Saída] A escutar na porta %d...%n", portaServidor);

        try (ServerSocket serverSocket = new ServerSocket(portaServidor)) {
            while (ativo) {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratarLigacao(socket)).start();
            }
        } catch (Exception e) {
            if (ativo) {
                System.err.println("[ThreadServidor Saída] Erro no servidor: " + e.getMessage());
            } else {
                System.out.println("[ThreadServidor Saída] Encerrado.");
            }
        }
    }

    private void tratarLigacao(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String linha;
            while ((linha = in.readLine()) != null) {
                Mensagem mensagem = Mensagem.fromJson(linha);

                if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
                    Object objVeiculo = mensagem.getConteudo().get("veiculo");
                    Veiculo veiculo = gson.fromJson(gson.toJson(objVeiculo), Veiculo.class);

                    // Regista a chegada do veículo na saída
                    saida.registarVeiculo(veiculo);
                } else {
                    System.out.printf("[ThreadServidor Saída] Mensagem ignorada (tipo=%s)%n", mensagem.getTipo());
                }
            }
        } catch (Exception e) {
            System.err.println("[ThreadServidor Saída] Erro ao processar ligação: " + e.getMessage());
        }
    }

    /** Para o servidor de forma controlada, desbloqueando o accept(). */
    public void pararServidor() {
        ativo = false;
        try (Socket s = new Socket("localhost", portaServidor)) {
            // abre e fecha para desbloquear o accept()
        } catch (Exception ignored) {}
    }
}
