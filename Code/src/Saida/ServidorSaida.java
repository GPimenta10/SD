/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Saida;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;
import Rede.Mensagem;
import Rede.Servidor;
import Veiculo.Veiculo;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 *
 */
public class ServidorSaida extends Servidor {
    private final Saida saida;
    private final Gson gson = new Gson();

    /**
     * Construtor da classe
     *
     * @param porta
     * @param saida
     */
    public ServidorSaida(String ip, int porta, Saida saida) {
        super(ip, porta, "ServidorSaida");

        if (saida == null) {
            throw new IllegalArgumentException("Instância de Saida não pode ser null");
        }

        this.saida = saida;
        setDaemon(true);
    }

    /**
     *
     */
    @Override
    protected void onInicio() {
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Servidor da Saída a escutar em " + ip + ":" + porta);
    }

    /**
     *
     *
     * @param linha Linha JSON recebida
     * @param leitor BufferedReader para ler mais dados se necessário
     * @param escritor PrintWriter para enviar respostas
     * @param socket Socket da conexão (para obter informações do cliente)
     * @throws IOException
     */
    @Override
    protected void tratarMensagem(String linha, BufferedReader leitor,
                                  PrintWriter escritor, Socket socket) throws IOException {
        Mensagem mensagem = Mensagem.fromJson(linha);

        if ("VEICULO".equalsIgnoreCase(mensagem.tipo())) {
            Object objVeiculo = mensagem.conteudo().get("veiculo");

            if (objVeiculo == null) {
                LogClienteDashboard.enviar(TipoLog.AVISO, "Mensagem de saída inválida: campo 'veiculo' ausente.");
                return;
            }

            Veiculo veiculo = gson.fromJson(gson.toJson(objVeiculo), Veiculo.class);

            LogClienteDashboard.enviar(
                    TipoLog.VEICULO,
                    "Veículo " + veiculo.getId() + " (" + veiculo.getTipo() +
                            ") saiu do sistema via " + mensagem.origem()
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
        LogClienteDashboard.enviar(TipoLog.ERRO, "Erro ao processar ligação na Saída: " + e.getMessage());
    }

    /**
     *
     *
     * @param e
     */
    @Override
    protected void onErroInicializacao(IOException e) {
        LogClienteDashboard.enviar(TipoLog.ERRO, "Erro no servidor da Saída: " + e.getMessage());
    }

    /**
     *
     */
    @Override
    protected void onEncerramento() {
        if (!ativo) {
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Servidor da Saída encerrado.");
        }
    }
}