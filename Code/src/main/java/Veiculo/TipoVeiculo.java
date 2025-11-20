package Veiculo;

/**
 * Enum que define os tipos de veículos no sistema
 * Relações de tempo respeitando o enunciado
 */
public enum TipoVeiculo {
    MOTA(0.5),      // Metade do tempo do carro
    CARRO(1.0),     // Tempo base
    CAMIAO(2.0);  // Dobro do tempo do carro (4 vezes o tempo da mota)

    private final double fatorVelocidade;

    /**
     * Construtor do enum
     * @param fator Fator multiplicador do tempo base
     */
    TipoVeiculo(double fator) {
        this.fatorVelocidade = fator;
    }

    /**
     * Retorna o fator de velocidade do veículo
     * Usado para calcular tempo de deslocamento
     */
    public double getFatorVelocidade() {
        return fatorVelocidade;
    }

}