/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

/**
 * DTO que representa as estatísticas globais do sistema (totais gerados e saídos).
 */
public class EstatisticasGlobais {
    public final int totalGerado;
    public final int geradosE1;
    public final int geradosE2;
    public final int geradosE3;
    public final int totalSaidas;

    public EstatisticasGlobais(int totalGerado, int e1, int e2, int e3, int saidas) {
        this.totalGerado = totalGerado;
        this.geradosE1 = e1;
        this.geradosE2 = e2;
        this.geradosE3 = e3;
        this.totalSaidas = saidas;
    }
}
