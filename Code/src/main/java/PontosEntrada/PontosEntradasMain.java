package PontosEntrada;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class PontosEntradasMain {

    public static void main(String[] args) throws Exception {

        // ==============================================
        // ESCOLHA DO CENÁRIO (trocar aqui por agora)
        // ==============================================
        String cargaSelecionada = "BAIXA";
        // BAIXA, MEDIA ou ALTA - Futuramente será um menu

        System.out.println("=".repeat(60));
        System.out.println("SISTEMA DE GERADORES DE ENTRADA");
        System.out.println("=".repeat(60));

        Gson gson = new Gson();

        // ===============================
        // 1. LER CONFIGURAÇÃO DAS ENTRADAS (classpath resources)
        // ===============================
        String jsonEntradas = readResourceAsString("configEntradas.json");
        JsonObject rootEntradas = gson.fromJson(jsonEntradas, JsonObject.class);
        JsonArray entradasJson = rootEntradas.getAsJsonArray("entradas");

        // Opcional: filtrar por entradas passadas em argumento: --only=E3 ou --only=E1,E3
        java.util.Set<String> onlyIds = null;
        if (args != null) {
            for (String a : args) {
                if (a != null && a.startsWith("--only=")) {
                    String ids = a.substring("--only=".length());
                    onlyIds = new java.util.HashSet<>();
                    for (String id : ids.split(",")) {
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
            throw new IllegalArgumentException("Nenhuma entrada selecionada. Verifique o argumento --only ou o config.");
        }

        // ===============================
        // 2. LER CONFIGURAÇÃO DAS CARGAS (classpath resources)
        // ===============================
        String jsonCargas = readResourceAsString("configCargas.json");
        JsonObject rootCargas = gson.fromJson(jsonCargas, JsonObject.class);
        JsonObject carga = rootCargas.getAsJsonObject("cargas").getAsJsonObject(cargaSelecionada);

        int totalVeiculos = carga.get("totalVeiculos").getAsInt();
        long intervaloMs = carga.get("intervaloMs").getAsLong();

        System.out.printf("Cenário selecionado: %s%n", cargaSelecionada);
        System.out.printf("Total global de veículos: %d%n", totalVeiculos);
        System.out.printf("Intervalo entre veículos: %d ms%n%n", intervaloMs);

        // ===============================
        // 3. DISTRIBUIR ENTRE E1/E2/E3
        // ===============================
        int nEntradas = selecionadas.size();
        int base = totalVeiculos / nEntradas;
        int resto = totalVeiculos % nEntradas;

        GeradorVeiculosLimitado[] geradores = new GeradorVeiculosLimitado[nEntradas];

        int idx = 0;
        for (JsonObject e : selecionadas) {

            String id = e.get("id").getAsString();
            String host = e.get("cruzamentoHost").getAsString();
            int porta = e.get("cruzamentoPorta").getAsInt();

            int limiteLocal = base + (idx < resto ? 1 : 0);

            System.out.printf("Entrada %s → %d veículos%n", id, limiteLocal);

            geradores[idx] = new GeradorVeiculosLimitado(
                    PontoEntrada.valueOf(id),
                    host,
                    porta,
                    intervaloMs,
                    limiteLocal
            );

            idx++;
        }

        // ===============================
        // 4. INICIAR GERADORES
        // ===============================
        System.out.println("\nA iniciar geradores...\n");

        for (GeradorVeiculosLimitado g : geradores)
            g.start();

        // ===============================
        // 5. AGUARDAR CONCLUSÃO
        // ===============================
        for (GeradorVeiculosLimitado g : geradores)
            g.join();

        System.out.println("\nTodos os geradores terminaram.");
    }

    private static String readResourceAsString(String resourceName) throws IOException {
        ClassLoader cl = PontosEntradasMain.class.getClassLoader();
        try (InputStream is = cl.getResourceAsStream(resourceName)) {
            if (is == null) {
                throw new IOException("Recurso não encontrado no classpath: " + resourceName);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
