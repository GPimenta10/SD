/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 *
 */
public class EstatisticaCruzamento {
    private final String nome;
    private final List<EstatisticaSemaforo> semaforos;

    /**
     *
     *
     * @param nome
     * @param semaforos
     */
    public EstatisticaCruzamento(String nome, List<EstatisticaSemaforo> semaforos) {
        this.nome = nome;
        this.semaforos = semaforos;
    }

    /**
     *
     *
     * @return
     */
    public String getNome() {
        return nome;
    }

    /**
     *
     *
     * @return
     */
    public List<EstatisticaSemaforo> getSemaforos() {
        return semaforos;
    }

    /**
     *
     *
     * @return
     */
    public Map<String, Object> toMap() {
        return Map.of(
                "cruzamento", nome,
                "semaforos", semaforos.stream()
                        .map(EstatisticaSemaforo::toMap)
                        .collect(Collectors.toList())
        );
    }
}