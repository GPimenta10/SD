package Rede;

import com.google.gson.Gson;
import java.util.Map;

// Esta classe foi convertida para um RECORD, pois funciona essencialmente
// como um DTO (Data Transfer Object) usado para transporte de dados entre cruzamentos.
// Records reduzem drasticamente o boilerplate (construtor, getters, toString),
// reforçam a imutabilidade e tornam clara a intenção da classe.
//
// Mantém-se um construtor adicional para preencher automaticamente o timestamp.
// Os métodos toJson() e fromJson() continuam a funcionar normalmente.
//
// Em Java 17+, esta é a abordagem mais limpa e adequada para objetos de mensagem.
public record Mensagem(
        String tipo,
        String origem,
        String destino,
        Map<String, Object> conteudo,
        long timestamp
) {

    // Construtor secundário que atribui automaticamente o timestamp
    public Mensagem(String tipo, String origem, String destino, Map<String, Object> conteudo) {
        this(tipo, origem, destino, conteudo, System.currentTimeMillis());
    }

    // Serializar para JSON
    public String toJson() {
        return new Gson().toJson(this);
    }

    // Deserializar a partir de JSON
    public static Mensagem fromJson(String json) {
        return new Gson().fromJson(json, Mensagem.class);
    }
}