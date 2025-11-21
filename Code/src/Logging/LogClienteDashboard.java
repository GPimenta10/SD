/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Logging;

import Dashboard.Logs.TipoLog;

import com.google.gson.Gson;

/**
 * Utilitário para envio de logs para o Dashboard centralizado.
 *
 * Agora utiliza internamente o LogSender para evitar duplicação
 * de código e centralizar a lógica de envio TCP.
 */
public class LogClienteDashboard {

    private static final Gson gson = new Gson();
    private static String nomeProcesso = "Desconhecido";

    /**
     * Define o nome do processo que está a enviar logs.
     *
     * @param nome Nome identificador do processo
     */
    public static void definirNomeProcesso(String nome) {
        if (nome == null || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome do processo não pode ser null ou vazio");
        }
        nomeProcesso = nome.trim();
    }

    /**
     * Envia uma mensagem de log para o Dashboard.
     *
     * @param nivel Nível do log
     * @param mensagem Conteúdo textual do log
     */
    public static void enviar(TipoLog nivel, String mensagem) {
        if (nivel == null) {
            throw new IllegalArgumentException("Nível do log não pode ser null");
        }
        if (mensagem == null) {
            throw new IllegalArgumentException("Mensagem do log não pode ser null");
        }

        LogSender.enviar(
                "LOG",
                nomeProcesso,
                nivel.name(),
                mensagem
        );
    }
}
