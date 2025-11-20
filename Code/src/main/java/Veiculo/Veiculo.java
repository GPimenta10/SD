package Veiculo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import PontosEntrada.PontoEntrada;

/**
 * Representa um veículo no sistema de tráfego urbano distribuído.
 * Cada veículo possui um identificador único, tipo, ponto de entrada,
 * caminho a percorrer e tempos de chegada/saída do sistema.
 *
 */
public class Veiculo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final TipoVeiculo tipo;
    private final PontoEntrada pontoEntrada;
    private final long tempoChegada;
    private final List<String> caminho;

    private long tempoSaida;
    private int indiceCaminhoAtual;

    /**
     * Constrói um novo veículo com o caminho especificado.
     * O tempo de chegada é definido automaticamente para o momento da criação.
     *
     * @param id Identificador único do veículo
     * @param tipo Tipo do veículo (define comportamento de velocidade)
     * @param pontoEntrada Ponto de entrada do veículo no sistema
     * @param caminho Lista ordenada de nós que o veículo deve percorrer
     * @throws IllegalArgumentException se algum parâmetro for null ou caminho vazio
     */
    public Veiculo(String id, TipoVeiculo tipo, PontoEntrada pontoEntrada, List<String> caminho) {
        if (id == null || tipo == null || pontoEntrada == null || caminho == null || caminho.isEmpty()) {
            throw new IllegalArgumentException("Todos os parâmetros devem ser não-nulos e caminho não pode ser vazio");
        }

        this.id = id;
        this.tipo = tipo;
        this.pontoEntrada = pontoEntrada;
        this.tempoChegada = System.currentTimeMillis();
        this.tempoSaida = -1;
        this.caminho = new ArrayList<>(caminho);
        this.indiceCaminhoAtual = 0;
    }

    /**
     * Retorna o identificador único do veículo.
     *
     * @return ID do veículo
     */
    public String getId() {
        return id;
    }

    /**
     * Retorna o tipo do veículo.
     *
     * @return Tipo do veículo
     */
    public TipoVeiculo getTipo() {
        return tipo;
    }

    /**
     * Retorna o ponto de entrada do veículo no sistema.
     *
     * @return Ponto de entrada
     */
    public PontoEntrada getPontoEntrada() {
        return pontoEntrada;
    }

    /**
     * Retorna uma cópia do caminho completo que o veículo deve percorrer.
     *
     * @return Lista com os identificadores dos nós do caminho
     */
    public List<String> getCaminho() {
        return new ArrayList<>(caminho); // Cópia defensiva
    }

    /**
     * Retorna o timestamp de chegada do veículo ao sistema.
     *
     * @return Tempo de chegada em milissegundos (epoch time)
     */
    public long getTempoChegada() {
        return tempoChegada;
    }

    /**
     * Retorna o identificador do próximo nó a ser visitado.
     * Se o veículo já completou o caminho, retorna "S" (saída).
     *
     * @return ID do próximo nó ou "S" se chegou ao destino
     */
    public String getProximoNo() {
        if (indiceCaminhoAtual < caminho.size()) {
            return caminho.get(indiceCaminhoAtual);
        }
        return "S";
    }

    /**
     * Define o tempo de saída do veículo do sistema.
     *
     * @param tempoSaida Timestamp de saída em milissegundos
     */
    public void setTempoSaida(long tempoSaida) {
        this.tempoSaida = tempoSaida;
    }

    /**
     * Avança o veículo para o próximo nó do caminho.
     * Incrementa o índice da posição atual.
     */
    public void avancarCaminho() {
        if (indiceCaminhoAtual < caminho.size()) {
            indiceCaminhoAtual++;
        }
    }

    /**
     * Retorna uma representação textual do veículo com informações principais.
     *
     * @return String formatada com ID (truncado se necessário), tipo, entrada e progresso
     */
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