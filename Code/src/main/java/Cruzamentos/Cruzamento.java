package Cruzamentos;

import com.google.gson.Gson;
import Rede.Mensagem;
import Veiculo.Veiculo;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe principal que representa um cruzamento do sistema.
 * Cada cruzamento pode ter 1..N semáforos, cada um com a sua fila.
 * Os semáforos partilham um lock e um índice ativo para garantir
 * que apenas um fica verde de cada vez.
 *
 * CORREÇÃO: Sistema automático de mapeamento origem -> fila
 */
public class Cruzamento {

    // === Identificação e comunicação ===
    private final String nomeCruzamento;
    private final int portaServidor;
    private final String ipDashboard;
    private final int portaDashboard;

    // === Estrutura interna ===
    private final List<FilaVeiculos> listaFilas = new ArrayList<>();
    private final List<Semaforo> listaSemaforos = new ArrayList<>();
    private final Map<String, ThreadCliente> mapaClientes = new HashMap<>();
    private final ThreadServidor threadServidor;
    private final ThreadDashboardCliente threadDashboardCliente;

    // === NOVO: Mapeamento automático de origens para filas ===
    // Quando um veículo chega de uma origem específica, sabemos para qual fila enviá-lo
    private final Map<String, FilaVeiculos> mapaOrigemParaFila = new HashMap<>();

    // === NOVO: Mapeamento de fila para destino ===
    // Cada fila está associada a um destino (para onde os veículos vão após passar)
    private final Map<FilaVeiculos, String> mapaFilaParaDestino = new HashMap<>();

    // === Sincronização entre semáforos ===
    private final Object lock = new Object();
    private final AtomicInteger semaforoAtivo = new AtomicInteger(0);

    // === Controlo ===
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    /**
     * Construtor principal.
     */
    public Cruzamento(String nomeCruzamento, int portaServidor, String ipDashboard, int portaDashboard) {
        this.nomeCruzamento = nomeCruzamento;
        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;

        this.threadServidor = new ThreadServidor(portaServidor, this);
        this.threadDashboardCliente = new ThreadDashboardCliente(ipDashboard, portaDashboard, this);
    }

    /**
     * Adiciona uma ligação (fila + semáforo + cliente) a outro cruzamento.
     *
     * @param nomeOrigem Nome do nó de onde os veículos chegam (ex: "E3", "Cr1")
     * @param nomeDestino Nome do cruzamento de destino (ex: "Cr3", "S")
     * @param ipDestino IP do destino
     * @param portaDestino Porta do destino
     */
    public void adicionarLigacao(String nomeOrigem, String nomeDestino, String ipDestino, int portaDestino) {
        // Cria a fila para esta ligação
        String nomeFila = "Fila_" + nomeOrigem + "->" + nomeCruzamento + "->" + nomeDestino;
        FilaVeiculos filaVeiculos = new FilaVeiculos();
        listaFilas.add(filaVeiculos);

        // Mapeia: veículos vindos de 'nomeOrigem' vão para esta fila
        mapaOrigemParaFila.put(nomeOrigem, filaVeiculos);

        // Mapeia: veículos desta fila vão para 'nomeDestino'
        mapaFilaParaDestino.put(filaVeiculos, nomeDestino);

        // Cria o semáforo para esta fila
        int idSemaforo = listaSemaforos.size();
        String nomeSemaforo = "Semaforo_" + nomeOrigem + "->" + nomeCruzamento + "->" + nomeDestino;
        Semaforo semaforo = new Semaforo(nomeSemaforo, filaVeiculos, 2000, lock, idSemaforo,
                    listaSemaforos.size() + 1, semaforoAtivo, this);
        listaSemaforos.add(semaforo);

        // Cria o cliente para enviar ao destino
        ThreadCliente threadCliente = new ThreadCliente(nomeCruzamento, nomeDestino, ipDestino, portaDestino);
        mapaClientes.put(nomeDestino, threadCliente);

        System.out.printf("[Cruzamento %s] Ligação adicionada: %s -> %s -> %s (%s:%d)%n",
                nomeCruzamento, nomeOrigem, nomeCruzamento, nomeDestino, ipDestino, portaDestino);
    }

    /**
     * Inicia todas as threads do cruzamento.
     */
    public void iniciar() {
        System.out.println("=== Cruzamento " + nomeCruzamento + " iniciado ===");
        System.out.printf("[Cruzamento %s] Total de filas/semáforos: %d%n",
                nomeCruzamento, listaFilas.size());

        threadServidor.start();
        threadDashboardCliente.start();

        for (ThreadCliente threadCliente : mapaClientes.values()) {
            threadCliente.start();
        }

        for (Semaforo semaforo : listaSemaforos) {
            semaforo.start();
        }
    }

