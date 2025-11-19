package Saida;

import Rede.Servidor;
import Rede.Mensagem;
import Dashboard.Logs.TipoLog;
import Utils.EnviarLogs;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ServidorSaida extends Servidor {

    private final Saida saida;
    private final Gson gson = new Gson();

    public ServidorSaida(int porta, Saida saida) {
        super(porta, "ServidorSaida");

        if (saida == null) {
            throw new IllegalArgumentException("Instância de Saida não pode ser null");
        }

        this.saida = saida;
        setDaemon(true);
    }

    @Override
    protected void onInicio() {
        EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor da Saída a escutar na porta " + porta);
    }

    @Override
    protected void tratarMensagem(String linha, BufferedReader leitor,
                                  PrintWriter escritor, Socket socket) throws IOException {
        Mensagem mensagem = Mensagem.fromJson(linha);

        if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
            Object objVeiculo = mensagem.getConteudo().get("veiculo");

            if (objVeiculo == null) {
                EnviarLogs.enviar(TipoLog.AVISO, "Mensagem de saída inválida: campo 'veiculo' ausente.");
                return;
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

    /**
     *
     *
     * @param e
     * @param socket
     */
    @Override
    protected void onErroProcessamento(IOException e, Socket socket) {
        EnviarLogs.enviar(TipoLog.ERRO, "Erro ao processar ligação na Saída: " + e.getMessage());
    }

    /**
     *
     *
     * @param e
     */
    @Override
    protected void onErroInicializacao(IOException e) {
        EnviarLogs.enviar(TipoLog.ERRO, "Erro no servidor da Saída: " + e.getMessage());
    }

    /**
     *
     */
    @Override
    protected void onEncerramento() {
        if (!ativo) {
            EnviarLogs.enviar(TipoLog.SISTEMA, "Servidor da Saída encerrado.");
        }
    }
}