/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cruzamentos;

import Veiculo.Veiculo;
import Rede.Mensagem;
import Rede.Cliente;

import java.util.HashMap;
import java.util.Map;

/**
 * Cliente TCP utilizado por um cruzamento para enviar veículos
 * ao próximo cruzamento ou à Saída.
 */
public class ClienteCruzamento extends Thread {

    private final String nomeCruzamentoDestino;
    private final Cliente clienteGenerico;
    private volatile boolean ativo = true;

    /**
     * Construtor da classe
     *
     * @param nomeCruzamentoDestino  Nome do cruzamento de destino
     * @param ipDestino              Endereço IP do destino
     * @param portaDestino           Porta TCP do destino
     */
    public ClienteCruzamento(String nomeCruzamentoDestino, String ipDestino, int portaDestino) {
        super("Cliente->" + nomeCruzamentoDestino);

        this.nomeCruzamentoDestino = nomeCruzamentoDestino;
        this.clienteGenerico = new Cliente(ipDestino, portaDestino);
    }

    @Override
    public void run() {
        while (ativo) {
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Envia um veículo para o cruzamento destino.
     *
     * @param veiculo Veículo a enviar
     * @param origem  Cruzamento de onde o veículo veio
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
        } catch (Exception ignored) {}
    }

    /**
     * Parar cliente
     */
    public void parar() {
        ativo = false;
        interrupt();
    }
}

