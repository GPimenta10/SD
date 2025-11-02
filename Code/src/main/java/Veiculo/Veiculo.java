package Veiculo;

import java.io.Serializable;
import java.util.List;
import java.util.ArrayList;

import PontosEntrada.PontoEntrada;

/**
 * Representa um veículo no sistema de tráfego
 * Serializável para transmissão via sockets
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
     *
     *
     * @param id ID do Veiculo
     * @param tipo Tipo de Veiculo
     * @param pontoEntrada Ponto pelo qual entrou no sistema
     * @param caminho Caminho percorrido desde que entrou até que saiu
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
     *  Get ID
     *
     * @return Retorna o ID do veículo
     */
    public String getId() {
        return id;
    }

    /**
     * Get TIPO
     *
     * @return Retorna o Tipo do veículo
     */
    public TipoVeiculo getTipo() {
        return tipo;
    }

    /**
     * GET PontoEntrada
     *
     * @return Retorna o Ponto de entrada do veículo
     */
    public PontoEntrada getPontoEntrada() {
        return pontoEntrada;
    }

    /**
     * GET TempoChegada
     *
     * @return Retorna o instante em que o veículo chegou
     */
    public long getTempoChegada() {
        return tempoChegada;
    }

    /**
     * GET TempoSaida
     *
     * @return Retorna o instante em que o veículo saiu
     */
    public long getTempoSaida() {
        return tempoSaida;
    }

    /**
     * Get Caminho
     *
     * @return Retorna o caminho percorrido pelo veículo
     */
    public List<String> getCaminho() {
        return new ArrayList<>(caminho);
    }

    /**
     * Get DwellingTime
     *
     * @return Retorna o tempo total no sistema (dwelling time)
     */
    public long getDwellingTime() {
        if (tempoSaida == -1) {
            return -1;
        }
        return tempoSaida - tempoChegada;
    }

    /**
     * Get ProximoNO
     *
     * @return Retorna o próximo nó no caminho
     */
    public String getProximoNo() {
        if (caminho.size() > 1)
            return caminho.get(1); // o próximo nó após o atual
        else
            return caminho.get(0);
    }

    /**
     * Set ID
     *
     * @param id Atribui um ID ao veículo
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Marca o tempo de saída do sistema
     *
     * @param tempoSaida Instante em que o veículo saiu do sistema
     */
    public void setTempoSaida(long tempoSaida) {
        this.tempoSaida = tempoSaida;
    }

    /**
     * Avança para o próximo nó do caminho
     *
     */
    public void avancarCaminho() {
        indiceCaminhoAtual++;
    }

    /**
     * Verifica se o veículo já chegou ao destino final do seu percurso.
     *
     * O veículo mantém internamente um índice (`indiceCaminhoAtual`)
     * que representa a sua posição atual na lista de nós (`caminho`).
     * Quando este índice atinge ou ultrapassa o tamanho da lista,
     * significa que o veículo completou o trajeto definido.
     *
     * @return True se o veículo chegou ao destino final; false caso contrário
     */
    public boolean chegouAoDestino() {
        return indiceCaminhoAtual >= caminho.size();
    }

    /**
     * Calcula o tempo estimado de deslocamento do veículo
     * com base no tipo e num tempo base de referência.
     *
     * O resultado é o tempo de deslocamento ajustado
     * multiplicando o tempo base pelo fator do tipo de veículo.
     *
     * @param tempoBaseCarro tempo de deslocamento base (em milissegundos) assumindo um carro como referência
     * @return tempo ajustado de deslocamento para este veículo (em milissegundos)
     */
    public long calcularTempoDeslocamento(long tempoBaseCarro) {
        return (long) (tempoBaseCarro * tipo.getFatorVelocidade());
    }

    /**
     * Metodo toString
     *
     * @return Informação do veículo como String
     */
    @Override
    public String toString() {
        return String.format("Veiculo[id=%s, tipo=%s, entrada=%s, caminho=%s]",
                id.substring(0, 8), tipo, pontoEntrada, caminho);
    }
}