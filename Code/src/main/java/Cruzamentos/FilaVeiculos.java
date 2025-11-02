package Cruzamentos;

import Veiculo.Veiculo;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Fila thread-safe para gerenciar veículos esperando em semáforos
 * Usa sincronização com wait/notify para coordenação entre threads
 */
public class FilaVeiculos {
    private final Queue<Veiculo> fila;
    private final String identificador;
    private int tamanhoMaximo;
    private long somaTemposEspera;
    private int totalVeiculosProcessados;

    /**
     * Construtor da fila
     */
    public FilaVeiculos(String identificador) {
        this.fila = new LinkedList<>();
        this.identificador = identificador;
        this.tamanhoMaximo = 0;
        this.somaTemposEspera = 0;
        this.totalVeiculosProcessados = 0;
    }

    /**
     * Adiciona veículo à fila (thread-safe)
     * @param veiculo Veículo a ser adicionado
     * @return Tempo de entrada na fila
     */
    public synchronized long adicionarVeiculo(Veiculo veiculo) {
        fila.offer(veiculo);

        // Atualiza tamanho máximo
        if (fila.size() > tamanhoMaximo) {
            tamanhoMaximo = fila.size();
        }

        // Notifica threads esperando (ex: semáforo esperando veículos)
        notifyAll();

        System.out.printf("[%s] Veículo %s adicionado. Tamanho fila: %d%n",
                identificador, veiculo.getId(), fila.size());

        return System.currentTimeMillis();
    }

    /**
     * Remove e retorna o próximo veículo (thread-safe)
     * Bloqueia se a fila estiver vazia
     * @param timeout Tempo máximo de espera em ms (0 = sem timeout)
     * @return Veículo removido ou null se timeout
     */
    public synchronized Veiculo removerVeiculo(long timeout) throws InterruptedException {
        long inicio = System.currentTimeMillis();

        // Espera até ter veículo disponível ou timeout
        while (fila.isEmpty()) {
            if (timeout > 0) {
                long tempoRestante = timeout - (System.currentTimeMillis() - inicio);
                if (tempoRestante <= 0) {
                    return null;
                }
                wait(tempoRestante);
            } else {
                wait(); // Espera indefinidamente
            }
        }

        Veiculo veiculo = fila.poll();
        totalVeiculosProcessados++;

        System.out.printf("[%s] Veículo %s removido. Tamanho fila: %d%n",
                identificador, veiculo.getId(), fila.size());

        return veiculo;
    }

    /**
     * Remove e retorna o próximo veículo, sem bloquear.
     * Retorna null se a fila estiver vazia.
     */
    public synchronized Veiculo removerSeDisponivel() {
        if (fila.isEmpty()) {
            return null;
        }

        Veiculo veiculo = fila.poll();
        totalVeiculosProcessados++;

        System.out.printf("[%s] Veículo %s removido (não bloqueante). Tamanho fila: %d%n",
                identificador, veiculo.getId(), fila.size());

        return veiculo;
    }

    /**
     * Registra tempo de espera de um veículo
     */
    public synchronized void registrarTempoEspera(long tempoEspera) {
        somaTemposEspera += tempoEspera;
    }

    /**
     * Retorna tamanho atual da fila (thread-safe)
     */
    public synchronized int getTamanhoAtual() {
        return fila.size();
    }

    /**
     * Retorna tamanho máximo atingido
     */
    public synchronized int getTamanhoMaximo() {
        return tamanhoMaximo;
    }

    /**
     * Retorna tamanho médio da fila
     */
    public synchronized double getTamanhoMedio() {
        if (totalVeiculosProcessados == 0) {
            return 0.0;
        }
        // Aproximação: soma dos tamanhos / total processado
        return (double) somaTemposEspera / totalVeiculosProcessados;
    }

    /**
     * Retorna tempo médio de espera
     */
    public synchronized long getTempoMedioEspera() {
        if (totalVeiculosProcessados == 0) {
            return 0;
        }
        return somaTemposEspera / totalVeiculosProcessados;
    }

    /**
     * Retorna total de veículos processados
     */
    public synchronized int getTotalProcessados() {
        return totalVeiculosProcessados;
    }

    /**
     * Verifica se fila está vazia (thread-safe)
     */
    public synchronized boolean estaVazia() {
        return fila.isEmpty();
    }

    /**
     * Retorna estatísticas da fila
     */
    public synchronized String getEstatisticas() {
        return String.format(
                "Fila[%s]: atual=%d, max=%d, processados=%d, tempoMedioEspera=%dms",
                identificador, fila.size(), tamanhoMaximo,
                totalVeiculosProcessados, getTempoMedioEspera()
        );
    }

    @Override
    public synchronized String toString() {
        return String.format("FilaVeiculos[%s: %d veículos]",
                identificador, fila.size());
    }
}