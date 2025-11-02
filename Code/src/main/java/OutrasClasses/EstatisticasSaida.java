package OutrasClasses;

import Veiculo.TipoVeiculo;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Responsável por calcular e armazenar estatísticas da saída.
 */
public class EstatisticasSaida {

    private final Map<TipoVeiculo, List<Long>> temposPorTipo = new ConcurrentHashMap<>();
    private final Map<TipoVeiculo, Integer> contagemPorTipo = new ConcurrentHashMap<>();
    private int totalVeiculos = 0;

    public EstatisticasSaida() {
        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            temposPorTipo.put(tipo, Collections.synchronizedList(new ArrayList<>()));
            contagemPorTipo.put(tipo, 0);
        }
    }

    public synchronized void registrarVeiculo(TipoVeiculo tipo, long dwellingTime) {
        temposPorTipo.get(tipo).add(dwellingTime);
        contagemPorTipo.put(tipo, contagemPorTipo.get(tipo) + 1);
        totalVeiculos++;
    }

    public int getTotalVeiculos() {
        return totalVeiculos;
    }

    private double media(List<Long> v) {
        return v.stream().mapToLong(Long::longValue).average().orElse(0);
    }

    private long min(List<Long> v) {
        return v.stream().min(Long::compare).orElse(0L);
    }

    private long max(List<Long> v) {
        return v.stream().max(Long::compare).orElse(0L);
    }

    private double desvio(List<Long> v, double media) {
        if (v.size() < 2) return 0;
        double soma = v.stream().mapToDouble(x -> Math.pow(x - media, 2)).sum();
        return Math.sqrt(soma / (v.size() - 1));
    }

    public void imprimirFinais() {
        System.out.println("\n═════════════════════════════════════════════════");
        System.out.printf("║   ESTATÍSTICAS FINAIS (%d veículos)            ║%n", totalVeiculos);
        System.out.println("═════════════════════════════════════════════════");

        for (TipoVeiculo tipo : TipoVeiculo.values()) {
            List<Long> tempos = temposPorTipo.get(tipo);
            if (tempos.isEmpty()) {
                System.out.printf("%s: Nenhum veículo.%n", tipo);
                continue;
            }

            double m = media(tempos);
            System.out.printf("%n%s (%d veículos)%n", tipo, contagemPorTipo.get(tipo));
            System.out.printf("  Médio: %.2fs%n", m / 1000.0);
            System.out.printf("  Mínimo: %.2fs%n", min(tempos) / 1000.0);
            System.out.printf("  Máximo: %.2fs%n", max(tempos) / 1000.0);
            System.out.printf("  Desvio Padrão: %.2fs%n", desvio(tempos, m) / 1000.0);
        }
        System.out.println("═════════════════════════════════════════════════\n");
    }
}

