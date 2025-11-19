package Cruzamentos;

import Utils.EnviarLogs;
import Veiculo.Veiculo;
import Dashboard.Logs.TipoLog;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

/**
 * Classe Cruzamento responsável por:
 *  - Criar e gerir filas de veículos (uma por origem)
 *  - Controlar os semáforos associados a cada fila
 *  - Receber veículos de outros cruzamentos
 *  - Enviar veículos para o cruzamento seguinte ou para a saída via TCP
 *  - Comunicar periodicamente o seu estado ao Dashboard
 *
 * Nota:
 *   Os semáforos apenas retiram veículos da fila associada.
 *   O envio entre cruzamentos é sempre feito pelo Cruzamento.
 */
public class Cruzamento {
    private final String nomeCruzamento;

    // Comunicação entre cruzamentos
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
     * Construtor do cruzamento.
     *
     * @param nomeCruzamento Nome do cruzamento
     * @param portaServidor Porta para receber veículos
     * @param ipDashboard IP do Dashboard
     * @param portaDashboard Porta do Dashboard
     */
    public Cruzamento(String nomeCruzamento, int portaServidor, String ipDashboard, int portaDashboard) {
        this.nomeCruzamento = nomeCruzamento;
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
     * Obtém o tamanho atual de uma fila específica.
     *
     * @param origemFila Identificador da fila
     * @return Tamanho da fila, ou -1 se não existir
     */
    public int obterTamanhoFila(String origemFila) {
        FilaVeiculos fila = mapaOrigemParaFila.get(origemFila);

        if (fila == null) {
            return -1;
        }

        return fila.getTamanhoAtual();
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
        EnviarLogs.enviar(TipoLog.SISTEMA, String.format("[%s] Configurar ligação: %s → %s", nomeCruzamento, origem, destino));

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
        EnviarLogs.enviar(TipoLog.CRUZAMENTO, "A iniciar o cruzamento " + nomeCruzamento);

        // Criar monitor dos semáforos
        monitorSemaforos = new MonitorSemaforos(mapaOrigemParaFila.size());

        // Criar semáforos
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

        // Arrancar servidor TCP
        servidorCruzamento = new ServidorCruzamento(portaServidor, this);
        servidorCruzamento.start();

        // Arrancar clientes TCP
        for (ClienteCruzamento cliente : mapaDestinoParaCliente.values()) {
            cliente.start();
        }

        // Cliente do Dashboard
        clienteCruzamentoDashboard = new ClienteCruzamentoDashboard(ipDashboard, portaDashboard, this);
        clienteCruzamentoDashboard.start();

        // Arrancar semáforos
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
            EnviarLogs.enviar(TipoLog.ERRO, String.format("[%s] ERRO: Origem '%s' desconhecida", nomeCruzamento, origem));
            return;
        }

        filaVeiculos.adicionar(veiculo);
        EnviarLogs.enviar(TipoLog.SISTEMA, String.format("[%s] Recebido veículo %s → fila %s", nomeCruzamento, veiculo.getId(), origem));
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
            EnviarLogs.enviar(TipoLog.ERRO, String.format("[%s] ERRO: Cliente TCP para '%s' não existe",
                    nomeCruzamento, destino));
            return;
        }

        EnviarLogs.enviar(TipoLog.SISTEMA, String.format("[%s] Enviar veículo %s → %s", nomeCruzamento, veiculo.getId(), destino));
        cliente.enviarVeiculo(veiculo, nomeCruzamento);
    }

    /**
     * Notifica o Dashboard acerca do movimento de um veículo.
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
            EnviarLogs.enviar(TipoLog.ERRO, String.format("[%s] Falha ao notificar Dashboard: %s",
                    nomeCruzamento, e.getMessage()));
        }
    }

    /**
     * Constrói o mapa de estatísticas enviado ao Dashboard.
     *
     * @return Mapa com o estado deste cruzamento
     */
    public Map<String, Object> gerarEstatisticas() {
        List<Map<String, Object>> listaInfo = new ArrayList<>();

        for (Semaforo semaforo : listaSemaforos) {
            Map<String, Object> info = new HashMap<>();

            info.put("id", semaforo.getIdSemaforo());
            info.put("estado", semaforo.isVerde() ? "VERDE" : "VERMELHO");
            info.put("tamanhoFila", semaforo.getTamanhoFila());
            info.put("origem", semaforo.getOrigem());
            info.put("destino", nomeCruzamento);

            listaInfo.add(info);
        }

        Map<String, Object> root = new HashMap<>();
        root.put("cruzamento", nomeCruzamento);
        root.put("semaforos", listaInfo);

        return root;
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