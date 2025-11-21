/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cruzamentos;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;
import Veiculo.Veiculo;
import Rede.Mensagem;
import Rede.Servidor;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;


/**
 *
 */
public class ServidorCruzamento extends Servidor {
    private final Cruzamento cruzamento;
    private final Gson gson = new Gson();

    public ServidorCruzamento(String ip, int porta, Cruzamento cruzamento) {
        super(ip, porta, "Servidor-" + cruzamento.getNomeCruzamento());
        this.cruzamento = cruzamento;
    }

    @Override
    protected void onInicio() {
        LogClienteDashboard.enviar(TipoLog.SISTEMA,
                "Servidor do cruzamento " + cruzamento.getNomeCruzamento() +
                        " a escutar em " + ip + ":" + porta + ".");
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
    protected void tratarMensagem(String linha, BufferedReader leitor, PrintWriter escritor, Socket socket) throws IOException {
        Mensagem mensagem = Mensagem.fromJson(linha);

        if ("VEICULO".equalsIgnoreCase(mensagem.tipo())) {
            processarVeiculo(mensagem);
        }
    }

    /**
     *
     *
     * @param mensagem
     */
    private void processarVeiculo(Mensagem mensagem) {
        Object conteudoObj = mensagem.conteudo().get("veiculo");

        if (conteudoObj != null) {
            Veiculo veiculo = gson.fromJson(gson.toJson(conteudoObj), Veiculo.class);

            String origem = mensagem.origem();
            Object origemObj = mensagem.conteudo().get("origem");

            if (origemObj != null) {
                origem = origemObj.toString();
            }

            cruzamento.receberVeiculo(veiculo, origem);
        }
    }

    /**
     *
     * @param e
     * @param socket
     */
    @Override
    protected void onErroProcessamento(IOException e, Socket socket) {
        LogClienteDashboard.enviar(TipoLog.ERRO,
                "Erro ao ler mensagem no cruzamento " +
                        cruzamento.getNomeCruzamento() + ": " + e.getMessage());
    }

    /**
     *
     * @param e
     */
    @Override
    protected void onErroInicializacao(IOException e) {
        LogClienteDashboard.enviar(TipoLog.ERRO,
                "Erro ao iniciar servidor do cruzamento " +
                        cruzamento.getNomeCruzamento() + ": " + e.getMessage());
    }

    /**
     *
     */
    @Override
    protected void onEncerramento() {
        LogClienteDashboard.enviar(TipoLog.SISTEMA,
                "Servidor do cruzamento " + cruzamento.getNomeCruzamento() + " encerrado.");
    }
}
