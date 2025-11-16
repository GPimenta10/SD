package Cruzamentos;

import Rede.Cliente;
import Rede.Mensagem;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe cliente específica de um cruzamento.
 * Responsável por enviar mensagens (ex: veículos) a outro cruzamento.
 *
 * CORREÇÃO: Agora inclui a origem na mensagem
 */
public class ThreadCliente extends Thread {

    private final String nomeCruzamentoOrigem;
    private final String nomeCruzamentoDestino;
    private final Cliente clienteGenerico;
    private final Gson gson = new Gson();

    private volatile boolean ativo = true;

    /**
     * Construtor da ThreadCliente.
     */
    public ThreadCliente(String nomeCruzamentoOrigem, String nomeCruzamentoDestino,
                         String ipDestino, int portaDestino) {
        super("Cliente-" + nomeCruzamentoOrigem + "->" + nomeCruzamentoDestino);
        this.nomeCruzamentoOrigem = nomeCruzamentoOrigem;
        this.nomeCruzamentoDestino = nomeCruzamentoDestino;
        this.clienteGenerico = new Cliente(ipDestino, portaDestino);
    }

    @Override
    public void run() {
        System.out.printf("[ThreadCliente %s->%s] Inicializado.%n",
                nomeCruzamentoOrigem, nomeCruzamentoDestino);

        while (ativo) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.printf("[ThreadCliente %s->%s] Encerrado.%n",
                nomeCruzamentoOrigem, nomeCruzamentoDestino);
    }

    /**
     * Envia um veículo ao cruzamento destino.
     *
     * @param veiculo Veículo a enviar
     * @param origem Nome do nó de origem (importante para o destino saber em qual fila colocar)
     */
    public void enviarVeiculo(Veiculo veiculo, String origem) {
        try {
            Map<String, Object> conteudo = new HashMap<>();
            conteudo.put("veiculo", veiculo);
            conteudo.put("origem", origem);  // NOVO: inclui a origem

            Mensagem mensagem = new Mensagem(
                    "VEICULO",
                    origem,  // origem correta
                    nomeCruzamentoDestino,
                    conteudo
            );

            clienteGenerico.enviarMensagem(mensagem);

            System.out.printf("[ThreadCliente %s->%s] Veículo enviado: %s (origem: %s)%n",
                    nomeCruzamentoOrigem, nomeCruzamentoDestino, veiculo.getId(), origem);
        } catch (Exception e) {
            System.err.printf("[ThreadCliente %s->%s] Erro ao enviar veículo: %s%n",
                    nomeCruzamentoOrigem, nomeCruzamentoDestino, e.getMessage());
        }
    }

    /**
     * Encerra o cliente de forma segura.
     */
    public void parar() {
        ativo = false;
        interrupt();
    }
}