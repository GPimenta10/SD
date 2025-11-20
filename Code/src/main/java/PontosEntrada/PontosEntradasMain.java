package PontosEntrada;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 *
 */
public class PontosEntradasMain {
    public static void main(String[] args) throws Exception {
        // Por agora cenário fixo
        String cargaSelecionada = "BAIXA";

        Gson gson = new Gson();

        // 1. LER CONFIGURAÇÃO DAS ENTRADAS
        String jsonEntradas = readResourceAsString("configEntradas.json");
        JsonObject rootEntradas = gson.fromJson(jsonEntradas, JsonObject.class);
        JsonArray entradasJson = rootEntradas.getAsJsonArray("entradas");

        // Usar todas as entradas disponíveis
        if (entradasJson.size() == 0) {
            LogClienteDashboard.enviar(TipoLog.ERRO, "Nenhuma entrada encontrada no ficheiro de configuração.");
            throw new IllegalArgumentException("Nenhuma entrada encontrada.");
        }

        LogClienteDashboard.enviar(TipoLog.SISTEMA, "A gerar veículos para todas as entradas: E1, E2, E3");

        // 2. LER CONFIGURAÇÃO DAS CARGAS
        String jsonCargas = readResourceAsString("configCargas.json");
        JsonObject rootCargas = gson.fromJson(jsonCargas, JsonObject.class);
        JsonObject carga = rootCargas.getAsJsonObject("cargas").getAsJsonObject(cargaSelecionada);

        int totalVeiculos = carga.get("totalVeiculos").getAsInt();
        long intervaloMs = carga.get("intervaloMs").getAsLong();

        // Log importante → cenário escolhido
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Geradores: cenário " + cargaSelecionada + " | Total veículos: " + totalVeiculos +
                " | Intervalo: " + intervaloMs + " ms");

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
                    limiteLocal
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
     *
     *
     * @param resourceName
     * @return
     * @throws IOException
     */
    private static String readResourceAsString(String resourceName) throws IOException {
        ClassLoader cl = PontosEntradasMain.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Recurso não encontrado: " + resourceName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}