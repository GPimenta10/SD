package Utils;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GestorEstatisticas {

    // ------------------------------------------------------------------------
    //                       INTERFACE Ouvinte
    // ------------------------------------------------------------------------

    public interface OuvinteEstatisticas {
        void onEstatisticasGlobaisAtualizadas(EstatisticasGlobais globais);
        void onEstatisticasCruzamentoAtualizadas(String cruzamento, Map<String, EstatisticasFila> filas);
        void onEstatisticasSaidaAtualizadas(Map<String, EstatisticasSaida> estatisticasSaida);
        void onResumoCruzamentosAtualizado(Map<String, Map<String, Integer>> resumo);
    }

    private final List<OuvinteEstatisticas> ouvintes = new ArrayList<>();

    // ------------------------------------------------------------------------
    //                       ESTADO INTERNO
    // ------------------------------------------------------------------------

    private final Map<String, Integer> veiculosGeradosPorEntrada = new ConcurrentHashMap<>();
    private int totalSaidas = 0;

    private final Map<String, Map<String, EstatisticasFila>> estatisticasFilas = new ConcurrentHashMap<>();
    private final Map<String, EstatisticasSaida> estatisticasPorTipo = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Integer>> veiculosPorCruzamento = new ConcurrentHashMap<>();


    public GestorEstatisticas() {

        veiculosGeradosPorEntrada.put("E1", 0);
        veiculosGeradosPorEntrada.put("E2", 0);
        veiculosGeradosPorEntrada.put("E3", 0);

        estatisticasPorTipo.put("MOTA", new EstatisticasSaida());
        estatisticasPorTipo.put("CARRO", new EstatisticasSaida());
        estatisticasPorTipo.put("CAMIAO", new EstatisticasSaida());

        for (int i = 1; i <= 5; i++) {
            String cruz = "Cr" + i;

            estatisticasFilas.put(cruz, new ConcurrentHashMap<>());

            Map<String, Integer> tipos = new ConcurrentHashMap<>();
            tipos.put("MOTA", 0);
            tipos.put("CARRO", 0);
            tipos.put("CAMIAO", 0);
            veiculosPorCruzamento.put(cruz, tipos);
        }
    }

    // ------------------------------------------------------------------------
    //                  OUVINTES
    // ------------------------------------------------------------------------
    public synchronized void adicionarOuvinte(OuvinteEstatisticas o) {
        if (!ouvintes.contains(o)) ouvintes.add(o);
    }

    public synchronized void removerOuvinte(OuvinteEstatisticas o) {
        ouvintes.remove(o);
    }

    // ------------------------------------------------------------------------
    //                  REGISTO DE EVENTOS DO BACKEND
    // ------------------------------------------------------------------------

    public synchronized void registarVeiculoGerado(String entrada) {
        veiculosGeradosPorEntrada.merge(entrada, 1, Integer::sum);
        notificarEstatisticasGlobais();
    }

    /**
     * NOVO: Registo via JSON (DashboardFrame → GestorEstatisticas)
     */
    public synchronized void registarSaidaVeiculoJSON(JsonObject json) {

        String tipo = json.get("tipo").getAsString();
        long dwelling = json.get("dwelling").getAsLong();

        // Converter JsonArray → List<String>
        List<String> caminho = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("caminho");
        for (int i = 0; i < arr.size(); i++) caminho.add(arr.get(i).getAsString());

        registarVeiculoSaiu(tipo, dwelling, caminho);
    }

    /**
     * Versão tradicional (mantida)
     */
    public synchronized void registarVeiculoSaiu(String tipo,
                                                 long dwellingTimeSegundos,
                                                 List<String> caminho) {

        totalSaidas++;

        String tipoNorm = normalizarTipo(tipo);
        EstatisticasSaida stats = estatisticasPorTipo.get(tipoNorm);

        if (stats != null)
            stats.registar(dwellingTimeSegundos);

        if (caminho != null) {
            for (String cruz : caminho) {
                if (cruz.startsWith("Cr")) {
                    veiculosPorCruzamento.get(cruz)
                            .merge(tipoNorm, 1, Integer::sum);
                }
            }
        }

        notificarEstatisticasGlobais();
        notificarEstatisticasSaida();
        notificarResumoCruzamentos();
    }


    public synchronized void registarFilaAtualizada(String cruzamento, String semaforo, int tamanhoAtual) {

        Map<String, EstatisticasFila> filas =
                estatisticasFilas.computeIfAbsent(cruzamento, k -> new ConcurrentHashMap<>());

        EstatisticasFila fila = filas.computeIfAbsent(semaforo, k -> new EstatisticasFila());
        fila.registar(tamanhoAtual);

        notificarEstatisticasCruzamento(cruzamento);
    }

    // ------------------------------------------------------------------------
    //                     MÉTODOS DE CONSULTA
    // ------------------------------------------------------------------------

    public synchronized EstatisticasGlobais getEstatisticasGlobais() {
        return new EstatisticasGlobais(
                getTotalGerado(),
                veiculosGeradosPorEntrada.get("E1"),
                veiculosGeradosPorEntrada.get("E2"),
                veiculosGeradosPorEntrada.get("E3"),
                totalSaidas
        );
    }

    private int getTotalGerado() {
        return veiculosGeradosPorEntrada.values().stream().mapToInt(i -> i).sum();
    }

    public synchronized Map<String, EstatisticasFila> getEstatisticasCruzamento(String cruz) {
        return new HashMap<>(estatisticasFilas.getOrDefault(cruz, Map.of()));
    }

    public synchronized Map<String, EstatisticasSaida> getEstatisticasSaida() {
        return new HashMap<>(estatisticasPorTipo);
    }

    public synchronized Map<String, Map<String, Integer>> getResumoCruzamentos() {
        Map<String, Map<String, Integer>> copia = new HashMap<>();
        veiculosPorCruzamento.forEach((c, tipos) ->
                copia.put(c, new HashMap<>(tipos))
        );
        return copia;
    }

    // ------------------------------------------------------------------------
    //                     NOTIFICAÇÕES AOS OUVINTES
    // ------------------------------------------------------------------------

    private void notificarEstatisticasGlobais() {
        EstatisticasGlobais globais = getEstatisticasGlobais();
        for (OuvinteEstatisticas o : new ArrayList<>(ouvintes))
            o.onEstatisticasGlobaisAtualizadas(globais);
    }

    private void notificarEstatisticasCruzamento(String cruz) {
        Map<String, EstatisticasFila> filas = getEstatisticasCruzamento(cruz);
        for (OuvinteEstatisticas o : new ArrayList<>(ouvintes))
            o.onEstatisticasCruzamentoAtualizadas(cruz, filas);
    }

    private void notificarEstatisticasSaida() {
        Map<String, EstatisticasSaida> saida = getEstatisticasSaida();
        for (OuvinteEstatisticas o : new ArrayList<>(ouvintes))
            o.onEstatisticasSaidaAtualizadas(saida);
    }

    private void notificarResumoCruzamentos() {
        Map<String, Map<String, Integer>> resumo = getResumoCruzamentos();
        for (OuvinteEstatisticas o : new ArrayList<>(ouvintes))
            o.onResumoCruzamentosAtualizado(resumo);
    }

    // ------------------------------------------------------------------------
    //                      AUXILIARES
    // ------------------------------------------------------------------------

    private String normalizarTipo(String tipo) {
        return tipo.toUpperCase()
                .replace("CAMIÃO", "CAMIAO")
                .replace("CAMIÃ£O", "CAMIAO")
                .trim();
    }

    // ------------------------------------------------------------------------
    //                      DTOs
    // ------------------------------------------------------------------------

    public static class EstatisticasGlobais {
        public final int totalGerado, geradosE1, geradosE2, geradosE3, totalSaidas;

        public EstatisticasGlobais(int totalGerado, int e1, int e2, int e3, int saidas) {
            this.totalGerado = totalGerado;
            this.geradosE1 = e1;
            this.geradosE2 = e2;
            this.geradosE3 = e3;
            this.totalSaidas = saidas;
        }
    }

    public static class EstatisticasFila {
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
        public double getMedia() { return contagem > 0 ? (double)soma / contagem : 0; }
    }

    public static class EstatisticasSaida {
        private long min = Long.MAX_VALUE, max = 0, total = 0;
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
        public double getMedia() { return quantidade > 0 ? (double) total / quantidade : 0; }
    }
}
