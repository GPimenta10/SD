/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

import java.util.Map;

/**
 *
 *
 */
public class EstatisticaSemaforo {
    private final int id;
    private final String estado;
    private final int tamanhoFila;
    private final String origem;
    private final String destino;

    /**
     * Construtor da classe
     *
     * @param id
     * @param estado
     * @param tamanhoFila
     * @param origem
     * @param destino
     */
    public EstatisticaSemaforo(int id, String estado, int tamanhoFila, String origem, String destino) {
        this.id = id;
        this.estado = estado;
        this.tamanhoFila = tamanhoFila;
        this.origem = origem;
        this.destino = destino;
    }

    /**
     *
     *
     * @return
     */
    public int getId() {
        return id;
    }

    /**
     *
     *
     * @return
     */
    public String getEstado() {
        return estado;
    }

    /**
     *
     *
     * @return
     */
    public int getTamanhoFila() {
        return tamanhoFila;
    }

    /**
     *
     *
     * @return
     */
    public String getOrigem() {
        return origem;
    }

    /**
     *
     *
     * @return
     */
    public String getDestino() {
        return destino;
    }

    /**
     *
     *
     * @return
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "id", id,
                "estado", estado,
                "tamanhoFila", tamanhoFila,
                "origem", origem,
                "destino", destino
        );
    }
}