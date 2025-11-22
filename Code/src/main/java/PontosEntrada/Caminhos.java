package PontosEntrada;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classe responsável por gerar caminhos para veículos com base no ponto de entrada
 * e no cenário selecionado.
 */
public class Caminhos {

    /**
     * Gera um caminho para o veículo com base na entrada e no cenário.
     *
     * @param entrada Ponto de entrada do veículo
     * @param cenario Tipo de cenário (ALEATORIO ou CAMINHO_CURTO)
     * @return Lista ordenada de nós que o veículo deve percorrer
     */
    public static List<String> gerarCaminho(PontoEntrada entrada, TipoCenario cenario) {
        return switch (cenario) {
            case ALEATORIO -> gerarCaminhoAleatorio(entrada);
            case CAMINHO_CURTO -> gerarCaminhoCurto(entrada);
        };
    }

    /**
     * Gera um caminho aleatório com distribuição probabilística.
     * Mantém a lógica original do sistema.
     *
     * @param entrada Ponto de entrada do veículo
     * @return Caminho aleatório
     */
    private static List<String> gerarCaminhoAleatorio(PontoEntrada entrada) {
        double p = ThreadLocalRandom.current().nextDouble();

        return switch (entrada) {
            case E1 -> {
                if (p < 0.34) {
                    yield Arrays.asList("Cr1", "Cr4", "Cr5", "S");
                } else if (p < 0.67) {
                    yield Arrays.asList("Cr1", "Cr2", "Cr5", "S");
                } else {
                    yield Arrays.asList("Cr1", "Cr2", "Cr3", "S");
                }
            }
            case E2 -> {
                if (p < 0.34) {
                    yield Arrays.asList("Cr2", "Cr5", "S");
                } else if (p < 0.67) {
                    yield Arrays.asList("Cr2", "Cr3", "S");
                } else {
                    yield Arrays.asList("Cr2", "Cr1", "Cr4", "Cr5", "S");
                }
            }
            case E3 -> {
                if (p < 0.34) {
                    yield Arrays.asList("Cr3", "S");
                } else if (p < 0.67) {
                    yield Arrays.asList("Cr3", "Cr2", "Cr5", "S");
                } else {
                    yield Arrays.asList("Cr3", "Cr2", "Cr1", "Cr4", "Cr5", "S");
                }
            }
        };
    }

    /**
     * Gera o caminho mais curto (hardcoded) para cada entrada.
     *
     * E1: E1 -> Cr1 -> Cr4 -> Cr5 -> S (3 cruzamentos)
     * E2: E2 -> Cr2 -> Cr3 -> S ou Cr2 -> Cr5 -> S (50/50, 2 cruzamentos cada)
     * E3: E3 -> Cr3 -> S (1 cruzamento - o mais curto)
     *
     * @param entrada Ponto de entrada do veículo
     * @return Caminho mais curto
     */
    private static List<String> gerarCaminhoCurto(PontoEntrada entrada) {
        return switch (entrada) {
            case E1 -> Arrays.asList("Cr1", "Cr4", "Cr5", "S");
            case E2 -> {
                // Divisão 50/50 para balancear o tráfego
                if (ThreadLocalRandom.current().nextBoolean()) {
                    yield Arrays.asList("Cr2", "Cr3", "S");
                } else {
                    yield Arrays.asList("Cr2", "Cr5", "S");
                }
            }
            case E3 -> Arrays.asList("Cr3", "S");
        };
    }
}