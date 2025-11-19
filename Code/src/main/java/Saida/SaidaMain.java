package Saida;

import Dashboard.Logs.TipoLog;
import Utils.ConfigLoader;
import Utils.EnviarLogs;
import com.google.gson.JsonObject;

/**
 * Processo principal para a Saída do sistema de tráfego.
 *
 * Carrega as configurações do ficheiro configMapa.json e inicializa o servidor
 * que recebe veículos que completam o seu percurso no sistema.
 */
public class SaidaMain {
    public static void main(String[] args) {
        EnviarLogs.definirNomeProcesso("Saida");
        EnviarLogs.enviar(TipoLog.SISTEMA, "Processo Saída iniciado");

        // Carrega configuração do ficheiro JSON
        JsonObject config = ConfigLoader.carregarSaida();
        int portaServidor = config.get("portaServidor").getAsInt();

        String ipDashboard = config.get("ipDashboard").getAsString();
        int portaDashboard = config.get("portaDashboard").getAsInt();

        // Inicializa a Saída com as configurações
        Saida saida = new Saida(portaServidor, ipDashboard, portaDashboard);
        saida.iniciar();

        EnviarLogs.enviar(TipoLog.SISTEMA, String.format("Saída configurada: porta local %d → Dashboard %s:%d",
                        portaServidor, ipDashboard, portaDashboard)
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            EnviarLogs.enviar(TipoLog.SISTEMA, "A encerrar Saída");
            saida.parar();
        }));

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            EnviarLogs.enviar(TipoLog.AVISO, "Saída interrompida: " + e.getMessage());
            saida.parar();
        }
    }
}