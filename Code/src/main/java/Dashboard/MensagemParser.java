package Dashboard;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Classe utilitária para parsing de mensagens recebidas pelo Dashboard.
 *
 * ✅ Elimina duplicação de código
 * ✅ Centraliza lógica de extração
 * ✅ Thread-safe (stateless)
 */
public class MensagemParser {

    // Padrões regex compilados (performance)
    private static final Pattern PATTERN_ID = Pattern.compile("\\b(E[1-3]-?\\d{3,})\\b");
    private static final Pattern PATTERN_CRUZAMENTO = Pattern.compile("\\[Cruzamento(\\d+)\\]");
    private static final Pattern PATTERN_SEMAFORO = Pattern.compile("\\[Semáforo\\s+([^\\]]+)\\]");
    private static final Pattern PATTERN_TIPO = Pattern.compile("\\((MOTA|CARRO|CAMIAO)\\)");

    /**
     * Extrai ID do veículo da mensagem.
     * Exemplos: "E3-001", "E1001", "E2-042"
     *
     * @return ID do veículo ou null se não encontrado
     */
    public static String extrairID(String mensagem) {
        if (mensagem == null) return null;

        Matcher m = PATTERN_ID.matcher(mensagem);
        return m.find() ? m.group(1) : null;
    }

    /**
     * Extrai número do cruzamento da mensagem.
     * Exemplo: "[Cruzamento3]" → "Cruzamento3"
     */
    public static String extrairCruzamento(String mensagem) {
        if (mensagem == null) return null;

        Matcher m = PATTERN_CRUZAMENTO.matcher(mensagem);
        if (m.find()) {
            return "Cruzamento" + m.group(1);
        }
        return null;
    }

    /**
     * Extrai nome do semáforo da mensagem.
     * Exemplo: "[Semáforo E2]" → "E2"
     */
    public static String extrairSemaforo(String mensagem) {
        if (mensagem == null) return null;

        Matcher m = PATTERN_SEMAFORO.matcher(mensagem);
        return m.find() ? m.group(1).trim() : null;
    }

    /**
     * Extrai tipo de veículo da mensagem.
     * Exemplo: "Enviado E3-001 (CARRO)" → "CARRO"
     */
    public static String extrairTipo(String mensagem) {
        if (mensagem == null) return null;

        Matcher m = PATTERN_TIPO.matcher(mensagem);
        return m.find() ? m.group(1) : "DESCONHECIDO";
    }

    /**
     * Extrai valor de um campo chave=valor.
     * Exemplo: extrairValor("id=E3001 tipo=CARRO", "id=") → "E3001"
     */
    public static String extrairValor(String mensagem, String chave) {
        if (mensagem == null || chave == null) return "";

        try {
            int inicio = mensagem.indexOf(chave);
            if (inicio == -1) return "";

            int start = inicio + chave.length();
            int fim = mensagem.indexOf(' ', start);
            if (fim == -1) fim = mensagem.length();

            String valor = mensagem.substring(start, fim).trim();
            return valor.replaceAll("[\\[\\],:]", ""); // Remove caracteres especiais

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Extrai tempo formatado da mensagem.
     * Exemplo: "tempo no sistema - 5.2s" → "5.2s"
     */
    public static String extrairTempo(String mensagem, String marcador) {
        if (mensagem == null || !mensagem.contains(marcador)) return "-";

        try {
            int idx = mensagem.indexOf(marcador);
            int inicio = mensagem.lastIndexOf("-", idx);

            if (inicio >= 0) {
                String parte = mensagem.substring(inicio + 1, idx).trim();
                return parte.replace("s", "") + "s";
            }
        } catch (Exception ignored) {}

        return "-";
    }

    /**
     * Verifica se mensagem contém palavra-chave (case-insensitive).
     */
    public static boolean contem(String mensagem, String palavra) {
        if (mensagem == null || palavra == null) return false;
        return mensagem.toLowerCase().contains(palavra.toLowerCase());
    }

    /**
     * Extrai entrada (E1, E2, E3) da mensagem.
     */
    public static String extrairEntrada(String mensagem) {
        if (mensagem == null) return null;

        if (mensagem.contains("E1")) return "E1";
        if (mensagem.contains("E2")) return "E2";
        if (mensagem.contains("E3")) return "E3";

        return null;
    }

    /**
     * Formata mensagem para display limpo.
     * Remove caracteres especiais e normaliza espaços.
     */
    public static String limparMensagem(String mensagem) {
        if (mensagem == null) return "";

        return mensagem
                .replaceAll("\\s+", " ")  // Múltiplos espaços → 1 espaço
                .replaceAll("[\\[\\]]", "") // Remove [ ]
                .trim();
    }

    /**
     * Valida se mensagem está no formato esperado.
     */
    public static boolean validarFormato(String mensagem) {
        return mensagem != null &&
                !mensagem.isEmpty() &&
                mensagem.length() < 500; // Proteção contra mensagens gigantes
    }
}