package Saida;

import Dashboard.Logs.TipoLog;
import Rede.Mensagem;
import Utils.EnviarLogs;
import Veiculo.Veiculo;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Servidor TCP responsável por receber veículos que saem do sistema.
 *
 * Este servidor escuta numa porta específica e processa mensagens JSON
 * do tipo "VEICULO" enviadas pelos cruzamentos finais quando um veículo
 * completa o seu percurso e sai do sistema de tráfego.
 *
 * O servidor executa numa thread separada (daemon) e cria uma thread
 * para cada conexão recebida, permitindo processar múltiplos veículos
 * simultaneamente.
 */
public class ServidorSaida extends Thread {

    private final int portaServidor;
    private final Saida saida;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    /**
     * Construtor da classe
     *
     * @param portaServidor Porta TCP onde o servidor irá escutar
     * @param saida Instância da Saída que irá registar os veículos
     * @throws IllegalArgumentException se a porta for inválida ou saida for null
     */
    public ServidorSaida(int portaServidor, Saida saida) {
        super("ServidorSaida");

        if (portaServidor < 1 || portaServidor > 65535) {
            throw new IllegalArgumentException("Porta inválida: deve estar entre 1 e 65535");
        }
        if (saida == null) {
            throw new IllegalArgumentException("Instância de Saida não pode ser null");
        }

        this.portaServidor = portaServidor;
        this.saida = saida;
        setDaemon(true);
    }

    /**
     * Executa o servidor
     *
     * Inicia o servidor TCP na porta configurada e aceita conexões de entrada.
     * Para cada conexão aceite, cria uma thread para processamento paralelo.
     */
    @Override
    public void run() {
        EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor da Saída a escutar na porta " + portaServidor);

        try (ServerSocket serverSocket = new ServerSocket(portaServidor)) {
            while (ativo) {
                Socket socket = serverSocket.accept();
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

    /**
     * Processa uma conexão TCP recebida.
     *
     * Lê mensagens JSON linha a linha do socket, deserializa-as como objetos
     * Mensagem e processa apenas as do tipo "VEICULO". Extrai o objeto veículo
     * da mensagem e regista-o na Saída.
     *
     * Mensagens inválidas ou sem o campo 'veiculo' são ignoradas com log de aviso.
     *
     * @param socket Socket da conexão estabelecida com o cliente
     */
    private void tratarLigacao(Socket socket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            String linha;
            while ((linha = in.readLine()) != null) {

                Mensagem mensagem = Mensagem.fromJson(linha);

                if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
                    Object objVeiculo = mensagem.getConteudo().get("veiculo");

                    if (objVeiculo == null) {
                        EnviarLogs.enviar(TipoLog.AVISO, "Mensagem de saída inválida: campo 'veiculo' ausente.");
                        continue;
                    }

                    Veiculo veiculo = gson.fromJson(gson.toJson(objVeiculo), Veiculo.class);

                    EnviarLogs.enviar(
                            TipoLog.VEICULO,
                            "Veículo " + veiculo.getId() + " (" + veiculo.getTipo() +
                                    ") saiu do sistema via " + mensagem.getOrigem()
                    );

                    saida.registarVeiculo(veiculo);
                }
            }
        } catch (Exception e) {
            EnviarLogs.enviar(TipoLog.ERRO, "Erro ao processar ligação na Saída: " + e.getMessage());
        }
    }

    /**
     * Para o servidor de forma controlada.
     */
    public void pararServidor() {
        ativo = false;
        try (Socket s = new Socket("localhost", portaServidor)) {} catch (Exception ignored) {}
    }
}