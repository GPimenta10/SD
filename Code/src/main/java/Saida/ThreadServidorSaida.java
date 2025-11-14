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
 * Recebe mensagens JSON do tipo "VEICULO" enviadas pelos cruzamentos finais.
 * ATUALIZADO: Logs detalhados para debug
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
        System.out.printf("[ThreadServidor Saída] 🎧 A escutar na porta %d...%n", portaServidor);

        try (ServerSocket serverSocket = new ServerSocket(portaServidor)) {
            while (ativo) {
                Socket socket = serverSocket.accept();
                System.out.printf("[ThreadServidor Saída] 🔗 Nova conexão recebida de %s%n",
                        socket.getRemoteSocketAddress());
                new Thread(() -> tratarLigacao(socket)).start();
            }
        } catch (Exception e) {
            if (ativo) {
                System.err.println("[ThreadServidor Saída] ❌ Erro no servidor: " + e.getMessage());
                e.printStackTrace();
            } else {
                System.out.println("[ThreadServidor Saída] ✓ Servidor encerrado.");
            }
        }
    }

    private void tratarLigacao(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            String linha;
            while ((linha = in.readLine()) != null) {
                System.out.printf("[ThreadServidor Saída] 📨 Mensagem recebida: %s%n",
                        linha.substring(0, Math.min(100, linha.length())) + "...");

                Mensagem mensagem = Mensagem.fromJson(linha);

                System.out.printf("[ThreadServidor Saída]    Tipo: %s, Origem: %s%n",
                        mensagem.getTipo(), mensagem.getOrigem());

                if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
                    Object objVeiculo = mensagem.getConteudo().get("veiculo");

                    if (objVeiculo == null) {
                        System.err.println("[ThreadServidor Saída] ❌ ERRO: Campo 'veiculo' não encontrado!");
                        System.err.printf("[ThreadServidor Saída] Conteúdo: %s%n", mensagem.getConteudo());
                        continue;
                    }

                    Veiculo veiculo = gson.fromJson(gson.toJson(objVeiculo), Veiculo.class);

                    System.out.printf("[ThreadServidor Saída] ✅ Veículo recebido: %s (%s) de %s%n",
                            veiculo.getId(), veiculo.getTipo(), mensagem.getOrigem());

                    // Regista a chegada do veículo na saída
                    saida.registarVeiculo(veiculo);

                } else {
                    System.out.printf("[ThreadServidor Saída] ⚠️ Mensagem ignorada (tipo=%s)%n",
                            mensagem.getTipo());
                }
            }
        } catch (Exception e) {
            System.err.println("[ThreadServidor Saída] ❌ Erro ao processar ligação: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Para o servidor de forma controlada */
    public void pararServidor() {
        ativo = false;
        try (Socket s = new Socket("localhost", portaServidor)) {
            // abre e fecha para desbloquear o accept()
        } catch (Exception ignored) {}
    }
}