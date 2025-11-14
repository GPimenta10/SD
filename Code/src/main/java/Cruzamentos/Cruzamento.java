package Cruzamentos;

import Rede.Mensagem;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

/**
 * Classe Cruzamento responsável por:
 *  - Criar e gerir filas de veículos
 *  - Criar semáforos associados a cada fila
 *  - Enviar veículos para outros cruzamentos via TCP
 *  - Receber veículos de outros cruzamentos
 *  - Comunicar periodicamente com o Dashboard (opcional)
 *
 *  NOTA IMPORTANTE:
 *  Os semáforos são simples: apenas retiram veículos da fila.
 *  Quem envia os veículos via socket é o Cruzamento.
 */
public class Cruzamento {

    private final String nomeCruzamento;

    // Comunicação entre cruzamentos
    private final int portaServidor;
    private final Map<String, ThreadCliente> mapaDestinoParaCliente = new HashMap<>();
    private ThreadServidor threadServidor;

    // Dashboard (opcional)
    private final String ipDashboard;
    private final int portaDashboard;
    private ThreadDashboardCliente threadDashboardCliente;

    // Fila → Destino e Origem → Fila
    private final Map<String, FilaVeiculos> mapaOrigemParaFila = new HashMap<>();
    private final Map<FilaVeiculos, String> mapaFilaParaDestino = new HashMap<>();

    // Semáforos
    private final List<Semaforo> listaSemaforos = new ArrayList<>();
    private MonitorSemaforos monitorSemaforos;

    private final Gson gson = new Gson();

    public Cruzamento(String nomeCruzamento, int portaServidor, String ipDashboard, int portaDashboard) {
        this.nomeCruzamento = nomeCruzamento;
        this.portaServidor = portaServidor;

        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;
    }

    public String getNomeCruzamento() {
        return nomeCruzamento;
    }

    /**
     * Define uma ligação ao cruzamento seguinte.
     *
     * @param origem        Nome da entrada deste cruzamento
     * @param destino       Nome do cruzamento seguinte
     * @param ipDestino     Endereço IP do cruzamento seguinte
     * @param portaDestino  Porta TCP do cruzamento seguinte
     */
    public void adicionarLigacao(String origem, String destino, String ipDestino, int portaDestino) {

        System.out.printf("[%s] Configurar ligação: %s → %s%n",
                nomeCruzamento, origem, destino);

        // Cria fila para esta origem
        FilaVeiculos fila = new FilaVeiculos();
        mapaOrigemParaFila.put(origem, fila);
        mapaFilaParaDestino.put(fila, destino);

        // Apenas 1 cliente TCP por destino
        if (!mapaDestinoParaCliente.containsKey(destino)) {
            ThreadCliente cliente = new ThreadCliente(nomeCruzamento, destino, ipDestino, portaDestino);
            mapaDestinoParaCliente.put(destino, cliente);
        }
    }

    /**
     *
     *
     */
    public void iniciar() {

        System.out.println("=".repeat(60));
        System.out.printf(">>> INICIAR CRUZAMENTO %s%n", nomeCruzamento);
        System.out.println("=".repeat(60));

        // Criar monitor para os semáforos
        monitorSemaforos = new MonitorSemaforos(mapaOrigemParaFila.size());

        // Criar Semáforos
        int idSemaforo = 0;
        for (Map.Entry<String, FilaVeiculos> entry : mapaOrigemParaFila.entrySet()) {

            String origem = entry.getKey();
            FilaVeiculos filaVeiculos = entry.getValue();

            Semaforo semaforo = new Semaforo(
                    idSemaforo,
                    monitorSemaforos,
                    3000,                 // tempo de verde
                    filaVeiculos,         // fila associada
                    this                  // referência ao cruzamento
            );

            listaSemaforos.add(semaforo);
            idSemaforo++;
        }

        // Iniciar servidor TCP
        threadServidor = new ThreadServidor(portaServidor, this);
        threadServidor.start();

        // Iniciar clientes TCP
        for (ThreadCliente cliente : mapaDestinoParaCliente.values()) {
            cliente.start();
        }

        // Iniciar Dashboard (opcional)
        threadDashboardCliente = new ThreadDashboardCliente(ipDashboard, portaDashboard, this);
        threadDashboardCliente.start();

        // Iniciar semáforos
        for (Semaforo semaforo : listaSemaforos) {
            semaforo.start();
        }
    }

    /**
     *
     *
     * @param veiculo
     * @param origem
     */
    public void receberVeiculo(Veiculo veiculo, String origem) {
        FilaVeiculos filaVeiculos = mapaOrigemParaFila.get(origem);

        if (filaVeiculos == null) {
            System.err.printf("[%s] ERRO: Origem '%s' desconhecida%n", nomeCruzamento, origem);
            return;
        }

        filaVeiculos.adicionar(veiculo);
        System.out.printf("[%s] Recebido veículo %s → fila %s%n", nomeCruzamento, veiculo.getId(), origem);
    }

    /**
     * Chamado pelo Semáforo quando um veículo sai da fila
     *
     * @param veiculo
     * @param filaOrigem
     */
    public void enviarVeiculoAposPassarSemaforo(Veiculo veiculo, FilaVeiculos filaOrigem) {

        // Avançar o caminho do veículo
        veiculo.avancarCaminho();

        String destino = mapaFilaParaDestino.get(filaOrigem);

        if (destino == null) {
            System.err.printf("[%s] ERRO: Sem destino para fila!%n", nomeCruzamento);
            return;
        }

        enviarVeiculoParaDestino(destino, veiculo);
    }

    /**
     *
     *
     * @param destino
     * @param veiculo
     */
    private void enviarVeiculoParaDestino(String destino, Veiculo veiculo) {

        ThreadCliente cliente = mapaDestinoParaCliente.get(destino);

        if (cliente == null) {
            System.err.printf("[%s] ERRO: Cliente TCP para '%s' não existe%n",
                    nomeCruzamento, destino);
            return;
        }

        System.out.printf("[%s] Enviar veículo %s → %s%n",
                nomeCruzamento, veiculo.getId(), destino);

        cliente.enviarVeiculo(veiculo, nomeCruzamento);
    }

    /**
     * Gera JSON simplificado para o Dashboard
     *
     * @return
     */
    public String gerarEstatisticasJSON() {

        List<Map<String, Object>> listaInfo = new ArrayList<>();

        for (Semaforo semaforo : listaSemaforos) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", semaforo.getIdSemaforo());
            info.put("estado", semaforo.isVerde() ? "VERDE" : "VERMELHO");
            info.put("tamanhoFila", semaforo.getTamanhoFila());
            listaInfo.add(info);
        }

        Map<String, Object> root = new HashMap<>();
        root.put("cruzamento", nomeCruzamento);
        root.put("semaforos", listaInfo);

        return gson.toJson(root);
    }


    /**
     *
     *
     */
    public void parar() {

        for (Semaforo semaforo : listaSemaforos) {
            semaforo.pararSemaforo();
        }

        for (ThreadCliente cliente : mapaDestinoParaCliente.values()) {
            cliente.parar();
        }

        if (threadServidor != null) {
            threadServidor.pararServidor();
        }

        if (threadDashboardCliente != null) {
            threadDashboardCliente.parar();
        }
    }
}
