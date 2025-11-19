package Utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.FileReader;

/**
 * Utilitário para carregar configurações do ficheiro config.json.
 *
 * Fornece métodos para aceder às diferentes secções de configuração
 * do sistema (dashboard, saída, cruzamentos).
 */
public class ConfigLoader {

    private static final String CONFIG_FILE = "/resources/config.json";
    private static final Gson gson = new Gson();
    private static JsonObject configCache = null;

    /**
     * Carrega e retorna uma secção específica do ficheiro config.json.
     *
     * @param secao Nome da secção a carregar (ex: "saida", "dashboard")
     * @return JsonObject com os dados da secção solicitada
     */
    public static JsonObject carregarSecao(String secao) {
        try {
            if (configCache == null) {
                try (FileReader reader = new FileReader(CONFIG_FILE)) {
                    configCache = gson.fromJson(reader, JsonObject.class);
                }
            }
            return configCache.getAsJsonObject(secao);
        } catch (Exception e) {
            System.err.println("ERRO: Não foi possível carregar " + CONFIG_FILE + " - " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    /**
     * Carrega a configuração da Saída.
     *
     * @return JsonObject com configuração da saída
     */
    public static JsonObject carregarSaida() {
        return carregarSecao("saida");
    }

    /**
     * Carrega a configuração do Dashboard.
     *
     * @return JsonObject com configuração do dashboard
     */
    public static JsonObject carregarDashboard() {
        return carregarSecao("dashboard");
    }
}