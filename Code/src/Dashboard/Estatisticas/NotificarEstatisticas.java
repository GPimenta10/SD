/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Classe responsável exclusivamente por gerir os ouvintes e notificar eventos.
 * Retira a responsabilidade de notificação da classe GestorEstatisticas.
 */
public class NotificarEstatisticas {

    private final List<ReceiverEstatisticas> ouvintes = new ArrayList<>();

    public synchronized void adicionarOuvinte(ReceiverEstatisticas o) {
        if (!ouvintes.contains(o)) {
            ouvintes.add(o);
        }
    }

    public synchronized void removerOuvinte(ReceiverEstatisticas o) {
        ouvintes.remove(o);
    }

    public void notificarGlobais(EstatisticasGlobais globais) {
        for (ReceiverEstatisticas o : new ArrayList<>(ouvintes)) {
            o.onEstatisticasGlobaisAtualizadas(globais);
        }
    }

    public void notificarCruzamento(String cruzamento, Map<String, EstatisticasFila> filas) {
        for (ReceiverEstatisticas o : new ArrayList<>(ouvintes)) {
            o.onEstatisticasCruzamentoAtualizadas(cruzamento, filas);
        }
    }

    public void notificarSaida(Map<String, EstatisticasSaida> saida) {
        for (ReceiverEstatisticas o : new ArrayList<>(ouvintes)) {
            o.onEstatisticasSaidaAtualizadas(saida);
        }
    }

    public void notificarResumo(Map<String, Map<String, Integer>> resumo) {
        for (ReceiverEstatisticas o : new ArrayList<>(ouvintes)) {
            o.onResumoCruzamentosAtualizado(resumo);
        }
    }
}