package Cruzamentos;

import Rede.Cliente;
import Rede.Mensagem;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente TCP utilizado por um cruzamento para enviar veículos ao próximo cruzamento ou saída
 * no percurso do veículo. Cada instância representa uma ligação lógica
 * CruzamentoOrigem -> CruzamentoDestino/Saida.
 */
public class ClienteCruzamento extends Thread {

    private final String nomeCruzamentoOrigem;
    private final String nomeCruzamentoDestino;
    private final Cliente clienteGenerico;   // Cliente TCP reutilizado para enviar mensagens
    private final Gson gson = new Gson();

    private volatile boolean ativo = true;

    /**
     * Construtor da classe
     *
     * @param nomeCruzamentoOrigem  Identificação do cruzamento de onde a mensagem sai
     * @param nomeCruzamentoDestino Identificação do cruzamento para onde a mensagem vai
     * @param ipDestino             Endereço IP do servidor destino
     * @param portaDestino          Porta TCP do servidor destino
     */
    public ClienteCruzamento(String nomeCruzamentoOrigem, String nomeCruzamentoDestino, String ipDestino, int portaDestino) {
        super("Cliente-" + nomeCruzamentoOrigem + "->" + nomeCruzamentoDestino);

        this.nomeCruzamentoOrigem = nomeCruzamentoOrigem;
        this.nomeCruzamentoDestino = nomeCruzamentoDestino;
        this.clienteGenerico = new Cliente(ipDestino, portaDestino);
    }

    /**
     * Execução base da thread
     */
    @Override
    public void run() {
        // Debug opcional:
        /*
        System.out.printf("[ClienteCruzamento %s->%s] Inicializado.%n",
                nomeCruzamentoOrigem, nomeCruzamentoDestino);
        */

        while (ativo) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        // Debug opcional:
        /*
        System.out.printf("[ClienteCruzamento %s->%s] Encerrado.%n",
                nomeCruzamentoOrigem, nomeCruzamentoDestino);
        */
    }

    /**
     * Envia um veículo para o cruzamento destino.
     * Inclui a origem na mensagem para permitir ao destino colocá-lo na fila correta.
     *
     * @param veiculo Veículo a transmitir
     * @param origem  Cruzamento por onde o veículo entrou antes de chegar aqui
     */
    public void enviarVeiculo(Veiculo veiculo, String origem) {
        try {
            Map<String, Object> conteudo = new HashMap<>();
            conteudo.put("veiculo", veiculo);
            conteudo.put("origem", origem);

            Mensagem mensagem = new Mensagem(
                    "VEICULO",
                    origem,
                    nomeCruzamentoDestino,
                    conteudo
            );

            clienteGenerico.enviarMensagem(mensagem);

            // Debug opcional:
            /*
            System.out.printf("[ClienteCruzamento %s->%s] Veículo enviado: %s (origem: %s)%n",
                    nomeCruzamentoOrigem, nomeCruzamentoDestino, veiculo.getId(), origem);
            */

        } catch (Exception e) {
            // Erro técnico — não enviar para Dashboard para evitar spam
            /*
            System.err.printf("[ClienteCruzamento %s->%s] Erro ao enviar veículo: %s%n",
                    nomeCruzamentoOrigem, nomeCruzamentoDestino, e.getMessage());
            */
        }
    }

    /**
     * Encerra a thread de forma segura.
     */
    public void parar() {
        ativo = false;
        interrupt();
    }
}
