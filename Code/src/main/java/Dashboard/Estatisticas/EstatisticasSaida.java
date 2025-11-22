/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

/**
 * DTO responsável por manter as métricas de saída para um tipo de veículo.
 */
public class EstatisticasSaida {
    private long min = Long.MAX_VALUE;
    private long max = 0;
    private long total = 0;
    private int quantidade = 0;

    public void registar(long tempo) {
        quantidade++;
        total += tempo;
        if (tempo < min) min = tempo;
        if (tempo > max) max = tempo;
    }

    public int getQuantidade() { return quantidade; }
    public long getMinimo() { return quantidade > 0 ? min : 0; }
    public long getMaximo() { return max; }
    
    public double getMedia() { 
        return quantidade > 0 ? (double) total / quantidade : 0; 
    }
}
