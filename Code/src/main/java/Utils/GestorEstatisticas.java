package Utils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestor centralizado de estatísticas do sistema de tráfego.
 *
 * Esta classe funciona como intermediário entre:
 *  BACKEND: Cruzamentos, Geradores, Saída (enviam dados brutos via JSON/TCP)
 *  FRONTEND: Painéis Swing (apenas exibem informação processada)
 *
 * Responsabilidades:
 *  - Receber eventos brutos do backend (via ServidorDashboard)
 *  - Calcular estatísticas agregadas (média, mínimo, máximo, totais)
 *  - Fornecer dados processados ao frontend
 *  - Notificar ouvintes (Dashboard / painéis) quando as estatísticas mudam
 *  - Garantir thread-safety (múltiplas threads do ServidorDashboard)
 */
public class GestorEstatisticas {

    // ------------------------------------------------------------------------
    //                       INTERFACE DE LISTENER (PUSH)
    // ------------------------------------------------------------------------

    /**
     * Listener para receber notificações de atualização de estatísticas.
     * Implementação típica estará no DashboardFrame ou num controlador.
     */
    public interface OuvinteEstatisticas {
        /**
         * Estatísticas globais atualizadas (total gerado, por entrada, saídas).
         */
        void onEstatisticasGlobaisAtualizadas(EstatisticasGlobais globais);

        /**
         * Estatísticas de filas atualizadas para um cruzamento específico.
         */
        void onEstatisticasCruzamentoAtualizadas(String cruzamento,
                                                 Map<String, EstatisticasFila> filas);

        /**
         * Estatísticas de saída (dwelling time, quantidade) por tipo atualizadas.
         */
        void onEstatisticasSaidaAtualizadas(Map<String, EstatisticasSaida> estatisticasSaida);

        /**
         * Resumo de veículos por cruzamento e tipo atualizado.
         */
        void onResumoCruzamentosAtualizado(Map<String, Map<String, Integer>> resumo);
    }

    // Lista de ouvintes registados (UI / controllers)
    private final List<OuvinteEstatisticas> ouvintes = new ArrayList<>();

    // ------------------------------------------------------------------------
    //                              ESTADO INTERNO
    // ------------------------------------------------------------------------

    // Estatísticas Globais
    private final Map<String, Integer> veiculosGeradosPorEntrada = new ConcurrentHashMap<>();
    private int totalSaidas = 0;

    // Estatísticas de Filas (Cruzamento -> Semáforo -> Estatísticas)
    private final Map<String, Map<String, EstatisticasFila>> estatisticasFilas = new ConcurrentHashMap<>();

    // Estatísticas de Saída (Tipo -> Estatísticas)
    private final Map<String, EstatisticasSaida> estatisticasPorTipo = new ConcurrentHashMap<>();

    // Resumo de Cruzamentos (Cruzamento -> Tipo -> Quantidade)
    private final Map<String, Map<String, Integer>> veiculosPorCruzamento = new ConcurrentHashMap<>();

    /**
     * Construtor: inicializa mapas com entradas e tipos conhecidos.
     */
    public GestorEstatisticas() {
        veiculosGeradosPorEntrada.put("E1", 0);
        veiculosGeradosPorEntrada.put("E2", 0);
        veiculosGeradosPorEntrada.put("E3", 0);

        estatisticasPorTipo.put("MOTA", new EstatisticasSaida());
        estatisticasPorTipo.put("CARRO", new EstatisticasSaida());
        estatisticasPorTipo.put("CAMIAO", new EstatisticasSaida());

        for (int i = 1; i <= 5; i++) {
            String cruzamento = "Cr" + i;
            estatisticasFilas.put(cruzamento, new ConcurrentHashMap<>());

            Map<String, Integer> tipos = new ConcurrentHashMap<>();
            tipos.put("MOTA", 0);
            tipos.put("CARRO", 0);
            tipos.put("CAMIAO", 0);
            veiculosPorCruzamento.put(cruzamento, tipos);
        }
    }

    // ------------------------------------------------------------------------
    //                 REGISTO / REMOÇÃO DE OUVINTES (UI)
    // ------------------------------------------------------------------------

    /**
     * Adiciona um ouvinte que será notificado sempre que estatísticas forem atualizadas.
     */
    public synchronized void adicionarOuvinte(OuvinteEstatisticas ouvinte) {
        if (ouvinte != null && !ouvintes.contains(ouvinte)) {
            ouvintes.add(ouvinte);
        }
    }

    /**
     * Remove um ouvinte previamente registado.
     */
    public synchronized void removerOuvinte(OuvinteEstatisticas ouvinte) {
        ouvintes.remove(ouvinte);
    }

    // ------------------------------------------------------------------------
    //              MÉTODOS DE REGISTO (chamados pelo Backend)
    // ------------------------------------------------------------------------

