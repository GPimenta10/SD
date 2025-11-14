package PontosEntrada;

/**
 * Processo independente para o Gerador de Veículos E3
 * IMPORTANTE: Gera EXATAMENTE 6 veículos e depois PARA
 */
public class ProcessMainE3 {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Gerador de Veículos E3 (LIMITADO A 6)");
        System.out.println("=".repeat(60));

        // USA O GERADOR LIMITADO (não o infinito!)
        GeradorVeiculosLimitado gerador = new GeradorVeiculosLimitado(
                PontoEntrada.E3,
                "localhost",
                5003,  // Porta do Cr3
                4000,  // Intervalo: 4 segundos entre veículos
                6      // LIMITE: apenas 6 veículos
        );

        gerador.start();
        System.out.println("[ProcessMainE3] ⚙️  Gerador iniciado - limite: 6 veículos");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ProcessMainE3] Encerrando gerador...");
            gerador.parar();
        }));

        try {
            // Aguarda o gerador terminar naturalmente
            gerador.join();

            System.out.println("\n" + "=".repeat(60));
            System.out.println("GERADOR E3 FINALIZADO");
            System.out.println("=".repeat(60));
            System.out.printf("Total gerado: %d veículos%n", gerador.getTotalGerado());
            System.out.println("=".repeat(60));

            // Mantém processo ativo por mais 60 segundos para processar veículos restantes
            System.out.println("\n[ProcessMainE3] Aguardando processamento dos veículos...");
            Thread.sleep(60000);

        } catch (InterruptedException e) {
            gerador.parar();
        }

        System.out.println("[ProcessMainE3] ✓ Processo encerrado.");
    }
}