package Dashboard.Utils;

public class EventoMovimento {
    public final String id;
    public final String tipo;
    public final String origem;
    public final String destino;

    public EventoMovimento(String id, String tipo, String origem, String destino) {
        this.id = id;
        this.tipo = tipo;
        this.origem = origem;
        this.destino = destino;
    }
}

