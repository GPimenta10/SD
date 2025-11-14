package Veiculo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import PontosEntrada.PontoEntrada;

/**
 * Representa um veículo no sistema de tráfego urbano distribuído.
 * Cada veículo possui um identificador, tipo, ponto de entrada,
 * caminho a percorrer e tempos de chegada/saída do sistema.
 */
public class Veiculo implements Serializable {
    private static final long serialVersionUID = 1L;

    // === Dados fixos ===
    private final String id;
    private final TipoVeiculo tipo;
    private final PontoEntrada pontoEntrada;
    private final long tempoChegada;
    private final List<String> caminho;

    // === Estado dinâmico ===
    private long tempoSaida;
    private int indiceCaminhoAtual;

    // === Construtor ===
    public Veiculo(String id, TipoVeiculo tipo, PontoEntrada pontoEntrada, List<String> caminho) {
        this.id = id;
        this.tipo = tipo;
        this.pontoEntrada = pontoEntrada;
        this.tempoChegada = System.currentTimeMillis();
        this.tempoSaida = -1;
        this.caminho = new ArrayList<>(caminho);
        this.indiceCaminhoAtual = 0;
    }

    // === Getters (dados fixos) ===
    public String getId() {
        return id;
    }

    public TipoVeiculo getTipo() {
        return tipo;
    }

    public PontoEntrada getPontoEntrada() {
        return pontoEntrada;
    }

    public long getTempoChegada() {
        return tempoChegada;
    }

    public List<String> getCaminho() {
        return new ArrayList<>(caminho);
    }

    // === Getters e setters (estado dinâmico) ===
    public long getTempoSaida() {
        return tempoSaida;
    }

    public void setTempoSaida(long tempoSaida) {
        this.tempoSaida = tempoSaida;
    }

    public int getIndiceCaminhoAtual() {
        return indiceCaminhoAtual;
    }

    // === Lógica de percurso ===

    /** Retorna o próximo nó no caminho. */
    public String getProximoNo() {
        if (indiceCaminhoAtual < caminho.size()) {
            return caminho.get(indiceCaminhoAtual);
        }
        return "S"; // ponto de saída
    }

    /** Avança para o próximo nó do caminho. */
    public void avancarCaminho() {
        if (indiceCaminhoAtual < caminho.size()) {
            indiceCaminhoAtual++;
        }
    }

    /** Verifica se o veículo já chegou ao destino final. */
    public boolean chegouAoDestino() {
        return indiceCaminhoAtual >= caminho.size();
    }

    /**
     * Altera o próximo nó do caminho.
     * Útil quando um cruzamento redefine a rota.
     */
    public void alterarProximoNo(String no) {
        if (indiceCaminhoAtual < caminho.size()) {
            caminho.set(indiceCaminhoAtual, no);
        } else {
            caminho.add(no);
        }
    }

    // === Métricas e tempos ===

    /** Retorna o tempo total de permanência no sistema (dwelling time). */
    public long getDwellingTime() {
        return (tempoSaida == -1) ? -1 : tempoSaida - tempoChegada;
    }

    /** Calcula o tempo estimado de deslocamento com base no tipo de veículo. */
    public long calcularTempoDeslocamento(long tempoBaseCarro) {
        return (long) (tempoBaseCarro * tipo.getFatorVelocidade());
    }

    // === Representação textual ===
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
}
