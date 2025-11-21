package Saida;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;
import Utils.ConfigLoader;

import com.google.gson.JsonObject;

/**
 * Processo principal para a Saída do sistema de tráfego.
 *
 * Carrega as configurações do ficheiro configMapa.json e inicializa o servidor
 * que recebe veículos que completam o seu percurso no sistema.
 */
public class SaidaMain {
    public static void main(String[] args) {
        LogClienteDashboard.definirNomeProcesso("Saida");
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Processo Saída iniciado");

        JsonObject config = ConfigLoader.carregarSaida();

        // Ler IP e Porta da configuração
        String ipServidor = config.has("ipServidor")
                ? config.get("ipServidor").getAsString()
                : "localhost";
        int portaServidor = config.get("portaServidor").getAsInt();

        String ipDashboard = config.get("ipDashboard").getAsString();
        int portaDashboard = config.get("portaDashboard").getAsInt();

        Saida saida = new Saida(ipServidor, portaServidor, ipDashboard, portaDashboard);
        saida.iniciar();

        LogClienteDashboard.enviar(TipoLog.SISTEMA, String.format("Saída configurada: local %s:%d → Dashboard %s:%d",
                ipServidor, portaServidor, ipDashboard, portaDashboard)
        );

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "A encerrar Saída");
            saida.parar();
        }));

        try {
            while (true) {
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Saída interrompida: " + e.getMessage());
            saida.parar();
        }
    }
}