package PontosEntrada;

import Veiculo.Veiculo;
import java.io.PrintWriter;

/**
 * Responsável por enviar os veículos via socket TCP.
 */
public class DistribuidorVeiculos {

    private final String hostDestino;
    private final int portaDestino;

    public DistribuidorVeiculos(String hostDestino, int portaDestino) {
        this.hostDestino = hostDestino;
        this.portaDestino = portaDestino;
    }

    public void enviar(PrintWriter writer, Veiculo v) {
        String mensagem = String.format("VEICULO|%s|%s|%s|%s|%d",
                v.getId(), v.getTipo(), v.getPontoEntrada(), v.getProximoNo(), v.getTempoChegada());
        writer.println(mensagem);

        System.out.printf("[Distribuidor] Enviado %s (%s) via %s:%d%n",
                v.getId(), v.getTipo(), hostDestino, portaDestino);
    }
}
