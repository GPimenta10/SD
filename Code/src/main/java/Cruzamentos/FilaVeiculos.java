package Cruzamentos;

import Collections.Exceptions.EmptyCollectionException;
import Veiculo.Veiculo;
import Collections.Queue.LinkedQueue;

/**
 * Fila thread-safe para gerenciar veículos esperando em semáforos.
 *
 * ✅ Usa LinkedQueue personalizada
 * ✅ Limite fixo de 10 veículos
 * ✅ Sem prioridade para motas
 * ✅ Thread-safe com synchronized
 */
public class FilaVeiculos {
    private static final int LIMITE_MAXIMO = 10;

    private final LinkedQueue<Veiculo> fila;
    private final String identificador;

    // Estatísticas
    private int tamanhoMaximoAlcancado;
    private long somaTemposEspera;
    private int totalVeiculosProcessados;

    public FilaVeiculos(String identificador) {
        this.fila = new LinkedQueue<>();
        this.identificador = identificador;
        this.tamanhoMaximoAlcancado = 0;
        this.somaTemposEspera = 0;
        this.totalVeiculosProcessados = 0;
    }

    /**
     * Adiciona veículo à fila (thread-safe).
     *
     * @return true se adicionado com sucesso, false se fila cheia
     */
    public synchronized boolean adicionarVeiculo(Veiculo veiculo) {
        if (estaCheia()) {
            System.out.printf("[%s] ⚠️ Fila cheia (%d/%d). Veículo %s REJEITADO%n",
                    identificador, fila.size(), LIMITE_MAXIMO, veiculo.getId());
            return false;
        }

        fila.enqueue(veiculo);

        // Atualiza estatísticas
        if (fila.size() > tamanhoMaximoAlcancado) {
            tamanhoMaximoAlcancado = fila.size();
        }

        System.out.printf("[%s] ✓ Veículo %s adicionado [%d/%d]%n",
                identificador, veiculo.getId(), fila.size(), LIMITE_MAXIMO);

        notifyAll(); // acorda threads que esperam por veículos

        return true;
    }

    /**
     * Remove e retorna o próximo veículo (FIFO - sem prioridade).
     * Não bloqueia se fila vazia.
     *
     * @return Veículo removido ou null se vazia
     */
    public synchronized Veiculo removerSeDisponivel() {
        if (fila.isEmpty()) {
            return null;
        }

        try {
            Veiculo veiculo = fila.dequeue();
            totalVeiculosProcessados++;

            System.out.printf("[%s] 🚗 Veículo %s removido [%d/%d]%n",
                    identificador, veiculo.getId(), fila.size(), LIMITE_MAXIMO);

            notifyAll(); // notifica threads esperando por espaço

            return veiculo;

        } catch (EmptyCollectionException e) {
            // Não deve acontecer devido ao isEmpty(), mas por segurança
            System.err.printf("[%s] Erro inesperado ao remover veículo: %s%n",
                    identificador, e.getMessage());
            return null;
        }
    }

    /**
     * Aguarda até haver espaço na fila (bloqueante).
     * Usado por geradores para implementar backpressure.
     */
    public synchronized void aguardarEspaco() throws InterruptedException {
        while (estaCheia()) {
            System.out.printf("[%s] ⏳ Aguardando espaço... [%d/%d]%n",
                    identificador, fila.size(), LIMITE_MAXIMO);
            wait(1000); // timeout de 1s para evitar deadlock
        }
    }

    // ========== CONSULTAS (Thread-Safe) ==========

    public synchronized int getTamanhoAtual() {
        return fila.size();
    }

    public synchronized boolean estaCheia() {
        return fila.size() >= LIMITE_MAXIMO;
    }

    public synchronized boolean estaVazia() {
        return fila.isEmpty();
    }

    public synchronized int getEspacoDisponivel() {
        return LIMITE_MAXIMO - fila.size();
    }

    public synchronized int getTotalProcessados() {
        return totalVeiculosProcessados;
    }

    public synchronized int getTamanhoMaximo() {
        return tamanhoMaximoAlcancado;
    }

    // ========== ESTATÍSTICAS ==========

    public synchronized String getEstatisticas() {
        return String.format(
                "Fila[%s]: atual=%d/%d, max=%d, processados=%d",
                identificador,
                fila.size(),
                LIMITE_MAXIMO,
                tamanhoMaximoAlcancado,
                totalVeiculosProcessados
        );
    }

    @Override
    public synchronized String toString() {
        return String.format("FilaVeiculos[%s: %d/%d veículos]",
                identificador, fila.size(), LIMITE_MAXIMO);
    }
}