package Utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Utilitário para carregar configurações do ficheiro configMapa.json.
 *
 * Fornece métodos para aceder às diferentes secções de configuração
 * do sistema distribuído de tráfego urbano (dashboard, saída, cruzamentos).
 *
 * O ficheiro é carregado apenas uma vez e mantido em cache para
 * melhorar performance em acessos subsequentes.
 */
public class ConfigLoader {

    private static final String CONFIG_FILE = "/configMapa.json";
    private static final Gson gson = new Gson();
    private static JsonObject configCache = null;

    /**
     * Carrega o ficheiro configMapa.json do classpath para o cache.
     *
     * @throws Exception se o ficheiro não for encontrado ou for inválido
     */
    private static void carregarFicheiroConfig() throws Exception {
        var inputStream = ConfigLoader.class.getResourceAsStream(CONFIG_FILE);

        if (inputStream == null) {
            throw new Exception("Ficheiro " + CONFIG_FILE + " não encontrado no classpath (src/resources/)");
        }

        try (var reader = new java.io.InputStreamReader(inputStream)) {
            configCache = gson.fromJson(reader, JsonObject.class);
        }
    }

    /**
     * Carrega e retorna uma secção específica do ficheiro configMapa.json.
     *
     * Utiliza cache para evitar múltiplas leituras do ficheiro.
     * O ficheiro é carregado do classpath (src/resources/).
     * Em caso de erro, imprime mensagem e termina o processo.
     *
     * @param secao Nome da secção a carregar (ex: "saida", "dashboard")
     * @return JsonObject com os dados da secção solicitada
     */
    public static JsonObject carregarSecao(String secao) {
        try {
            if (configCache == null) {
                carregarFicheiroConfig();
            }
            return configCache.getAsJsonObject(secao);
        } catch (Exception e) {
            System.err.println("ERRO: Não foi possível carregar secção '" + secao + "' - " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    /**
     * Carrega a configuração da Saída.
     *
     * @return JsonObject com configuração da saída (porta, IP e porta do Dashboard)
     */
    public static JsonObject carregarSaida() {
        return carregarSecao("saida");
    }

    /**
     * Carrega a configuração do Dashboard.
     *
     * @return JsonObject com configuração do dashboard (porta do servidor)
     */
    public static JsonObject carregarDashboard() {
        return carregarSecao("dashboard");
    }

    /**
     * Carrega a configuração das Entradas
     *
     * @return JsonArray com configuração das entradas
     */
    public static JsonArray carregarEntradas() {
        try {
            if (configCache == null) {
                carregarFicheiroConfig();
            }
            return configCache.getAsJsonArray("entradas");
        } catch (Exception e) {
            System.err.println("ERRO: Não foi possível carregar entradas - " + e.getMessage());
            System.exit(1);
            return null;
        }
    }

    /**
     * Carrega a configuração de um cruzamento específico pelo nome.
     *
     * Procura no array "cruzamentos" o objeto cujo campo "nome" corresponde
     * ao nome fornecido. Se o cruzamento não for encontrado, retorna null.
     *
     * @param nomeCruzamento Nome do cruzamento a procurar (ex: "Cr1", "Cr2")
     * @return JsonObject com configuração do cruzamento, ou null se não encontrado
     */
    public static JsonObject carregarCruzamento(String nomeCruzamento) {
        try {
            if (configCache == null) {
                carregarFicheiroConfig();
            }

            JsonArray cruzamentos = configCache.getAsJsonArray("cruzamentos");

            if (cruzamentos == null) {
                System.err.println("ERRO: Secção 'cruzamentos' não encontrada no configMapa.json");
                return null;
            }

            for (JsonElement elem : cruzamentos) {
                JsonObject cr = elem.getAsJsonObject();
                if (cr.get("nome").getAsString().equals(nomeCruzamento)) {
                    return cr;
                }
            }

            return null;
        } catch (Exception e) {
            System.err.println("ERRO: Não foi possível carregar cruzamento - " + e.getMessage());
            System.exit(1);
            return null;
        }
    }
}