package PontosEntrada;

import java.util.*;

/**
 * Versão SIMPLIFICADA para teste
 * TODOS os veículos de E3 vão direto para S via Cr3
 */
public class Caminhos {

    /**
     * Para teste: retorna sempre o caminho mais curto
     */
    public static List<String> gerarCaminho(PontoEntrada entrada) {

        System.out.printf("[Caminhos] Gerando caminho para %s...%n", entrada);

        switch (entrada) {
            case E3:
                // TESTE: Apenas o caminho direto E3 → Cr3 → S
                List<String> caminho = Arrays.asList("Cr3", "S");
                System.out.printf("[Caminhos] Caminho gerado: %s%n", caminho);
                return caminho;

            case E1:
                return Arrays.asList("Cr1", "Cr4", "Cr5", "S");

            case E2:
                return Arrays.asList("Cr2", "Cr5", "S");

            default:
                System.err.println("[Caminhos] ERRO: Entrada desconhecida: " + entrada);
                return Collections.singletonList("S");
        }
    }

    /**
     * Retorna todos os caminhos possíveis (para análise futura)
     */
    public static List<List<String>> getTodosCaminhos(PontoEntrada entrada) {
        List<List<String>> caminhos = new ArrayList<>();

        switch (entrada) {
            case E1:
                caminhos.add(Arrays.asList("Cr1", "Cr4", "Cr5", "S"));
                caminhos.add(Arrays.asList("Cr1", "Cr2", "Cr5", "S"));
                caminhos.add(Arrays.asList("Cr1", "Cr2", "Cr3", "S"));
                break;
            case E2:
                caminhos.add(Arrays.asList("Cr2", "Cr5", "S"));
                caminhos.add(Arrays.asList("Cr2", "Cr3", "S"));
                caminhos.add(Arrays.asList("Cr2", "Cr1", "Cr4", "Cr5", "S"));
                break;
            case E3:
                caminhos.add(Arrays.asList("Cr3", "S"));
                caminhos.add(Arrays.asList("Cr3", "Cr2", "Cr5", "S"));
                caminhos.add(Arrays.asList("Cr3", "Cr2", "Cr1", "Cr4", "Cr5", "S"));
                break;
        }

        return caminhos;
    }
}