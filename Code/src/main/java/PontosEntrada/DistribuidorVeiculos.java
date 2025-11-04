package PontosEntrada;

import Veiculo.Veiculo;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.SocketException;

/**
 * Responsável por enviar veículos via socket TCP usando serialização.
 *
 * ✅ NOVO: Implementa backpressure ao detectar filas cheias
 * ✅ Usa retry com backoff exponencial
 */
public class DistribuidorVeiculos {

    private final String hostDestino;
    private final int portaDestino;

    // Configurações de retry
    private static final int MAX_TENTATIVAS = 5;
    private static final long TEMPO_INICIAL_RETRY_MS = 500;
    private static final long TEMPO_MAXIMO_RETRY_MS = 5000;

    public DistribuidorVeiculos(String hostDestino, int portaDestino) {
        this.hostDestino = hostDestino;
        this.portaDestino = portaDestino;
    }

    /**
     * Envia um veículo serializado através do ObjectOutputStream.
     *
     *
     * @param oos Stream de objetos já aberto
     * @param v Veículo a enviar
     * @throws IOException Se houver erro irrecuperável
     */
    public void enviar(ObjectOutputStream oos, Veiculo v) throws IOException {
        int tentativa = 0;
        long tempoEspera = TEMPO_INICIAL_RETRY_MS;

        while (tentativa < MAX_TENTATIVAS) {
            try {
                // Tenta enviar
                synchronized (oos) {
                    oos.writeObject(v);
                    oos.flush();
                }

                System.out.printf("[Distribuidor] ✓ Enviado %s (%s) via %s:%d%n",
                        v.getId(), v.getTipo(), hostDestino, portaDestino);

                return; // ✅ Sucesso!

            } catch (SocketException e) {
                tentativa++;

                if (tentativa >= MAX_TENTATIVAS) {
                    System.err.printf("[Distribuidor] ✗ Falha permanente após %d tentativas: %s (%s)%n",
                            MAX_TENTATIVAS, v.getId(), e.getMessage());
                    throw new IOException("Falha após " + MAX_TENTATIVAS + " tentativas", e);
                }

                // Backoff exponencial
                System.err.printf("[Distribuidor] ⚠️ Tentativa %d/%d falhou para %s. " +
                                "Aguardando %dms (possível fila cheia)...%n",
                        tentativa, MAX_TENTATIVAS, v.getId(), tempoEspera);

                try {
                    Thread.sleep(tempoEspera);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrompido durante retry", ie);
                }

                // Aumenta tempo de espera (backoff exponencial)
                tempoEspera = Math.min(tempoEspera * 2, TEMPO_MAXIMO_RETRY_MS);

            } catch (IOException e) {
                // Outro erro genérico de escrita (stream fechado, etc)
                System.err.printf("[Distribuidor] ✗ Falha ao enviar %s (%s): %s%n",
                        v.getId(), v.getTipo(), e.getMessage());
                throw e;
            }
        }
    }

    /**
     * Versão alternativa: envia com timeout explícito.
     * Útil se quisermos controle mais fino.
     */
    public boolean enviarComTimeout(ObjectOutputStream oos, Veiculo v, long timeoutMs) {
        long inicio = System.currentTimeMillis();

        while (System.currentTimeMillis() - inicio < timeoutMs) {
            try {
                enviar(oos, v);
                return true;
            } catch (IOException e) {
                // Aguarda um pouco antes de tentar novamente
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }

        System.err.printf("[Distribuidor] ⏱️ Timeout ao enviar %s após %dms%n",
                v.getId(), timeoutMs);
        return false;
    }
}