package Cruzamentos;

import Dashboard.Estatisticas.EstatisticaCruzamento;
import Dashboard.Estatisticas.EstatisticaSemaforo;
import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;
import Veiculo.Veiculo;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Classe Cruzamento responsável por:
 *   Criar e gerir filas de veículos (uma por origem)
 *   Controlar os semáforos associados a cada fila
 *   Receber veículos de outros cruzamentos
 *   Enviar veículos para o cruzamento seguinte ou para a saída via TCP
 *   Comunicar periodicamente o seu estado ao Dashboard
 *
 * Nota:
 *   Os semáforos apenas retiram veículos da fila associada.
 *   O envio entre cruzamentos é sempre feito pelo Cruzamento.
 */
public class Cruzamento {
    private final String nomeCruzamento;

    // Comunicação entre cruzamentos
    private final String ipServidor;
    private final int portaServidor;
    private final Map<String, ClienteCruzamento> mapaDestinoParaCliente = new HashMap<>();
    private ServidorCruzamento servidorCruzamento;

    // Dashboard
    private final String ipDashboard;
    private final int portaDashboard;
    private ClienteCruzamentoDashboard clienteCruzamentoDashboard;

    // Filas
    private final Map<String, FilaVeiculos> mapaOrigemParaFila = new HashMap<>();
    private final Map<FilaVeiculos, String> mapaFilaParaDestino = new HashMap<>();

    // Semáforos
    private final List<Semaforo> listaSemaforos = new ArrayList<>();
    private MonitorSemaforos monitorSemaforos;

    private final Gson gson = new Gson();

    /**
     * Construtor da classe.
     *
     * @param nomeCruzamento Nome do cruzamento
     * @param ipServidor IP para este cruzamento escutar
     * @param portaServidor Porta para receber veículos
     * @param ipDashboard IP do Dashboard
     * @param portaDashboard Porta do Dashboard
     */
    public Cruzamento(String nomeCruzamento, String ipServidor, int portaServidor, String ipDashboard, int portaDashboard) {
        this.nomeCruzamento = nomeCruzamento;
        this.ipServidor = ipServidor;
        this.portaServidor = portaServidor;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;
    }

    /**
     * Obtém o nome do cruzamento.
     *
     * @return Nome do cruzamento
     */
    public String getNomeCruzamento() {
        return nomeCruzamento;
    }

    /**
     * Define uma ligação entre uma origem deste cruzamento e um cruzamento seguinte.
     *
     * @param origem Nome da origem
     * @param destino Nome do próximo cruzamento
     * @param ipDestino IP do cruzamento destino
     * @param portaDestino Porta TCP do cruzamento destino
     */
    public void adicionarLigacao(String origem, String destino, String ipDestino, int portaDestino) {
        LogClienteDashboard.enviar(TipoLog.SISTEMA, String.format("[%s] Configurar ligação: %s → %s", nomeCruzamento, origem, destino));

        // Criar a fila associada à origem
        FilaVeiculos fila = new FilaVeiculos();
        mapaOrigemParaFila.put(origem, fila);
        mapaFilaParaDestino.put(fila, destino);

        // Criar cliente TCP apenas uma vez por destino
        if (!mapaDestinoParaCliente.containsKey(destino)) {
            ClienteCruzamento cliente = new ClienteCruzamento(destino, ipDestino, portaDestino);
            mapaDestinoParaCliente.put(destino, cliente);
        }
    }

    /**
     * Inicializa o cruzamento, os semáforos e os clientes/servidores TCP.
     */
    public void iniciar() {
        LogClienteDashboard.enviar(TipoLog.CRUZAMENTO, "A iniciar o cruzamento " + nomeCruzamento);

        monitorSemaforos = new MonitorSemaforos(mapaOrigemParaFila.size());

        int idSemaforo = 0;
        for (Map.Entry<String, FilaVeiculos> entry : mapaOrigemParaFila.entrySet()) {

            String origem = entry.getKey();
            FilaVeiculos filaVeiculos = entry.getValue();

            Semaforo semaforo = new Semaforo(
                    idSemaforo,
                    origem,
                    monitorSemaforos,
                    5000,
                    filaVeiculos,
                    this
            );

            listaSemaforos.add(semaforo);
            idSemaforo++;
        }

        // Agora passamos o IP e a Porta
        servidorCruzamento = new ServidorCruzamento(ipServidor, portaServidor, this);
        servidorCruzamento.start();

        for (ClienteCruzamento cliente : mapaDestinoParaCliente.values()) {
            cliente.start();
        }

        clienteCruzamentoDashboard = new ClienteCruzamentoDashboard(ipDashboard, portaDashboard, this);
        clienteCruzamentoDashboard.start();

        for (Semaforo semaforo : listaSemaforos) {
            semaforo.start();
        }
    }

