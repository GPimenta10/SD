package Cruzamentos;

import Collections.Exceptions.EmptyCollectionException;
import Collections.Queue.LinkedQueue;
import Veiculo.Veiculo;

/**
 * Classe que encapsula uma fila de veículos.
 * Usa uma LinkedQueue como estrutura de dados subjacente.
 * A LinkedQueue usada, foi utilizada na UC Estrutura de Dados
 * Oferece métodos sincronizados para garantir segurança em concorrência.
 */
public class FilaVeiculos {

    /** Estrutura interna da fila */
    private final LinkedQueue<Veiculo> filaVeiculos = new LinkedQueue<>();

    /**
     * Adiciona um veículo à fila.
     *
     * @param veiculo Veículo a adicionar
     */
    public synchronized void adicionar(Veiculo veiculo) {
        if (veiculo == null) {
            // System.err.println("[FilaVeiculos] Tentativa de adicionar veículo nulo ignorada.");
            return;
        }

        filaVeiculos.enqueue(veiculo);

        // Log interno (comentado — útil para debug futuro)
        // System.out.printf("[FilaVeiculos] Veículo %s adicionado à fila. Tamanho atual: %d%n",
        //        veiculo.getId(), filaVeiculos.size());
    }

    /**
     * Remove e devolve o primeiro veículo da fila, se existir.
     *
     * @return Retorna null se a fila estiver vazia.
     */
    public synchronized Veiculo removerSeDisponivel() {
        if (filaVeiculos.isEmpty()) {
            return null;
        }

        try {
            return filaVeiculos.dequeue();
        } catch (EmptyCollectionException e) {
            return null;
        }
    }

    /**
     * Devolve o tamanho atual da fila.
     *
     * @return Número de veículos na fila
     */
    public synchronized int getTamanhoAtual() {
        return filaVeiculos.size();
    }

    /**
     * Verifica se a fila está vazia.
     *
     * @return True se vazia, false caso contrário
     */
    public synchronized boolean isVazia() {
        return filaVeiculos.isEmpty();
    }

    /**
     * Representação textual da fila (para debug)
     *
     * @return Uma String com dados da fila
     */
    @Override
    public synchronized String toString() {
        return filaVeiculos.toString();
    }
}