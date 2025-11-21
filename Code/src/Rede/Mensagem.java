/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Rede;

import com.google.gson.Gson;
import java.util.Map;

/**
 * 
 */
public record Mensagem(String tipo, String origem, String destino, Map<String, Object> conteudo, long timestamp) {

    /**
     * Construtor
     * 
     * @param tipo
     * @param origem
     * @param destino
     * @param conteudo 
     */
    public Mensagem(String tipo, String origem, String destino, Map<String, Object> conteudo) {
        this(tipo, origem, destino, conteudo, System.currentTimeMillis());
    }

    /**
     * 
     * 
     * @return 
     */
    public String toJson() {
        return new Gson().toJson(this);
    }

    /**
     * 
     * 
     * @param json
     * @return 
     */
    public static Mensagem fromJson(String json) {
        return new Gson().fromJson(json, Mensagem.class);
    }
}
