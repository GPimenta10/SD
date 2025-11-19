package Rede;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class Cliente {
    private String endereco;
    private int porta;

    public Cliente(String endereco, int porta) {
        this.endereco = endereco;
        this.porta = porta;
    }

    public void enviarMensagem(Mensagem msg) {
        try (Socket socket = new Socket(endereco, porta);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String json = msg.toJson();
            out.println(json);
            System.out.println("[Cliente] Enviada: " + json);

        } catch (IOException e) {
            System.err.println("[Cliente] Erro ao enviar mensagem: " + e.getMessage());
        }
    }
}
