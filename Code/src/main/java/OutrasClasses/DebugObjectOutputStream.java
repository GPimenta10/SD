package OutrasClasses;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import Veiculo.Veiculo;

public class DebugObjectOutputStream extends ObjectOutputStream {
    private final String origemId;   // ex.: "Cr3"
    private final String destinoId;  // ex.: "S"

    public DebugObjectOutputStream(OutputStream out, String origemId, String destinoId) throws IOException {
        super(out);
        this.origemId = origemId;
        this.destinoId = destinoId;
        // NÃO fazer flush aqui
        System.out.printf("[WIRE-OUT][%s->%s] Stream criado%n", origemId, destinoId);
    }

    @Override
    protected void writeObjectOverride(Object obj) throws IOException {
        if (obj instanceof Veiculo v) {
            System.out.printf(
                    "[WIRE-OUT][%s->%s] writeObject Veiculo id=%s tipo=%s idx=%d caminho=%s%n",
                    origemId, destinoId, v.getId(), v.getTipo(), v.getIndiceCaminhoAtual(), v.getCaminho()
            );
        } else {
            System.out.printf(
                    "[WIRE-OUT][%s->%s] writeObject %s%n",
                    origemId, destinoId, (obj == null ? "null" : obj.getClass().getName())
            );
        }
        super.writeObjectOverride(obj);
    }

    @Override
    public void flush() throws IOException {
        System.out.printf("[WIRE-OUT][%s->%s] flush()%n", origemId, destinoId);
        super.flush();
    }
}
