package PontosEntrada;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Calcula o intervalo até o próximo veículo (fixo ou Poisson).
 */
public class Temporizador {

    private final GeradorVeiculos.ModoGeracao modo;
    private final long intervaloFixo = 2000;
    private final double taxaLambda = 0.5;

    public Temporizador(GeradorVeiculos.ModoGeracao modo) {
        this.modo = modo;
    }

    public long proximoIntervalo() {
        if (modo == GeradorVeiculos.ModoGeracao.FIXO) {
            return intervaloFixo;
        } else {
            double u = ThreadLocalRandom.current().nextDouble();
            return (long) (-Math.log(1 - u) / taxaLambda * 1000);
        }
    }
}
