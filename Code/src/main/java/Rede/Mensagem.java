package Rede;

import com.google.gson.Gson;
import java.util.Map;

public class Mensagem {

    private String tipo;          // Ex: "VEICULO", "ESTATISTICA", "CONTROLO"
    private String origem;        // Ex: "Cr1"
    private String destino;       // Ex: "Cr2" ou "Dashboard"
    private Map<String, Object> conteudo; // Dados adicionais (veículo, fila, etc.)
    private long timestamp;       // Momento de envio (para debug)

    // Construtor principal
    public Mensagem(String tipo, String origem, String destino, Map<String, Object> conteudo) {
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
        this.conteudo = conteudo;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters
    public String getTipo() { return tipo; }
    public String getOrigem() { return origem; }
    public String getDestino() { return destino; }
    public Map<String, Object> getConteudo() { return conteudo; }
    public long getTimestamp() { return timestamp; }

    // Converte o objeto para JSON (para envio)
    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    // Reconstrói o objeto a partir de JSON (ao receber)
    public static Mensagem fromJson(String json) {
        Gson gson = new Gson();
        return gson.fromJson(json, Mensagem.class);
    }

    // Para debug
    @Override
    public String toString() {
        return "Mensagem{" +
                "tipo='" + tipo + '\'' +
                ", origem='" + origem + '\'' +
                ", destino='" + destino + '\'' +
                ", conteudo=" + conteudo +
                ", timestamp=" + timestamp +
                '}';
    }
}

