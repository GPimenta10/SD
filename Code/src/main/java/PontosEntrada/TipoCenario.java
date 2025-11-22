package PontosEntrada;

/**
 * Enum que define os cenários de geração de caminhos disponíveis no sistema.
 *
 * ALEATORIO: Caminhos gerados aleatoriamente com distribuição probabilística
 * CAMINHO_CURTO: Caminhos mais curtos (hardcoded) para cada ponto de entrada
 */
public enum TipoCenario {
    ALEATORIO("Caminhos Aleatórios"),
    CAMINHO_CURTO("Caminho Mais Curto");

    private final String descricao;

    /**
     * Construtor do enum
     *
     * @param descricao Descrição legível do cenário
     */
    TipoCenario(String descricao) {
        this.descricao = descricao;
    }

    /**
     * Retorna a descrição legível do cenário
     *
     * @return Descrição do cenário
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Representação em string (usada em comboboxes)
     */
    @Override
    public String toString() {
        return descricao;
    }
}