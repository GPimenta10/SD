package Cruzamentos;

public class MonitorSemaforos {

    private int turno = 0;
    private final int total;

    public MonitorSemaforos(int total) {
        this.total = total;
    }

    public synchronized void esperarVez(int id) throws InterruptedException {
        while (turno != id)
            wait();
    }

    public synchronized void proximaVez() {
        turno = (turno + 1) % total;
        notifyAll();
    }

    public synchronized void acordarTodos() {
        notifyAll();
    }
}
