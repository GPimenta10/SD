package PontosEntrada;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Caminhos {

    private static final ThreadLocalRandom random = ThreadLocalRandom.current();

    public static List<String> gerarCaminho(PontoEntrada entrada) {
        double p = random.nextDouble(); // Aleatório entre 0.0 e 1.0

        switch (entrada) {
            case E1:
                if (p < 0.34) { // 34%
                    return Arrays.asList("Cr1", "Cr4", "Cr5", "S");
                } else if (p < 0.67) { // 33%
                    return Arrays.asList("Cr1", "Cr2", "Cr5", "S");
                } else { // 33%
                    return Arrays.asList("Cr1", "Cr2", "Cr3", "S");
                }

            case E2:
                if (p < 0.34) { // 34%
                    return Arrays.asList("Cr2", "Cr5", "S");
                } else if (p < 0.67) { // 33%
                    return Arrays.asList("Cr2", "Cr3", "S");
                } else { // 33%
                    return Arrays.asList("Cr2", "Cr1", "Cr4", "Cr5", "S");
                }

            case E3:
                if (p < 0.34) { // 34%
                    return Arrays.asList("Cr3", "S");
                } else if (p < 0.67) { // 33%
                    return Arrays.asList("Cr3", "Cr2", "Cr5", "S");
                } else { // 33%
                    return Arrays.asList("Cr3", "Cr2", "Cr1", "Cr4", "Cr5", "S");
                }

            default:
                return Collections.singletonList("S");
        }
    }
}