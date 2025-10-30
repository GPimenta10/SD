package EntradaVeiculos;

import OutrasClasses.ComunicadorSocket;
import Veiculo.Veiculo;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;

public abstract class EntradaBase {
    protected final String nome;
    protected final String hostCruzamento;
    protected final int portaCruzamento;
    protected final List<String> percurso;

    public EntradaBase(String nome, String host, int porta, List<String> percurso) {
        this.nome = nome;
        this.hostCruzamento = host;
        this.portaCruzamento = porta;
        this.percurso = percurso;
    }

    public void enviarVeiculo(Veiculo v) {
        try (Socket socket = new Socket(hostCruzamento, portaCruzamento);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            v.setTimestampEntradaFila(System.currentTimeMillis());
            out.writeObject(v);
            out.flush();

            log("→ Enviado: " + v.getId() + " (" + v.getTipo() + ")");
            ComunicadorSocket.enviarParaDashboard(
                    "[" + nome + "] Enviado: " + v.getId() + " (" + v.getTipo() + ")",
                    "127.0.0.1", 9000
            );

        } catch (IOException e) {
            System.err.println("[" + nome + "] Erro ao enviar veículo: " + e.getMessage());
        }
    }

    protected void log(String mensagem) {
        System.out.println("[" + nome + "] " + mensagem);
    }
}
