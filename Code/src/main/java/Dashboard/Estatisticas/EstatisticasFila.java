/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

/**
 * DTO responsável por manter as métricas de uma fila de semáforo específica.
 */
public class EstatisticasFila {
    private int atual = 0;
    private int minimo = Integer.MAX_VALUE;
    private int maximo = 0;
    private long soma = 0;
    private long contagem = 0;

    public void registar(int novo) {
        atual = novo;
        if (novo > maximo) maximo = novo;
        if (novo < minimo) minimo = novo;
        soma += novo;
        contagem++;
    }

    public int getAtual() { return atual; }
    public int getMinimo() { return minimo == Integer.MAX_VALUE ? 0 : minimo; }
    public int getMaximo() { return maximo; }
    
    public double getMedia() { 
        return contagem > 0 ? (double)soma / contagem : 0; 
    }
}
