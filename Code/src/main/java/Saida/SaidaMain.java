package Saida;

import Dashboard.TipoLog;
import Utils.EnviarLogs;

/**
 * Processo independente para a Saída.
 */
public class SaidaMain {

    public static void main(String[] args) {

        // System.out.println("=".repeat(60));
        // System.out.println("PROCESSO: Saída");
        // System.out.println("=".repeat(60));
        EnviarLogs.definirNomeProcesso("Saida");
        EnviarLogs.enviar(TipoLog.SISTEMA, "Processo Saída iniciado");

        Saida saida = new Saida(5999, "localhost", 6000);
        saida.iniciar();
        EnviarLogs.enviar(TipoLog.SISTEMA, "Saída ligação: porta local 5999 → Dashboard 6000");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
           EnviarLogs.enviar(TipoLog.SISTEMA, "A encerrar Saída");
            saida.parar();
        }));

        try {
            while (true) Thread.sleep(1000);
        } catch (InterruptedException e) {
           EnviarLogs.enviar(TipoLog.AVISO, "Saída interrompida: " + e.getMessage());
            saida.parar();
        }
    }
}
