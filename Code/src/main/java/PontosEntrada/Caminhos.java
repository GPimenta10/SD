package PontosEntrada;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classe responsável por definir caminhos possíveis para cada ponto de entrada
 * de acordo com as probabilidades indicadas no enunciado.
 */
public class Caminhos {

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    /**
     * Retorna um caminho aleatório de acordo com o ponto de entrada.
     * @param entrada ponto de entrada (E1, E2 ou E3)
     * @return lista de nós representando o percurso completo até S
     */
    public static List<String> gerarCaminho(PontoEntrada entrada) {
        double p = random.nextDouble();

        switch (entrada) {
            /*case E1 -> {
                if (p < 0.34) {
                    return Arrays.asList("Cr1", "Cr4", "Cr5", "S");
                } else if (p < 0.67) {
                    return Arrays.asList("Cr1", "Cr2", "Cr5", "S");
                } else {
                    return Arrays.asList("Cr1", "Cr2", "Cr3", "S");
                }
            }
            case E2 -> {
                if (p < 0.34) {
                    return Arrays.asList("Cr2", "Cr5", "S");
                } else if (p < 0.67) {
                    return Arrays.asList("Cr2", "Cr3", "S");
                } else {
                    return Arrays.asList("Cr2", "Cr1", "Cr4", "Cr5", "S");
                }
            }*/
            case E3 -> {
                return Arrays.asList("Cr3", "S");
                /*if (p < 0.34) {
                    return Arrays.asList("Cr3", "S");
                } else if (p < 0.67) {
                    return Arrays.asList("Cr3", "Cr2", "Cr5", "S");
                } else {
                    return Arrays.asList("Cr3", "Cr2", "Cr1", "Cr4", "Cr5", "S");
                }*/
            }
            default -> {
                return Collections.singletonList("S");
            }
        }
    }
}
