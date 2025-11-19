package Cruzamentos;

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
import java.util.HashMap;
import java.util.Map;

public class ServidorCruzamento extends Servidor {

    private final Cruzamento cruzamento;
    private final Gson gson = new Gson();

    public ServidorCruzamento(int porta, Cruzamento cruzamento) {
        super(porta, "Servidor-" + cruzamento.getNomeCruzamento());
        this.cruzamento = cruzamento;
    }

    @Override
    protected void onInicio() {
        EnviarLogs.enviar(TipoLog.SISTEMA,
                "Servidor do cruzamento " + cruzamento.getNomeCruzamento() +
                        " a escutar na porta " + porta + ".");
    }

    @Override
    protected void tratarMensagem(String linha, BufferedReader leitor,
                                  PrintWriter escritor, Socket socket) throws IOException {
        Mensagem mensagem = Mensagem.fromJson(linha);

        if ("VEICULO".equalsIgnoreCase(mensagem.getTipo())) {
            processarVeiculo(mensagem);
        }
        else if ("ESTADO_FILA".equalsIgnoreCase(mensagem.getTipo())) {
            processarEstadoFila(mensagem, escritor);
        }
    }

    private void processarVeiculo(Mensagem mensagem) {
        Object conteudoObj = mensagem.getConteudo().get("veiculo");

        if (conteudoObj != null) {
            Veiculo veiculo = gson.fromJson(gson.toJson(conteudoObj), Veiculo.class);

            String origem = mensagem.getOrigem();
            Object origemObj = mensagem.getConteudo().get("origem");

            if (origemObj != null) {
                origem = origemObj.toString();
            }

            cruzamento.receberVeiculo(veiculo, origem);
        }
    }

    private void processarEstadoFila(Mensagem mensagem, PrintWriter escritor) {
        Object origemFilaObj = mensagem.getConteudo().get("origemFila");

        if (origemFilaObj != null) {
            String origemFila = origemFilaObj.toString();
            int tamanhoFila = cruzamento.obterTamanhoFila(origemFila);

            Map<String, Object> conteudoResposta = new HashMap<>();
            conteudoResposta.put("tamanhoFila", tamanhoFila);
            conteudoResposta.put("origemFila", origemFila);

            Mensagem resposta = new Mensagem(
                    "RESPOSTA_ESTADO_FILA",
                    cruzamento.getNomeCruzamento(),
                    mensagem.getOrigem(),
                    conteudoResposta
            );

            escritor.println(resposta.toJson());
        }
    }

    @Override
    protected void onErroProcessamento(IOException e, Socket socket) {
        EnviarLogs.enviar(TipoLog.ERRO,
                "Erro ao ler mensagem no cruzamento " +
                        cruzamento.getNomeCruzamento() + ": " + e.getMessage());
    }

    @Override
    protected void onErroInicializacao(IOException e) {
        EnviarLogs.enviar(TipoLog.ERRO,
                "Erro ao iniciar servidor do cruzamento " +
                        cruzamento.getNomeCruzamento() + ": " + e.getMessage());
    }

    @Override
    protected void onEncerramento() {
        EnviarLogs.enviar(TipoLog.SISTEMA,
                "Servidor do cruzamento " + cruzamento.getNomeCruzamento() + " encerrado.");
    }
}