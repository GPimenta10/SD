package PontosEntrada;

/**
 * Processo independente para o Gerador de Veículos E1
 * Gera exatamente 7 veículos (distribuição uniforme de 20 veículos)
 */
public class ProcessMainE1 {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Gerador de Veículos E1");
        System.out.println("=".repeat(60));

        // Gera 7 veículos a cada 3000ms e envia para Cr1
        GeradorVeiculosLimitado gerador = new GeradorVeiculosLimitado(
                PontoEntrada.E1,
                "localhost",
                5001,  // Porta do Cr1
                3000,  // Intervalo de geração
                7      // LIMITE: 7 veículos
        );
        gerador.start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[ProcessMainE1] Encerrando gerador...");
            gerador.parar();
        }));

        try {
            // Aguarda o gerador terminar
            gerador.join();
            System.out.println("\n[ProcessMainE1] ✓ Gerador E1 finalizou com sucesso!");
            System.out.printf("[ProcessMainE1] Total gerado: %d veículos%n", gerador.getTotalGerado());

            // Mantém processo ativo por mais 30 segundos para logs
            Thread.sleep(30000);

        } catch (InterruptedException e) {
            gerador.parar();
        }

        System.out.println("[ProcessMainE1] Processo encerrado.");
    }
}