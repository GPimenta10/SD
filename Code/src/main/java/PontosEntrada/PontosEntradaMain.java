package PontosEntrada;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;

import Utils.ConfigLoader;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Processo responsável por gerir a entrada de veículos no sistema.
 *
 * Argumentos esperados:
 *   args[0] - Carga selecionada (BAIXA, MEDIA, ALTA)
 *   args[1] - Cenário selecionado (ALEATORIO, CAMINHO_CURTO)
 */
public class PontosEntradaMain {
    public static void main(String[] args) throws Exception {
        Gson gson = new Gson();

        // Ler argumentos
        String cargaSelecionada = (args.length > 0) ? args[0] : "BAIXA";
        String cenarioStr = (args.length > 1) ? args[1] : "ALEATORIO";

        // Converter string para enum TipoCenario
        TipoCenario cenario;
        try {
            cenario = TipoCenario.valueOf(cenarioStr);
        } catch (IllegalArgumentException e) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Cenário '" + cenarioStr + "' desconhecido. A usar 'ALEATORIO'.");
            cenario = TipoCenario.ALEATORIO;
        }

        JsonObject configDashboard = ConfigLoader.carregarDashboard();
        String ipDashboard = configDashboard.get("ipServidor").getAsString();
        int portaDashboard = configDashboard.get("portaServidor").getAsInt();

        // 1. LER CONFIGURAÇÃO DAS ENTRADAS
        JsonArray entradasJson = ConfigLoader.carregarEntradas();

        if (entradasJson.size() == 0) {
            LogClienteDashboard.enviar(TipoLog.ERRO, "Nenhuma entrada encontrada no ficheiro de configuração.");
            throw new IllegalArgumentException("Nenhuma entrada encontrada.");
        }

        LogClienteDashboard.enviar(TipoLog.SISTEMA, "A gerar veículos para todas as entradas: E1, E2, E3");

        // 2. LER CONFIGURAÇÃO DAS CARGAS
        String jsonCargas = readResourceAsString("configCargas.json");
        JsonObject rootCargas = gson.fromJson(jsonCargas, JsonObject.class);

        if (!rootCargas.getAsJsonObject("cargas").has(cargaSelecionada)) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Carga '" + cargaSelecionada + "' desconhecida. A usar 'BAIXA'.");
            cargaSelecionada = "BAIXA";
        }

        JsonObject carga = rootCargas.getAsJsonObject("cargas").getAsJsonObject(cargaSelecionada);

        int totalVeiculos = carga.get("totalVeiculos").getAsInt();
        long intervaloMs = carga.get("intervaloMs").getAsLong();

        LogClienteDashboard.enviar(TipoLog.SISTEMA,
                String.format("Geradores: Carga=%s | Cenário=%s | Total=%d | Intervalo=%dms",
                        cargaSelecionada, cenario.getDescricao(), totalVeiculos, intervaloMs));

        // 3. DISTRIBUIR ENTRE E1/E2/E3
        int nEntradas = entradasJson.size();
        int base = totalVeiculos / nEntradas;
        int resto = totalVeiculos % nEntradas;

        GeradorVeiculos[] geradores = new GeradorVeiculos[nEntradas];

        for (int idx = 0; idx < nEntradas; idx++) {
            JsonObject e = entradasJson.get(idx).getAsJsonObject();
            String id = e.get("id").getAsString();
            String host = e.get("cruzamentoHost").getAsString();
            int porta = e.get("cruzamentoPorta").getAsInt();

            int limiteLocal = base + (idx < resto ? 1 : 0);

            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Entrada " + id + " → vai gerar " + limiteLocal + " veículos.");

            geradores[idx] = new GeradorVeiculos(
                    PontoEntrada.valueOf(id),
                    host,
                    porta,
                    intervaloMs,
                    limiteLocal,
                    ipDashboard,
                    portaDashboard,
                    cenario
            );
        }

        // 4. INICIAR GERADORES
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Geradores de entrada iniciados.");

        for (GeradorVeiculos g : geradores) {
            g.start();
        }

        // 5. AGUARDAR CONCLUSÃO
        for (GeradorVeiculos g : geradores) {
            g.join();
        }
        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Todos os geradores concluíram a criação de veículos.");
    }

    /**
     * Utilitário para ler ficheiros de recursos do JAR/Classpath.
     *
     * @param resourceName Nome do recurso a ler
     * @return Conteúdo do ficheiro como String
     * @throws IOException Se o recurso não for encontrado
     */
    private static String readResourceAsString(String resourceName) throws IOException {
        ClassLoader cl = PontosEntradaMain.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Recurso não encontrado: " + resourceName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}