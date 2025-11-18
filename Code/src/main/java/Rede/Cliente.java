package Rede;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {

    private String endereco; // IP ou hostname do servidor de destino
    private int porta;       // Porta do servidor

    public Cliente(String endereco, int porta) {
        this.endereco = endereco;
        this.porta = porta;
    }

    /**
     * Envia uma mensagem (objeto Mensagem) para o servidor indicado.
     */
    public void enviarMensagem(Mensagem msg) {
        try (Socket socket = new Socket(endereco, porta);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String json = msg.toJson();
            out.println(json);
        } catch (IOException e) {
            System.err.println("[Cliente] Erro ao enviar mensagem: " + e.getMessage());
        }
    }
}
