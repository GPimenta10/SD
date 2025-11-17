package Rede;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Servidor implements Runnable {

    private int porta;
    private boolean ativo = true;

    public Servidor(int porta) {
        this.porta = porta;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (ativo) {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratarConexao(socket)).start();
            }
        } catch (IOException e) {
            System.err.println("[Servidor] Erro: " + e.getMessage());
        }
    }

    private void tratarConexao(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String json;
            while ((json = in.readLine()) != null) {
                Mensagem msg = Mensagem.fromJson(json);
                System.out.println("[Servidor] Recebida: " + msg);
            }
        } catch (IOException e) {
            System.err.println("[Servidor] Erro ao ler mensagem: " + e.getMessage());
        }
    }

    public void parar() {
        ativo = false;
    }
}