    /**
     * Recebe um veículo proveniente de outro cruzamento.
     *
     * @param veiculo Veículo recebido
     * @param origem Origem do envio
     */
    public void receberVeiculo(Veiculo veiculo, String origem) {
        FilaVeiculos filaVeiculos = mapaOrigemParaFila.get(origem);

        if (filaVeiculos == null) {
            LogClienteDashboard.enviar(TipoLog.ERRO, String.format("[%s] ERRO: Origem '%s' desconhecida", nomeCruzamento, origem));
            return;
        }

        filaVeiculos.adicionar(veiculo);
        LogClienteDashboard.enviar(TipoLog.SISTEMA, String.format("[%s] Recebido veículo %s → fila %s", nomeCruzamento, veiculo.getId(), origem));
    }

    /**
     * Chamado pelo Semáforo quando um veículo atravessa o cruzamento.
     *
     * @param veiculo Veículo que saiu da fila
     * @param filaOrigem Fila correspondente
     */
    public void enviarVeiculoAposPassarSemaforo(Veiculo veiculo, FilaVeiculos filaOrigem) {
        // Avançar um passo no caminho
        veiculo.avancarCaminho();

        // Obter próximo destino (ou "S" se for saída)
        String destino = veiculo.getProximoNo();

        if (destino == null || destino.equals(nomeCruzamento)) {
            destino = "S";
        }

        notificarDashboardMovimento(veiculo, nomeCruzamento, destino);
        enviarVeiculoParaDestino(destino, veiculo);
    }

    /**
     * Envia um veículo para um cruzamento destino.
     */
    private void enviarVeiculoParaDestino(String destino, Veiculo veiculo) {
        ClienteCruzamento cliente = mapaDestinoParaCliente.get(destino);

        if (cliente == null) {
            LogClienteDashboard.enviar(TipoLog.ERRO, String.format("[%s] ERRO: Cliente TCP para '%s' não existe",
                    nomeCruzamento, destino));
            return;
        }

        LogClienteDashboard.enviar(TipoLog.SISTEMA, String.format("[%s] Enviar veículo %s → %s", nomeCruzamento, veiculo.getId(), destino));
        cliente.enviarVeiculo(veiculo, nomeCruzamento);
    }

    /**
     * Notifica o Dashboard sobre o movimento de um veículo.
     */
    private void notificarDashboardMovimento(Veiculo veiculo, String origem, String destino) {
        try (Socket socket = new Socket(ipDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Map<String, Object> conteudo = new HashMap<>();
            conteudo.put("id", veiculo.getId());
            conteudo.put("tipo", veiculo.getTipo().name());
            conteudo.put("origem", origem);
            conteudo.put("destino", destino);

            Map<String, Object> mensagem = new HashMap<>();
            mensagem.put("tipo", "VEICULO_MOVIMENTO");
            mensagem.put("remetente", origem);
            mensagem.put("destinatario", "Dashboard");
            mensagem.put("conteudo", conteudo);

            out.println(gson.toJson(mensagem));
        } catch (IOException e) {
            LogClienteDashboard.enviar(TipoLog.ERRO, String.format("[%s] Falha ao notificar Dashboard: %s", nomeCruzamento, e.getMessage()));
        }
    }

    /**
     * Constrói o mapa de estatísticas enviado ao Dashboard.
     *
     * @return
     */
    public Map<String, Object> gerarEstatisticas() {
        List<EstatisticaSemaforo> lista = new ArrayList<>();

        for (Semaforo semaforo : listaSemaforos) {
            lista.add(semaforo.getEstatistica(nomeCruzamento));
        }

        // Criar objeto de estatísticas do cruzamento
        EstatisticaCruzamento estatistica = new EstatisticaCruzamento(nomeCruzamento, lista);

        // Mantém compatibilidade: devolve Map<String,Object>
        return estatistica.toMap();
    }

    /**
     * Encerra todos os componentes do cruzamento.
     */
    public void parar() {
        for (Semaforo semaforo : listaSemaforos) {
            semaforo.pararSemaforo();
        }

        for (ClienteCruzamento cliente : mapaDestinoParaCliente.values()) {
            cliente.parar();
        }

        if (servidorCruzamento != null) {
            servidorCruzamento.pararServidor();
        }

        if (clienteCruzamentoDashboard != null) {
            clienteCruzamentoDashboard.parar();
        }
    }
}