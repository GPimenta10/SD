package Cruzamentos;

/**
 *
 */
public class MonitorSemaforos {
    private int turno = 0;
    private final int total;

    /**
     * Construtor da classe
     *
     * @param total
     */
    public MonitorSemaforos(int total) {
        if (total <= 0) {
            throw new IllegalArgumentException("Total de semáforos deve ser maior que zero");
        }
        this.total = total;
    }

    /**
     *
     *
     * @param id
     * @throws InterruptedException
     */
    public synchronized void esperarVez(int id) throws InterruptedException {
        if (id < 0 || id >= total) {
            throw new IllegalArgumentException("ID de semáforo inválido: " + id);
        }
        while (turno != id)
            wait();
    }

    /**
     *
     *
     */
    public synchronized void proximaVez() {
        turno = (turno + 1) % total;
        notifyAll();
    }

    /**
     *
     *
     */
    public synchronized void acordarTodos() {
        notifyAll();
    }
}

