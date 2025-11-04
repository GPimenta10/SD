package Veiculo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import PontosEntrada.PontoEntrada;

/**
 * Representa um veículo no sistema de tráfego
 *
 */
public class Veiculo implements Serializable {
    private static final long serialVersionUID = 1L;

    // Atributos do veículo
    private String id;
    private final TipoVeiculo tipo;
    private final PontoEntrada pontoEntrada;
    private final long tempoChegada;
    private long tempoSaida;
    private final List<String> caminho;
    private int indiceCaminhoAtual;

    /**
     * Construtor do Veiculo
     */
    public Veiculo(String id, TipoVeiculo tipo, PontoEntrada pontoEntrada, List<String> caminho) {
        this.id = id;
        this.tipo = tipo;
        this.pontoEntrada = pontoEntrada;
        this.tempoChegada = System.currentTimeMillis();
        this.tempoSaida = -1;
        this.caminho = new ArrayList<>(caminho);
        this.indiceCaminhoAtual = 0;
    }

    /**
     *
     * @return
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return
     */
    public TipoVeiculo getTipo() {
        return tipo;
    }

    /**
     *
     * @return
     */
    public PontoEntrada getPontoEntrada() {
        return pontoEntrada;
    }

    /**
     *
     * @return
     */
    public long getTempoChegada() {
        return tempoChegada;
    }

    /**
     *
     * @return
     */
    public long getTempoSaida() {
        return tempoSaida;
    }

    /**
     *
     * @return
     */
    public List<String> getCaminho() {
        return new ArrayList<>(caminho);
    }

    /**
     * Retorna o tempo total no sistema (dwelling time)
     *
     * @return
     */
    public long getDwellingTime() {
        if (tempoSaida == -1) {
            return -1;
        }
        return tempoSaida - tempoChegada;
    }

    /**
     * Retorna o próximo nó no caminho.
     *
     * @return
     */
    public String getProximoNo() {
        if (indiceCaminhoAtual < caminho.size()) {
            return caminho.get(indiceCaminhoAtual);
        } else {
            // Já chegou ao destino final
            return "S"; // ou lançar exceção, depende da lógica
        }
    }

    /**
     *
     *
     * @param id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     *
     *
     * @param tempoSaida
     */
    public void setTempoSaida(long tempoSaida) {
        this.tempoSaida = tempoSaida;
    }

    /**
     * Avança para o próximo nó do caminho.
     */
    public void avancarCaminho() {
        if (indiceCaminhoAtual < caminho.size()) {
            indiceCaminhoAtual++;
        }
    }

    /**
     * Verifica se o veículo já chegou ao destino final.
     */
    public boolean chegouAoDestino() {
        return indiceCaminhoAtual >= caminho.size();
    }

    /**
     * Calcula o tempo estimado de deslocamento do veículo.
     */
    public long calcularTempoDeslocamento(long tempoBaseCarro) {
        return (long) (tempoBaseCarro * tipo.getFatorVelocidade());
    }

    /**
     * Retorna o índice atual no caminho (para debug/estatísticas).
     */
    public int getIndiceCaminhoAtual() {
        return indiceCaminhoAtual;
    }

    @Override
    public String toString() {
        return String.format("Veiculo[id=%s, tipo=%s, entrada=%s, caminho=%s, posição=%d/%d]",
                id.length() > 8 ? id.substring(0, 8) : id,
                tipo,
                pontoEntrada,
                caminho,
                indiceCaminhoAtual,
                caminho.size());
    }

    /**
     * Restaura o estado do objeto após desserialização.
     */
    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();

        // Garante que o índice não aponte novamente para o ponto atual
        if (indiceCaminhoAtual == 0 && caminho.size() > 1) {
            indiceCaminhoAtual = 1;
        }
    }
}