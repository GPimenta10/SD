package Cruzamentos;

import Dashboard.Logs.TipoLog;
import Rede.Mensagem;
import Utils.EnviarLogs;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Servidor de um cruzamento realizado por uma Thread.
 * Escuta numa porta TCP e processa mensagens recebidas.
 */
public class ServidorCruzamento extends Thread {

    private final int portaServidor;
    private final Cruzamento cruzamento;
    private volatile boolean ativo = true;

    private final Gson gson = new Gson();

    /**
     * Construtor da classe
     *
     * @param portaServidor Porta onde os clientes irão se conectar
     * @param cruzamento
     */
    public ServidorCruzamento(int portaServidor, Cruzamento cruzamento) {
        super("Servidor-" + cruzamento.getNomeCruzamento());
        this.portaServidor = portaServidor;
        this.cruzamento = cruzamento;
    }

    /**
     * Execução base da thread
     */
    @Override
    public void run() {
        EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor do cruzamento " + cruzamento.getNomeCruzamento() +
                " a escutar na porta " + portaServidor + ".");

        try (ServerSocket serverSocket = new ServerSocket(portaServidor)) {
            while (ativo) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> tratarLigacao(socket)).start();
                } catch (IOException e) {
                    if (ativo) {
                        // Debug silencioso (não interessa ao dashboard)
                        /*
                        System.err.printf("[ServidorCruzamento %s] Erro ao aceitar ligação: %s%n",
                               cruzamento.getNomeCruzamento(), e.getMessage());
                        */
                    }
                }
            }
        } catch (IOException e) {
            EnviarLogs.enviar(TipoLog.ERRO, "Erro ao iniciar servidor do cruzamento " +
                    cruzamento.getNomeCruzamento() + ": " + e.getMessage()
            );
        }
    }

    /**
     * Trata uma ligação recebida – interpreta a mensagem e executa a ação correspondente.
     *
     * @param socket
     */
    private void tratarLigacao(Socket socket) {
        try (BufferedReader leitor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true)) {

            String linhaJson;
            while ((linhaJson = leitor.readLine()) != null) {

                Mensagem mensagem = Mensagem.fromJson(linhaJson);

                // Debug (não enviar para Dashboard)
                /*
                System.out.printf(
                        "[ServidorCruzamento %s] Mensagem recebida: tipo=%s, origem=%s%n",
                        cruzamento.getNomeCruzamento(),
                        mensagem.getTipo(),
                        mensagem.getOrigem()
                );
                */

                if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
                    Object conteudoObj = mensagem.getConteudo().get("veiculo");

                    if (conteudoObj != null) {
                        Veiculo veiculo = gson.fromJson(gson.toJson(conteudoObj), Veiculo.class);

                        // Extrair origem
                        String origem = mensagem.getOrigem();
                        Object origemObj = mensagem.getConteudo().get("origem");

                        if (origemObj != null) {
                            origem = origemObj.toString();
                        }
                        cruzamento.receberVeiculo(veiculo, origem);
                    }
                }
                else if ("ESTADO_FILA".equalsIgnoreCase(mensagem.getTipo())) {
                    // Obter o identificador da fila solicitada
                    Object origemFilaObj = mensagem.getConteudo().get("origemFila");

                    if (origemFilaObj != null) {
                        String origemFila = origemFilaObj.toString();

                        // Consultar o tamanho da fila no cruzamento
                        int tamanhoFila = cruzamento.obterTamanhoFila(origemFila);

                        // Construir mensagem de resposta
                        Map<String, Object> conteudoResposta = new HashMap<>();
                        conteudoResposta.put("tamanhoFila", tamanhoFila);
                        conteudoResposta.put("origemFila", origemFila);

                        Mensagem resposta = new Mensagem(
                                "RESPOSTA_ESTADO_FILA",
                                cruzamento.getNomeCruzamento(),
                                mensagem.getOrigem(),
                                conteudoResposta
                        );

                        // Enviar resposta
                        escritor.println(resposta.toJson());
                    }
                }
            }
        } catch (IOException e) {
            EnviarLogs.enviar(TipoLog.ERRO, "Erro ao ler mensagem no cruzamento " +
                    cruzamento.getNomeCruzamento() + ": " + e.getMessage());
        }
    }

    /**
     * Interrompe o servidor de forma segura.
     */
    public void pararServidor() {
        ativo = false;
        interrupt();
        EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor do cruzamento " + cruzamento.getNomeCruzamento() + " encerrado.");
    }
}