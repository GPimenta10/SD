package Veiculo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa um veículo que circula no sistema de tráfego.
 * Cada veículo possui informações sobre o seu percurso, tempos e registos de passagem.
 * Os tempos são armazenados em milissegundos (long) para facilitar cálculos.
 */
public class Veiculo implements Serializable {

    private static final long serialVersionUID = 1L;

    private String id;
    private TipoVeiculo tipo;
    private String origem;
    private List<String> percurso;

    // Timestamps principais
    private long tempoChegada;             // Quando entrou no sistema
    private long tempoSaida;               // Quando saiu do sistema
    private long timestampEntradaFila;     // Quando entrou numa fila de semáforo

    // Métricas adicionais
    private long tempoTotalEspera;         // Soma total do tempo em filas (ms)

    // Rastreio de passagens
    private List<String> cruzamentosPorOndePassou;
    private List<String> semaforosPorOndePassou;

    /**
     * Construtor principal.
     */
    public Veiculo(String id, TipoVeiculo tipo, String origem, List<String> percurso) {
        this.id = id;
        this.tipo = tipo;
        this.origem = origem;
        this.percurso = percurso;
        this.tempoChegada = System.currentTimeMillis();
        this.cruzamentosPorOndePassou = new ArrayList<>();
        this.semaforosPorOndePassou = new ArrayList<>();
        this.tempoTotalEspera = 0;
    }

    // ========== MÉTODOS PRINCIPAIS ==========

    /**
     * Regista a saída do sistema (define tempo de saída).
     */
    public void registrarSaida() {
        this.tempoSaida = System.currentTimeMillis();
    }

    /**
     * Regista a passagem por um cruzamento.
     */
    public void registrarPassagemCruzamento(String nomeCruzamento) {
        cruzamentosPorOndePassou.add(nomeCruzamento);
    }

    /**
     * Regista a passagem por um semáforo.
     */
    public void registrarPassagemSemaforo(String nomeSemaforo) {
        semaforosPorOndePassou.add(nomeSemaforo);
    }

    /**
     * Calcula o tempo total que o veículo permaneceu no sistema.
     */
    public long calcularTempoNoSistema() {
        return tempoSaida - tempoChegada;
    }

    /**
     * Calcula o tempo de deslocamento entre nós com base no tipo do veículo.
     * @param tempoBase tempo base de deslocamento (por exemplo, tcarro)
     */
    public long tempoDeslocamentoBase(long tempoBase) {
        return (long) (tempoBase * tipo.getFatorVelocidade());
    }

    /**
     * Adiciona tempo de espera (em milissegundos) ao total acumulado.
     */
    public void adicionarEspera(long duracao) {
        tempoTotalEspera += duracao;
    }

    // ========== GETTERS/SETTERS ==========

    public String getId() { return id; }
    public TipoVeiculo getTipo() { return tipo; }
    public String getOrigem() { return origem; }
    public List<String> getPercurso() { return percurso; }
    public long getTempoChegada() { return tempoChegada; }
    public long getTempoSaida() { return tempoSaida; }
    public long getTimestampEntradaFila() { return timestampEntradaFila; }
    public List<String> getCruzamentosPorOndePassou() { return cruzamentosPorOndePassou; }
    public List<String> getSemaforosPorOndePassou() { return semaforosPorOndePassou; }
    public long getTempoTotalEspera() { return tempoTotalEspera; }

    public void setTimestampEntradaFila(long timestamp) {
        this.timestampEntradaFila = timestamp;
    }

    // ========== REPRESENTAÇÃO EM STRING ==========

    @Override
    public String toString() {
        return "Veiculo{id='" + id + "', tipo=" + tipo + ", origem='" + origem + "', percurso=" + percurso + "}";
    }
}
