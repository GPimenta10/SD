package Cruzamentos;

import Collections.Queue.LinkedQueue;
import Collections.Exceptions.EmptyCollectionException;
import Veiculo.Veiculo;

/**
 * Classe que encapsula uma fila de veículos.
 * Usa uma LinkedQueue genérica como estrutura de dados subjacente.
 * Oferece métodos sincronizados para garantir segurança em concorrência.
 */
public class FilaVeiculos {

    private final LinkedQueue<Veiculo> filaVeiculos = new LinkedQueue<>();

    /**
     * Adiciona um veículo à fila.
     * @param veiculo Veículo a adicionar
     */
    public synchronized void adicionar(Veiculo veiculo) {
        if (veiculo == null) {
            System.err.println("[FilaVeiculos] Tentativa de adicionar veículo nulo ignorada.");
            return;
        }
        filaVeiculos.enqueue(veiculo);
        System.out.printf("[FilaVeiculos] Veículo %s adicionado à fila. Tamanho atual: %d%n",
                veiculo.getId(), filaVeiculos.size());
    }

    /**
     * Remove e devolve o primeiro veículo da fila, se existir.
     * Retorna null se a fila estiver vazia.
     */
    public synchronized Veiculo removerSeDisponivel() {
        if (filaVeiculos.isEmpty()) {
            return null;
        }

        try {
            Veiculo veiculo = filaVeiculos.dequeue();
            System.out.printf("[FilaVeiculos] Veículo %s saiu da fila. Tamanho restante: %d%n",
                    veiculo.getId(), filaVeiculos.size());
            return veiculo;
        } catch (EmptyCollectionException e) {
            return null;
        }
    }

    /**
     * Devolve o tamanho atual da fila.
     * @return número de veículos na fila
     */
    public synchronized int getTamanhoAtual() {
        return filaVeiculos.size();
    }

    /**
     * Verifica se a fila está vazia.
     * @return true se vazia, false caso contrário
     */
    public synchronized boolean isVazia() {
        return filaVeiculos.isEmpty();
    }

    /**
     * Representação textual da fila.
     */
    @Override
    public synchronized String toString() {
        return filaVeiculos.toString();
    }
}
