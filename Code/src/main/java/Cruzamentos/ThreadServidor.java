package Cruzamentos;

import Rede.Mensagem;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Thread específica de servidor para um cruzamento.
 * Escuta numa porta TCP e processa mensagens recebidas de outros cruzamentos.
 *
 * CORREÇÃO: Agora extrai e passa a origem do veículo ao cruzamento
 */
public class ThreadServidor extends Thread {

    private final int portaServidor;
    private final Cruzamento cruzamento;
    private volatile boolean ativo = true;

    private final Gson gson = new Gson();

    public ThreadServidor(int portaServidor, Cruzamento cruzamento) {
        super("Servidor-" + cruzamento.getNomeCruzamento());
        this.portaServidor = portaServidor;
        this.cruzamento = cruzamento;
    }

    @Override
    public void run() {
        System.out.printf("[ThreadServidor %s] A escutar na porta %d...%n",
                cruzamento.getNomeCruzamento(), portaServidor);

        try (ServerSocket serverSocket = new ServerSocket(portaServidor)) {

            while (ativo) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> tratarLigacao(socket)).start();
                } catch (IOException e) {
                    if (ativo) {
                        System.err.printf("[ThreadServidor %s] Erro ao aceitar ligação: %s%n",
                                cruzamento.getNomeCruzamento(), e.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.printf("[ThreadServidor %s] Erro ao criar o servidor: %s%n",
                    cruzamento.getNomeCruzamento(), e.getMessage());
        }
    }

    /**
     * Trata uma ligação recebida – interpreta a mensagem e executa a ação correspondente.
     */
    private void tratarLigacao(Socket socket) {
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(socket.getInputStream()))) {

            String linhaJson;
            while ((linhaJson = leitor.readLine()) != null) {
                Mensagem mensagem = Mensagem.fromJson(linhaJson);
                System.out.printf("[ThreadServidor %s] Mensagem recebida: tipo=%s, origem=%s%n",
                        cruzamento.getNomeCruzamento(), mensagem.getTipo(), mensagem.getOrigem());

                if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
                    // Extrai o veículo
                    Object conteudoObj = mensagem.getConteudo().get("veiculo");
                    if (conteudoObj != null) {
                        Veiculo veiculo = gson.fromJson(gson.toJson(conteudoObj), Veiculo.class);

                        // NOVO: Extrai a origem (de onde o veículo veio)
                        String origem = mensagem.getOrigem();

                        // Se a origem estiver também no conteúdo, usa essa (mais específico)
                        Object origemObj = mensagem.getConteudo().get("origem");
                        if (origemObj != null) {
                            origem = origemObj.toString();
                        }

                        // Passa ao cruzamento com a informação de origem
                        cruzamento.receberVeiculo(veiculo, origem);
                    }
                } else {
                    System.out.printf("[ThreadServidor %s] Tipo de mensagem desconhecido: %s%n",
                            cruzamento.getNomeCruzamento(), mensagem.getTipo());
                }
            }

        } catch (IOException e) {
            System.err.printf("[ThreadServidor %s] Erro ao ler mensagem: %s%n",
                    cruzamento.getNomeCruzamento(), e.getMessage());
        }
    }

    /**
     * Interrompe o servidor de forma segura.
     */
    public void pararServidor() {
        ativo = false;
        interrupt();
        System.out.printf("[ThreadServidor %s] Servidor encerrado.%n", cruzamento.getNomeCruzamento());
    }
}