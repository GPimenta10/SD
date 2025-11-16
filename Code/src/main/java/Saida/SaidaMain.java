package Saida;

/**
 * Processo independente para a Saída.
 */
public class SaidaMain {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Saída");
        System.out.println("=".repeat(60));

        Saida saida = new Saida(5999, "localhost", 6000);
        saida.iniciar();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SaidaMain] Encerrando Saída...");
            saida.parar();
        }));

        try {
            while (true) Thread.sleep(1000);
        } catch (InterruptedException e) {
            saida.parar();
        }
    }
}
