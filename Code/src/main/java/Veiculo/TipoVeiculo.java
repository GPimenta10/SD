package Veiculo;

/**
 * Enum que define os tipos de veículos no sistema
 * Cada tipo tem um fator de velocidade diferente
 *
 * Relações de tempo:
 * - tmoto = 0.5 × tcarro
 * - tcaminhao = 4 × tmoto = 2 × tcarro
 */
public enum TipoVeiculo {
    MOTA(0.5),      // Mais rápido: metade do tempo
    CARRO(1.0),     // Tempo base (referência)
    CAMIAO(2.0);  // Mais lento: dobro do tempo

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

    /**
     * Calcula o tempo de deslocamento para este tipo
     * @param tempoBaseCarro Tempo base de um carro
     * @return Tempo de deslocamento ajustado
     */
    public long calcularTempoDeslocamento(long tempoBaseCarro) {
        return (long) (tempoBaseCarro * fatorVelocidade);
    }
}