    /**
     * Regista um veículo gerado numa entrada.
     * Chamado quando ServidorDashboard recebe mensagem "VEICULO_GERADO".
     *
     * @param entrada Identificador da entrada (E1, E2, E3)
     */
    public synchronized void registarVeiculoGerado(String entrada) {
        veiculosGeradosPorEntrada.merge(entrada, 1, Integer::sum);
        notificarEstatisticasGlobais();
    }

    /**
     * Regista um veículo que saiu do sistema.
     * Chamado quando ServidorDashboard recebe mensagem "VEICULO_SAIU".
     *
     * @param tipo Tipo do veículo (MOTA, CARRO, CAMIAO)
     * @param dwellingTimeSegundos Tempo total no sistema em segundos
     * @param caminho Lista de cruzamentos percorridos
     */
    public synchronized void registarVeiculoSaiu(String tipo,
                                                 long dwellingTimeSegundos,
                                                 List<String> caminho) {
        totalSaidas++;

        // Atualizar estatísticas de dwelling time por tipo
        EstatisticasSaida stats = estatisticasPorTipo.get(normalizarTipo(tipo));
        if (stats != null) {
            stats.registar(dwellingTimeSegundos);
        }

        // Atualizar contadores de cruzamentos percorridos
        if (caminho != null) {
            for (String cruzamento : caminho) {
                if (cruzamento.startsWith("Cr")) {
                    Map<String, Integer> tipos = veiculosPorCruzamento.get(cruzamento);
                    if (tipos != null) {
                        String tipoNorm = normalizarTipo(tipo);
                        tipos.merge(tipoNorm, 1, Integer::sum);
                    }
                }
            }
        }

        // Notificações relevantes
        notificarEstatisticasGlobais();
        notificarEstatisticasSaida();
        notificarResumoCruzamentos();
    }

    /**
     * Regista atualização do tamanho de uma fila num semáforo.
     * Chamado quando ServidorDashboard recebe mensagem "ESTATISTICA" com info de filas.
     *
     * @param cruzamento Identificador do cruzamento (Cr1, Cr2, ...)
     * @param semaforo Nome do semáforo (ex: "E1→Cr1")
     * @param tamanhoAtual Tamanho atual da fila
     */
    public synchronized void registarFilaAtualizada(String cruzamento, String semaforo, int tamanhoAtual) {
        Map<String, EstatisticasFila> filasCruzamento = estatisticasFilas.get(cruzamento);

        if (filasCruzamento == null) {
            filasCruzamento = new ConcurrentHashMap<>();
            estatisticasFilas.put(cruzamento, filasCruzamento);
        }

        EstatisticasFila stats = filasCruzamento.computeIfAbsent(
                semaforo,
                k -> new EstatisticasFila()
        );

        stats.registar(tamanhoAtual);

        notificarEstatisticasCruzamento(cruzamento);
    }

    // ------------------------------------------------------------------------
    //              MÉTODOS DE CONSULTA (chamados pelo Frontend)
    // ------------------------------------------------------------------------

    /**
     * Retorna estatísticas globais do sistema.
     * Usado por: PainelEstatsGlobais
     *
     * @return Objeto com estatísticas globais (cópia imutável)
     */
    public synchronized EstatisticasGlobais getEstatisticasGlobais() {
        return new EstatisticasGlobais(
                getTotalGerado(),
                veiculosGeradosPorEntrada.getOrDefault("E1", 0),
                veiculosGeradosPorEntrada.getOrDefault("E2", 0),
                veiculosGeradosPorEntrada.getOrDefault("E3", 0),
                totalSaidas
        );
    }

    /**
     * Retorna estatísticas de filas de um cruzamento específico.
     * Usado por: PainelEstatsCruzamentos
     *
     * @param cruzamento Identificador do cruzamento
     * @return Mapa (Semáforo -> Estatísticas) ou mapa vazio se cruzamento não existir
     */
    public synchronized Map<String, EstatisticasFila> getEstatisticasCruzamento(String cruzamento) {
        Map<String, EstatisticasFila> filas = estatisticasFilas.get(cruzamento);
        return filas != null ? new HashMap<>(filas) : new HashMap<>();
    }

    /**
     * Retorna estatísticas de saída agregadas por tipo de veículo.
     * Usado por: PainelEstatsSaida
     *
     * @return Mapa (Tipo -> Estatísticas) - cópia superficial (map), objetos internos partilhados
     */
    public synchronized Map<String, EstatisticasSaida> getEstatisticasSaida() {
        return new HashMap<>(estatisticasPorTipo);
    }

    /**
     * Retorna resumo de veículos por cruzamento e tipo.
     * Usado por: PainelResumoCruzamento
     *
     * @return Mapa (Cruzamento -> Tipo -> Quantidade) - cópia imutável
     */
    public synchronized Map<String, Map<String, Integer>> getResumoCruzamentos() {
        Map<String, Map<String, Integer>> copia = new HashMap<>();
        veiculosPorCruzamento.forEach((cruz, tipos) ->
                copia.put(cruz, new HashMap<>(tipos))
        );
        return copia;
    }

