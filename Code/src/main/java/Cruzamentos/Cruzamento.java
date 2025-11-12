package Cruzamentos;

import com.google.gson.Gson;
import Rede.Mensagem;
import Veiculo.Veiculo;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Classe principal que representa um cruzamento do sistema.
 * ATUALIZADO: Notifica Dashboard dos movimentos de veículos
 */
public class Cruzamento {

    private final String nomeCruzamento;
    private final int portaServidor;
    private final String ipDashboard;
    private final int portaDashboard;

    private final List<FilaVeiculos> listaFilas = new ArrayList<>();
    private final List<Semaforo> listaSemaforos = new ArrayList<>();
    private final Map<String, ThreadCliente> mapaClientes = new HashMap<>();
    private final ThreadServidor threadServidor;
    private final ThreadDashboardCliente threadDashboardCliente;

    private final Map<String, FilaVeiculos> mapaOrigemParaFila = new HashMap<>();
    private final Map<FilaVeiculos, String> mapaFilaParaDestino = new HashMap<>();

    private final Object lock = new Object();
    private final AtomicInteger semaforoAtivo = new AtomicInteger(0);

    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    public Cruzamento(String nomeCruzamento, int portaServidor, String ipDashboard, int portaDashboard) {
        this.nomeCruzamento = nomeCruzamento;
        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;

        this.threadServidor = new ThreadServidor(portaServidor, this);
        this.threadDashboardCliente = new ThreadDashboardCliente(ipDashboard, portaDashboard, this);
    }

    public void adicionarLigacao(String nomeOrigem, String nomeDestino, String ipDestino, int portaDestino) {
        String nomeFila = "Fila_" + nomeOrigem + "->" + nomeCruzamento + "->" + nomeDestino;
        FilaVeiculos filaVeiculos = new FilaVeiculos();
        listaFilas.add(filaVeiculos);

        mapaOrigemParaFila.put(nomeOrigem, filaVeiculos);
        mapaFilaParaDestino.put(filaVeiculos, nomeDestino);

        int idSemaforo = listaSemaforos.size();
        String nomeSemaforo = "Semaforo_" + nomeOrigem + "->" + nomeCruzamento + "->" + nomeDestino;
        Semaforo semaforo = new Semaforo(nomeSemaforo, filaVeiculos, 2000, lock, idSemaforo,
                listaSemaforos.size() + 1, semaforoAtivo, this);
        listaSemaforos.add(semaforo);

        ThreadCliente threadCliente = new ThreadCliente(nomeCruzamento, nomeDestino, ipDestino, portaDestino);
        mapaClientes.put(nomeDestino, threadCliente);

        System.out.printf("[Cruzamento %s] Ligação adicionada: %s -> %s -> %s (%s:%d)%n",
                nomeCruzamento, nomeOrigem, nomeCruzamento, nomeDestino, ipDestino, portaDestino);
    }

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

    public void receberVeiculo(Veiculo veiculo, String nomeOrigem) {
        System.out.printf("[Cruzamento %s] Veículo recebido: %s (origem: %s)%n",
                nomeCruzamento, veiculo.getId(), nomeOrigem);

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
     * ATUALIZADO: Agora notifica o Dashboard do movimento
     */
    public void enviarVeiculoAposPassarSemaforo(Veiculo veiculo, FilaVeiculos fila) {
        String nomeDestino = mapaFilaParaDestino.get(fila);

        if (nomeDestino == null) {
            System.err.printf("[Cruzamento %s] ERRO: Destino não encontrado para a fila!%n",
                    nomeCruzamento);
            return;
        }

        veiculo.avancarCaminho();

        System.out.printf("[Cruzamento %s] Enviando veículo %s para %s%n",
                nomeCruzamento, veiculo.getId(), nomeDestino);

        // NOVO: Notifica Dashboard do movimento
        notificarMovimentoDashboard(veiculo.getId(), veiculo.getTipo().name(), nomeDestino);

        enviarVeiculo(nomeDestino, veiculo);
    }

    /**
     * NOVO: Notifica o Dashboard do movimento de um veículo entre nós
     */
    private void notificarMovimentoDashboard(String idVeiculo, String tipo, String destino) {
        try (Socket socket = new Socket(ipDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> conteudo = new HashMap<>();
            conteudo.put("id", idVeiculo);
            conteudo.put("tipo", tipo);
            conteudo.put("origem", nomeCruzamento);
            conteudo.put("destino", destino);

            Mensagem msg = new Mensagem(
                    "VEICULO_MOVIMENTO",
                    nomeCruzamento,
                    "Dashboard",
                    conteudo
            );

            out.println(gson.toJson(msg));

        } catch (Exception e) {
            // Falha silenciosa para não interromper o fluxo
            System.err.printf("[Cruzamento %s] Erro ao notificar movimento: %s%n",
                    nomeCruzamento, e.getMessage());
        }
    }

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

    public String getNomeCruzamento() { return nomeCruzamento; }
    public List<Semaforo> getListaSemaforos() { return listaSemaforos; }
}