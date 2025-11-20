package Rede;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Classe base genérica para servidores TCP.
 * Implementa a lógica comum de aceitar conexões e delega o processamento
 * específico para as subclasses através do método abstrato tratarMensagem().
 */
public abstract class Servidor extends Thread {
    protected final String ip;
    protected final int porta;
    protected volatile boolean ativo = true;

    /**
     * Construtor base
     *
     * @param ip Endereço IP onde o servidor irá escutar
     * @param porta Porta TCP onde o servidor irá escutar
     * @param nomeThread Nome da thread (para identificação)
     */
    public Servidor(String ip, int porta, String nomeThread) {
        super(nomeThread);

        if (porta < 1 || porta > 65535) {
            throw new IllegalArgumentException("Porta inválida: deve estar entre 1 e 65535");
        }

        this.ip = (ip == null || ip.isEmpty()) ? "localhost" : ip;
        this.porta = porta;
    }

    /**
     * Execução principal do servidor.
     * Aceita conexões e cria threads para cada cliente.
     */
    @Override
    public void run() {
        onInicio();

        // Agora especificamos o IP (bindAddr) além da porta
        try (ServerSocket serverSocket = new ServerSocket(porta, 50, InetAddress.getByName(ip))) {
            while (ativo) {
                try {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> tratarConexao(socket)).start();
                } catch (IOException e) {
                    if (ativo) {
                        onErroAceitarConexao(e);
                    }
                }
            }
        } catch (IOException e) {
            onErroInicializacao(e);
        } finally {
            onEncerramento();
        }
    }

    /**
     * Trata uma conexão individual.
     * Lê mensagens linha a linha e delega o processamento.
     */
    private void tratarConexao(Socket socket) {
        try (
                BufferedReader leitor = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter escritor = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String linha;
            while ((linha = leitor.readLine()) != null) {
                tratarMensagem(linha, leitor, escritor, socket);
            }
        } catch (IOException e) {
            onErroProcessamento(e, socket);
        }
    }

    /**
     * Método abstrato que cada servidor específico deve implementar
     * para processar mensagens recebidas.
     *
     * @param linha Linha JSON recebida
     * @param leitor BufferedReader para ler mais dados se necessário
     * @param escritor PrintWriter para enviar respostas
     * @param socket Socket da conexão (para obter informações do cliente)
     */
    protected abstract void tratarMensagem(String linha, BufferedReader leitor, PrintWriter escritor, Socket socket) throws IOException;

    /**
     * Chamado quando o servidor inicia.
     * Sobrescreva para adicionar lógica de inicialização.
     */
    protected void onInicio() {}

    /**
     * Chamado quando ocorre erro ao aceitar conexão.
     */
    protected void onErroAceitarConexao(IOException e) {}

    /**
     * Chamado quando ocorre erro na inicialização do ServerSocket.
     */
    protected void onErroInicializacao(IOException e) {}

    /**
     * Chamado quando ocorre erro ao processar mensagem de um cliente.
     */
    protected void onErroProcessamento(IOException e, Socket socket) {}

    /**
     * Chamado quando o servidor é encerrado.
     */
    protected void onEncerramento() {}

    /**
     * Para o servidor de forma controlada.
     */
    public void pararServidor() {
        ativo = false;
        interrupt();

        try (Socket socket = new Socket(ip, porta)) {} catch (Exception ignored) {}
    }

    /**
     * Verifica se o servidor está ativo.
     */
    public boolean isAtivo() {
        return ativo;
    }

    /**
     * Obtém a porta do servidor.
     */
    public int getPorta() {
        return porta;
    }
}