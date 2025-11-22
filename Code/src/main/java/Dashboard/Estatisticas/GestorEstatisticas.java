/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Estatisticas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Gestor central de estatísticas.
 * Refatorizado para delegar DTOs e notificações, focando apenas na lógica de negócio.
 */
public class GestorEstatisticas {

    // Componente responsável pelas notificações (Extração de responsabilidade)
    private final NotificarEstatisticas notificador = new NotificarEstatisticas();

    // Estado Interno
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

    // ========================================================================
    // Delegar gestão de ouvintes para o Notificador
    // ========================================================================
    
    /**
     * Adiciona um ouvinte para receber atualizações das estatísticas.
     * 
     * @param o Ouvinte a ser adicionado
     */
    public void adicionarOuvinte(ReceiverEstatisticas o) {
        notificador.adicionarOuvinte(o);
    }

    /**
     * Remove um ouvinte das atualizações das estatísticas.
     * 
     * @param o Ouvinte a ser removido
     */
    public void removerOuvinte(ReceiverEstatisticas o) {
        notificador.removerOuvinte(o);
    }

    // ========================================================================
    // Lógica de Negócio
    // ========================================================================

    /**
     * Regista a geração de um veículo numa entrada específica.
     * 
     * @param entrada Nome da entrada onde o veículo foi gerado
     */
    public synchronized void registarVeiculoGerado(String entrada) {
        veiculosGeradosPorEntrada.merge(entrada, 1, Integer::sum);
        notificador.notificarGlobais(getEstatisticasGlobais());
    }

    /**
     * Regista a saída de um veículo com dados em formato JSON.
     * 
     * @param json Objeto JSON contendo os dados do veículo
     */
    public synchronized void registarSaidaVeiculoJSON(JsonObject json) {
        String tipo = json.get("tipo").getAsString();
        long dwelling = json.get("dwelling").getAsLong();

        List<String> caminho = new ArrayList<>();
        JsonArray arr = json.getAsJsonArray("caminho");
        for (int i = 0; i < arr.size(); i++) caminho.add(arr.get(i).getAsString());

        registarVeiculoSaiu(tipo, dwelling, caminho);
    }

    /**
     * Regista a saída de um veículo.
     * 
     * @param tipo Tipo do veículo
     * @param dwellingTimeSegundos Tempo de permanência em segundos
     * @param caminho Lista de cruzamentos percorridos
     */
    public synchronized void registarVeiculoSaiu(String tipo, long dwellingTimeSegundos, List<String> caminho) {
        totalSaidas++;

        String tipoNorm = normalizarTipo(tipo);
        EstatisticasSaida stats = estatisticasPorTipo.get(tipoNorm);

        if (stats != null) {
            stats.registar(dwellingTimeSegundos);
        }

        if (caminho != null) {
            for (String cruz : caminho) {
                if (cruz.startsWith("Cr")) {
                    veiculosPorCruzamento.get(cruz).merge(tipoNorm, 1, Integer::sum);
                }
            }
        }

        notificador.notificarGlobais(getEstatisticasGlobais());
        notificador.notificarSaida(getEstatisticasSaida());
        notificador.notificarResumo(getResumoCruzamentos());
    }

    /**
     * Regista atualização do tamanho da fila num semáforo de um cruzamento.
     * 
     * @param cruzamento Nome do cruzamento
     * @param semaforo   Nome do semáforo
     * @param tamanhoAtual Tamanho atual da fila
     */
    public synchronized void registarFilaAtualizada(String cruzamento, String semaforo, int tamanhoAtual) {
        Map<String, EstatisticasFila> filas = estatisticasFilas.computeIfAbsent(cruzamento, k -> new ConcurrentHashMap<>());
        EstatisticasFila fila = filas.computeIfAbsent(semaforo, k -> new EstatisticasFila());
        
        fila.registar(tamanhoAtual);

        notificador.notificarCruzamento(cruzamento, getEstatisticasCruzamento(cruzamento));
    }

    // ========================================================================
    // Métodos de Consulta (Getters)
    // ========================================================================

    public synchronized EstatisticasGlobais getEstatisticasGlobais() {
        return new EstatisticasGlobais(
                getTotalGerado(),
                veiculosGeradosPorEntrada.get("E1"),
                veiculosGeradosPorEntrada.get("E2"),
                veiculosGeradosPorEntrada.get("E3"),
                totalSaidas
        );
    }

    /**
     * Usa Streams API para somar o total de veículos gerados.
     */
    private int getTotalGerado() {
        return veiculosGeradosPorEntrada.values().stream()
                .mapToInt(Integer::intValue)
                .sum();
    }

    public synchronized Map<String, EstatisticasFila> getEstatisticasCruzamento(String cruz) {
        return new HashMap<>(estatisticasFilas.getOrDefault(cruz, Map.of()));
    }

    public synchronized Map<String, EstatisticasSaida> getEstatisticasSaida() {
        return new HashMap<>(estatisticasPorTipo);
    }

    public synchronized Map<String, Map<String, Integer>> getResumoCruzamentos() {
        Map<String, Map<String, Integer>> copia = new HashMap<>();
        veiculosPorCruzamento.forEach((c, tipos) -> copia.put(c, new HashMap<>(tipos)));
        return copia;
    }
    
    /**
    * Normaliza o tipo de veículo para consistência interna.
    * 
    * @param tipo Tipo de veículo original
    * @return Tipo de veículo normalizado
    */
    private String normalizarTipo(String tipo) {
        return tipo.toUpperCase()
                .replace("CAMIÃO", "CAMIAO")
                .replace("CAMIÃ£O", "CAMIAO")
                .trim();
    }
}