package Cruzamentos;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import Veiculo.Veiculo;

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
                    origem,
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

        // NOTIFICAR DASHBOARD DO MOVIMENTO
        notificarDashboardMovimento(veiculo, nomeCruzamento, destino);

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

//ADICIONADO -------------------------------------------------------------------------------
    /**
     * NOVO: Notifica o Dashboard sobre o movimento de um veículo.
     *
     * @param veiculo O veículo que se moveu.
     * @param origem  O ponto de partida do movimento (este cruzamento).
     * @param destino O próximo ponto no caminho do veículo.
     */
    private void notificarDashboardMovimento(Veiculo veiculo, String origem, String destino) {
        // Usa o IP e a Porta do Dashboard configurados para este cruzamento
        try (Socket socket = new Socket(ipDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> conteudo = new HashMap<>();
            conteudo.put("id", veiculo.getId());
            conteudo.put("tipo", veiculo.getTipo().name());
            conteudo.put("origem", origem);
            conteudo.put("destino", destino);

            // O tipo da mensagem é o mesmo que o gerador usa
            Map<String, Object> mensagem = new HashMap<>();
            mensagem.put("tipo", "VEICULO_MOVIMENTO");
            mensagem.put("remetente", origem);
            mensagem.put("destinatario", "Dashboard");
            mensagem.put("conteudo", conteudo);

            out.println(gson.toJson(mensagem));

        } catch (IOException e) {
            System.err.printf("[%s] Falha ao notificar Dashboard sobre movimento: %s%n", nomeCruzamento, e.getMessage());
        }
    }

    /**
     * Gera um mapa com as estatísticas do cruzamento para o Dashboard.
     *
     * @return Um mapa contendo o estado do cruzamento e dos seus semáforos.
     */
    public Map<String, Object> gerarEstatisticas() {
        List<Map<String, Object>> listaInfo = new ArrayList<>();

        for (Semaforo semaforo : listaSemaforos) {
            Map<String, Object> info = new HashMap<>();
            info.put("id", semaforo.getIdSemaforo());
            info.put("estado", semaforo.isVerde() ? "VERDE" : "VERMELHO");
            info.put("tamanhoFila", semaforo.getTamanhoFila());
            info.put("origem", semaforo.getOrigem());
            // ✓ CORREÇÃO: destino deve ser o próprio cruzamento, não o destino final
            info.put("destino", nomeCruzamento); // ← MUDA ISTO
            listaInfo.add(info);
        }

        Map<String, Object> root = new HashMap<>();
        root.put("cruzamento", nomeCruzamento);
        root.put("semaforos", listaInfo);

        return root;
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
