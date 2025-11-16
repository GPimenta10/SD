package Saida;

import Dashboard.TipoLog;
import Rede.Mensagem;
import Utils.EnviarLogs;
import Veiculo.Veiculo;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP da Saída.
 * Recebe mensagens JSON do tipo "VEICULO" enviadas pelos cruzamentos finais.
 * Apenas eventos importantes são enviados ao Dashboard.
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
       EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor da Saída a escutar na porta " + portaServidor);

        try (ServerSocket serverSocket = new ServerSocket(portaServidor)) {
            while (ativo) {
                Socket socket = serverSocket.accept();

                // Debug comentado:
                // System.out.println("[ThreadServidorSaida] Nova conexão: " + socket.getRemoteSocketAddress());

                new Thread(() -> tratarLigacao(socket)).start();
            }
        } catch (Exception e) {
            if (ativo) {
               EnviarLogs.enviar(TipoLog.ERRO, "Erro no servidor da Saída: " + e.getMessage());
            } else {
               EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor da Saída encerrado.");
            }
        }
    }

    private void tratarLigacao(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String linha;
            while ((linha = in.readLine()) != null) {

                // Debug comentado:
                // System.out.println("[ThreadServidorSaida] Mensagem recebida: " + linha);

                Mensagem mensagem = Mensagem.fromJson(linha);

                // Debug comentado:
                // System.out.println("[ThreadServidorSaida] Tipo: " + mensagem.getTipo() + ", Origem: " + mensagem.getOrigem());

                if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
                    Object objVeiculo = mensagem.getConteudo().get("veiculo");

                    if (objVeiculo == null) {
                       EnviarLogs.enviar(TipoLog.AVISO, "Mensagem de saída inválida: campo 'veiculo' ausente.");
                        continue;
                    }

                    Veiculo veiculo = gson.fromJson(gson.toJson(objVeiculo), Veiculo.class);

                    // ESTE É O EVENTO IMPORTANTE → vai para o Dashboard
                   EnviarLogs.enviar(TipoLog.VEICULO, "Veículo " + veiculo.getId() + " (" + veiculo.getTipo() + ") saiu do sistema via " + mensagem.getOrigem());

                    // Processa saída internamente
                    saida.registarVeiculo(veiculo);

                } /*else {
                    Debug comentado:
                    System.out.println("[ThreadServidorSaida] Mensagem ignorada (tipo=" + mensagem.getTipo() + ")");
                }*/
            }
        } catch (Exception e) {
           EnviarLogs.enviar(TipoLog.ERRO,"Erro ao processar ligação na Saída: " + e.getMessage());
        }
    }

    /**
     * Para o servidor de forma controlada
     */
    public void pararServidor() {
        ativo = false;
        try (Socket s = new Socket("localhost", portaServidor)) {} catch (Exception ignored) {}
    }
}
