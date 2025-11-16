package PontosEntrada;

/**
 * Processo independente para o Gerador de Veículos E2
 * Gera exatamente 7 veículos (distribuição uniforme de 20 veículos)
 */
public class ProcessMainE2 {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Gerador de Veículos E2");
        System.out.println("=".repeat(60));

        // Gera 7 veículos a cada 3000ms e envia para Cr2
        GeradorVeiculosLimitado gerador = new GeradorVeiculosLimitado(
                PontoEntrada.E2,
                "localhost",
                5002,  // Porta do Cr2
                3000,  // Intervalo de geração
                7      // LIMITE: 7 veículos
        );
        gerador.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ProcessMainE2] Encerrando gerador...");
            gerador.parar();
        }));

        try {
            // Aguarda o gerador terminar
            gerador.join();
            System.out.println("\n[ProcessMainE2] ✓ Gerador E2 finalizou com sucesso!");
            System.out.printf("[ProcessMainE2] Total gerado: %d veículos%n", gerador.getTotalGerado());

            // Mantém processo ativo por mais 30 segundos para logs
            Thread.sleep(30000);

        } catch (InterruptedException e) {
            gerador.parar();
        }

        System.out.println("[ProcessMainE2] Processo encerrado.");
    }
}