    /**
     * Retorna resumo de um cruzamento específico.
     *
     * @param cruzamento Identificador do cruzamento
     * @return Mapa (Tipo -> Quantidade) ou null se não existir
     */
    public synchronized Map<String, Integer> getResumoCruzamento(String cruzamento) {
        Map<String, Integer> resumo = veiculosPorCruzamento.get(cruzamento);
        return resumo != null ? new HashMap<>(resumo) : null;
    }

    // ------------------------------------------------------------------------
    //                      MÉTODOS AUXILIARES
    // ------------------------------------------------------------------------

    private int getTotalGerado() {
        return veiculosGeradosPorEntrada.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    /**
     * Normaliza tipos de veículo para a forma interna (sem acentos).
     */
    private String normalizarTipo(String tipo) {
        if (tipo == null) return "DESCONHECIDO";
        return tipo.toUpperCase()
                .replace("CAMIÃO", "CAMIAO")
                .replace("CAMIÃ£O", "CAMIAO")
                .trim();
    }

    // ------------------------------------------------------------------------
    //                  NOTIFICAÇÕES AOS OUVINTES (PUSH)
    // ------------------------------------------------------------------------

    private synchronized void notificarEstatisticasGlobais() {
        EstatisticasGlobais globais = getEstatisticasGlobais();
        List<OuvinteEstatisticas> copia = new ArrayList<>(ouvintes);

        for (OuvinteEstatisticas ouvinte : copia) {
            try {
                ouvinte.onEstatisticasGlobaisAtualizadas(globais);
            } catch (Exception e) {
                // Aqui poderias fazer log se quiseres
            }
        }
    }

    private synchronized void notificarEstatisticasCruzamento(String cruzamento) {
        Map<String, EstatisticasFila> filas = getEstatisticasCruzamento(cruzamento);
        List<OuvinteEstatisticas> copia = new ArrayList<>(ouvintes);

        for (OuvinteEstatisticas ouvinte : copia) {
            try {
                ouvinte.onEstatisticasCruzamentoAtualizadas(cruzamento, filas);
            } catch (Exception e) {}
        }
    }

    private synchronized void notificarEstatisticasSaida() {
        Map<String, EstatisticasSaida> saida = getEstatisticasSaida();
        List<OuvinteEstatisticas> copia = new ArrayList<>(ouvintes);

        for (OuvinteEstatisticas ouvinte : copia) {
            try {
                ouvinte.onEstatisticasSaidaAtualizadas(saida);
            } catch (Exception e) {}
        }
    }

    private synchronized void notificarResumoCruzamentos() {
        Map<String, Map<String, Integer>> resumo = getResumoCruzamentos();
        List<OuvinteEstatisticas> copia = new ArrayList<>(ouvintes);

        for (OuvinteEstatisticas ouvinte : copia) {
            try {
                ouvinte.onResumoCruzamentosAtualizado(resumo);
            } catch (Exception e) {
                // Log opcional
            }
        }
    }

    // ------------------------------------------------------------------------
    //                      CLASSES INTERNAS (DTOs)
    // ------------------------------------------------------------------------

    /**
     * DTO: Estatísticas Globais do Sistema
     * (Data Transfer Object - apenas dados, sem lógica)
     */
    public static class EstatisticasGlobais {
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

    /**
     * DTO: Estatísticas de uma Fila de Semáforo
     */
    public static class EstatisticasFila {
        private int atual = 0;
        private int minimo = Integer.MAX_VALUE;
        private int maximo = 0;
        private long soma = 0;
        private long contagem = 0;

        public void registar(int novoValor) {
            atual = novoValor;
            if (novoValor > maximo) maximo = novoValor;
            if (novoValor < minimo) minimo = novoValor;
            soma += novoValor;
            contagem++;
        }

        public int getAtual() { return atual; }
        public int getMinimo() { return minimo == Integer.MAX_VALUE ? 0 : minimo; }
        public int getMaximo() { return maximo; }

        public double getMedia() {
            return contagem > 0 ? (double) soma / contagem : 0.0;
        }
    }

    /**
     * DTO: Estatísticas de Saída por Tipo de Veículo
     */
    public static class EstatisticasSaida {
        private long dwellingTimeMin = Long.MAX_VALUE;
        private long dwellingTimeMax = 0;
        private long dwellingTimeTotal = 0;
        private int quantidade = 0;

        public void registar(long dwellingTime) {
            quantidade++;
            dwellingTimeTotal += dwellingTime;
            if (dwellingTime < dwellingTimeMin) dwellingTimeMin = dwellingTime;
            if (dwellingTime > dwellingTimeMax) dwellingTimeMax = dwellingTime;
        }

        public int getQuantidade() { return quantidade; }
        public long getMinimo() { return quantidade > 0 ? dwellingTimeMin : 0; }
        public long getMaximo() { return dwellingTimeMax; }

        public double getMedia() {
            return quantidade > 0 ? (double) dwellingTimeTotal / quantidade : 0.0;
        }
    }
}
