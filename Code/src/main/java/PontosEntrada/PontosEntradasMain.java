package PontosEntrada;

import Dashboard.TipoLog;
import Utils.EnviarLogs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PontosEntradasMain {

    public static void main(String[] args) throws Exception {

        // Por agora cenário fixo
        String cargaSelecionada = "BAIXA";

        Gson gson = new Gson();

        // 1. LER CONFIGURAÇÃO DAS ENTRADAS
        String jsonEntradas = readResourceAsString("configEntradas.json");
        JsonObject rootEntradas = gson.fromJson(jsonEntradas, JsonObject.class);
        JsonArray entradasJson = rootEntradas.getAsJsonArray("entradas");

        // Filtrar por argumento (--only)
        java.util.Set<String> onlyIds = null;
        if (args != null) {
            for (String a : args) {
                if (a != null && a.startsWith("--only=")) {
                    onlyIds = new java.util.HashSet<>();
                    for (String id : a.substring("--only=".length()).split(",")) {
                        if (!id.isBlank()) onlyIds.add(id.trim());
                    }
                }
            }
        }

        java.util.List<JsonObject> selecionadas = new java.util.ArrayList<>();
        for (var elem : entradasJson) {
            JsonObject e = elem.getAsJsonObject();
            String id = e.get("id").getAsString();
            if (onlyIds == null || onlyIds.contains(id)) {
                selecionadas.add(e);
            }
        }

        if (selecionadas.isEmpty()) {
            EnviarLogs.enviar(TipoLog.ERRO, "Nenhuma entrada selecionada. Verifique o argumento --only ou o ficheiro config.");
            throw new IllegalArgumentException("Nenhuma entrada selecionada.");
        }

        // 2. LER CONFIGURAÇÃO DAS CARGAS
        String jsonCargas = readResourceAsString("configCargas.json");
        JsonObject rootCargas = gson.fromJson(jsonCargas, JsonObject.class);
        JsonObject carga = rootCargas.getAsJsonObject("cargas").getAsJsonObject(cargaSelecionada);

        int totalVeiculos = carga.get("totalVeiculos").getAsInt();
        long intervaloMs = carga.get("intervaloMs").getAsLong();

        // Log importante → cenário escolhido
        EnviarLogs.enviar(TipoLog.SISTEMA, "Geradores: cenário " + cargaSelecionada + " | Total veículos: " + totalVeiculos +
                       " | Intervalo: " + intervaloMs + " ms");

        // 3. DISTRIBUIR ENTRE E1/E2/E3
        int nEntradas = selecionadas.size();
        int base = totalVeiculos / nEntradas;
        int resto = totalVeiculos % nEntradas;

        GeradorVeiculos[] geradores = new GeradorVeiculos[nEntradas];

        int idx = 0;
        for (JsonObject e : selecionadas) {
            String id = e.get("id").getAsString();
            String host = e.get("cruzamentoHost").getAsString();
            int porta = e.get("cruzamentoPorta").getAsInt();

            int limiteLocal = base + (idx < resto ? 1 : 0);

            EnviarLogs.enviar(TipoLog.SISTEMA, "Entrada " + id + " → vai gerar " + limiteLocal + " veículos.");

            geradores[idx] = new GeradorVeiculos(
                    PontoEntrada.valueOf(id),
                    host,
                    porta,
                    intervaloMs,
                    limiteLocal
            );
            idx++;
        }

        // 4. INICIAR GERADORES
        EnviarLogs.enviar(TipoLog.SISTEMA, "Geradores de entrada iniciados.");

        for (GeradorVeiculos g : geradores) {
            g.start();
        }

        // 5. AGUARDAR CONCLUSÃO
        for (GeradorVeiculos g : geradores) {
            g.join();
        }
        EnviarLogs.enviar(TipoLog.SUCESSO, "Todos os geradores concluíram a criação de veículos.");
    }

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
