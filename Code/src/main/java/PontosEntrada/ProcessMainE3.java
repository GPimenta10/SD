package PontosEntrada;

public class ProcessMainE3 {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Gerador de Veículos E3");
        System.out.println("=".repeat(60));

        GeradorVeiculos gerador = new GeradorVeiculos(PontoEntrada.E3, "localhost", 5003, 3000);
        gerador.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ProcessMainE3] Encerrando gerador...");
            gerador.parar();
        }));

        try {
            while (true) Thread.sleep(1000);
        } catch (InterruptedException e) {
            gerador.parar();
        }
    }
}
