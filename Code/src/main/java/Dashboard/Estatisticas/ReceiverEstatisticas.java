package Dashboard.Estatisticas;

import java.util.Map;

/**
 * Interface para observadores que desejam receber atualizações das estatísticas.
 */
public interface ReceiverEstatisticas {
    void onEstatisticasGlobaisAtualizadas(EstatisticasGlobais globais);
    void onEstatisticasCruzamentoAtualizadas(String cruzamento, Map<String, EstatisticasFila> filas);
    void onEstatisticasSaidaAtualizadas(Map<String, EstatisticasSaida> estatisticasSaida);
    void onResumoCruzamentosAtualizado(Map<String, Map<String, Integer>> resumo);
}