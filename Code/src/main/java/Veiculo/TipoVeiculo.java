package Veiculo;

public enum TipoVeiculo {
    MOTA(0.5),       // referência base
    CARRO(1.0),      // demora o dobro da mota
    CAMIAO(4.0);     // demora o dobro do carro (ou 4x a mota)

    private final double fatorVelocidade;

    TipoVeiculo(double fatorVelocidade) {
        this.fatorVelocidade = fatorVelocidade;
    }

    public double getFatorVelocidade() {
        return fatorVelocidade;
    }
}
