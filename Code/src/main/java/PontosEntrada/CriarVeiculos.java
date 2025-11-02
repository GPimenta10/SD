package PontosEntrada;

import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Responsável por criar veículos com ID, tipo e caminho.
 */
public class CriarVeiculos {

    private final PontoEntrada pontoEntrada;
    private static final Map<PontoEntrada, AtomicInteger> CONTADORES = new ConcurrentHashMap<>(Map.of(
            PontoEntrada.E1, new AtomicInteger(0),
            PontoEntrada.E2, new AtomicInteger(0),
            PontoEntrada.E3, new AtomicInteger(0)
    ));

    public CriarVeiculos(PontoEntrada pontoEntrada) {
        this.pontoEntrada = pontoEntrada;
    }

    public Veiculo criarVeiculo() {
        TipoVeiculo tipo = sortearTipo();
        List<String> caminho = Caminhos.gerarCaminho(pontoEntrada);

        int numero = CONTADORES.get(pontoEntrada).incrementAndGet();
        String id = pontoEntrada.name() + String.format("%03d", numero);

        return new Veiculo(id, tipo, pontoEntrada, caminho);
    }

    private TipoVeiculo sortearTipo() {
        double p = ThreadLocalRandom.current().nextDouble();
        if (p < 0.33) return TipoVeiculo.MOTA;
        if (p < 0.66) return TipoVeiculo.CARRO;
        return TipoVeiculo.CAMIAO;
    }
}