    /**
     * Recebe um veículo vindo de outro nó (invocado pelo servidor).
     * Determina automaticamente para qual fila o veículo deve ir.
     *
     * @param veiculo Veículo recebido
     * @param nomeOrigem Nome do nó de onde o veículo veio
     */
    public void receberVeiculo(Veiculo veiculo, String nomeOrigem) {
        System.out.printf("[Cruzamento %s] Veículo recebido: %s (origem: %s)%n",
                nomeCruzamento, veiculo.getId(), nomeOrigem);

        // Busca a fila correspondente à origem
        FilaVeiculos filaCorreta = mapaOrigemParaFila.get(nomeOrigem);

        if (filaCorreta != null) {
            filaCorreta.adicionar(veiculo);
            System.out.printf("[Cruzamento %s] Veículo %s adicionado à fila de %s%n",
                    nomeCruzamento, veiculo.getId(), nomeOrigem);
        } else {
            System.err.printf("[Cruzamento %s] ERRO: Nenhuma fila mapeada para origem '%s'!%n",
                    nomeCruzamento, nomeOrigem);
            System.err.printf("[Cruzamento %s] Origens disponíveis: %s%n",
                    nomeCruzamento, mapaOrigemParaFila.keySet());
        }
    }

    /**
     * Envia um veículo ao próximo nó do seu caminho.
     * Chamado pelo semáforo após o veículo passar.
     *
     * @param veiculo Veículo a ser enviado
     * @param fila Fila de onde o veículo saiu (para determinar o destino)
     */
    public void enviarVeiculoAposPassarSemaforo(Veiculo veiculo, FilaVeiculos fila) {
        // Determina o destino baseado na fila
        String nomeDestino = mapaFilaParaDestino.get(fila);

        if (nomeDestino == null) {
            System.err.printf("[Cruzamento %s] ERRO: Destino não encontrado para a fila!%n",
                    nomeCruzamento);
            return;
        }

        // Avança o veículo no caminho
        veiculo.avancarCaminho();

        System.out.printf("[Cruzamento %s] Enviando veículo %s para %s%n",
                nomeCruzamento, veiculo.getId(), nomeDestino);

        // Envia ao destino
        enviarVeiculo(nomeDestino, veiculo);
    }

    /**
     * Envia um veículo ao cruzamento de destino.
     */
    public void enviarVeiculo(String nomeDestino, Veiculo veiculo) {
        ThreadCliente threadCliente = mapaClientes.get(nomeDestino);
        if (threadCliente != null) {
            threadCliente.enviarVeiculo(veiculo, nomeCruzamento);
        } else {
            System.err.printf("[Cruzamento %s] Destino '%s' não encontrado nos clientes!%n",
                    nomeCruzamento, nomeDestino);
            System.err.printf("[Cruzamento %s] Clientes disponíveis: %s%n",
                    nomeCruzamento, mapaClientes.keySet());
        }
    }

    /**
     * Gera um JSON com o estado atual das filas e semáforos (para o dashboard).
     */
    public String gerarEstatisticasJSON() {
        List<Map<String, Object>> listaInformacoesSemaforos = new ArrayList<>();

        for (int indice = 0; indice < listaSemaforos.size(); indice++) {
            Semaforo semaforo = listaSemaforos.get(indice);
            boolean semaforoAberto = (semaforoAtivo.get() == indice);

            Map<String, Object> informacaoSemaforo = new HashMap<>();
            informacaoSemaforo.put("nome", semaforo.getNome());
            informacaoSemaforo.put("estado", semaforoAberto ? "VERDE" : "VERMELHO");
            informacaoSemaforo.put("tamanhoFila", semaforo.getTamanhoFila());
            listaInformacoesSemaforos.add(informacaoSemaforo);
        }

        Map<String, Object> estadoCruzamento = new HashMap<>();
        estadoCruzamento.put("cruzamento", nomeCruzamento);
        estadoCruzamento.put("semaforos", listaInformacoesSemaforos);

        return gson.toJson(estadoCruzamento);
    }

    /**
     * Encerra todas as threads e recursos associados ao cruzamento.
     */
    public void parar() {
        ativo = false;

        for (Semaforo semaforo : listaSemaforos) {
            semaforo.parar();
        }

        for (ThreadCliente threadCliente : mapaClientes.values()) {
            threadCliente.parar();
        }

        threadServidor.pararServidor();
        threadDashboardCliente.parar();

        System.out.println("=== Cruzamento " + nomeCruzamento + " parado ===");
    }

    // === Getters ===
    public String getNomeCruzamento() { return nomeCruzamento; }
    public List<Semaforo> getListaSemaforos() { return listaSemaforos; }
}