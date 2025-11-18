package Dashboard;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import Dashboard.Logs.DashLogger;
import Dashboard.Logs.TipoLog;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Servidor que recebe mensagens TCP dos cruzamentos e da saída.
 * Atualiza a interface gráfica em tempo real.
 *
 */
public class ServidorDashboard extends Thread {

    private final int porta;
    private final DashboardFrame dashboardFrame;
    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    public ServidorDashboard(int porta, DashboardFrame dashboardFrame) {
        this.porta = porta;
        this.dashboardFrame = dashboardFrame;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            DashLogger.log(TipoLog.SISTEMA,"Dashboard a escutar na porta " + porta);

            while (ativo) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarCliente(socket)).start();
            }
        } catch (Exception e) {
            if (ativo) {
                DashLogger.log(TipoLog.ERRO,
                        "Erro no servidor do Dashboard: " + e.getMessage());
            }
        }
    }

    private void processarCliente(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = in.readLine()) != null) {
                processarMensagem(linha, socket);
            }
        } catch (Exception e) {
            DashLogger.log(TipoLog.ERRO, "Erro ao processar cliente: " + e.getMessage());
        }
    }

    private void processarMensagem(String json, Socket socket) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);
            String tipo = obj.get("tipo").getAsString();

            switch (tipo) {
                case "VEICULO_SAIU" -> processarVeiculoSaida(obj);
                case "VEICULO_GERADO" -> processarVeiculoGerado(obj);
                case "VEICULO_MOVIMENTO" -> processarVeiculoMovimento(obj);
                case "ESTATISTICA" -> processarEstatisticaCruzamento(obj);
                case "ESTATISTICA_SAIDA" -> processarEstatisticaSaida(obj);
                case "LOG" -> processarLog(obj, socket);
                default -> DashLogger.log(TipoLog.AVISO,"Mensagem desconhecida recebida: " + tipo);
            }
        } catch (Exception e) {
            DashLogger.log(TipoLog.ERRO, "Erro ao interpretar JSON: " + e.getMessage());
        }
    }

    private void processarLog(JsonObject jsonObjeto, Socket socketCliente) {
        try {
            if (!jsonObjeto.has("processo") || !jsonObjeto.has("nivel") || !jsonObjeto.has("mensagem")) {
                DashLogger.log(TipoLog.AVISO, "LOG recebido mas faltam campos obrigatórios.");
                return;
            }

            // Extrair campos do JSON
            String nomeProcesso = jsonObjeto.get("processo").getAsString();
            String nivelTexto = jsonObjeto.get("nivel").getAsString();
            String mensagemLog = jsonObjeto.get("mensagem").getAsString();

            TipoLog nivelLog;
            try {
                nivelLog = TipoLog.valueOf(nivelTexto);
            } catch (Exception e) {
                nivelLog = TipoLog.AVISO;
            }

            String enderecoIP = socketCliente.getInetAddress().getHostAddress();

            // Extrair porta real do servidor a partir da mensagem
            int portaServidor = -1;
            Pattern regexPorta = Pattern.compile("porta\\s+(\\d+)");
            Matcher matcherPorta = regexPorta.matcher(mensagemLog);

            if (matcherPorta.find()) {
                portaServidor = Integer.parseInt(matcherPorta.group(1));

                int portaFinal = portaServidor;
                SwingUtilities.invokeLater(() -> dashboardFrame.getPainelServidores().adicionarServidor(nomeProcesso, enderecoIP, portaFinal));

                DashLogger.log(TipoLog.SISTEMA,"Servidor registado: " + nomeProcesso + " -> " + enderecoIP + ":" + portaFinal);
            }

            // Registar o log normal
            DashLogger.log(nivelLog, "[" + nomeProcesso + "] " + mensagemLog);
        } catch (Exception e) {
            DashLogger.log(TipoLog.ERRO,
                    "Erro ao processar LOG recebido: " + e.getMessage());
        }
    }


    private void processarEstatisticaSaida(JsonObject obj) {
        // Neste momento não fazemos nada com estatísticas da Saída.
        // Método aqui apenas para evitar warnings ou erros.
    }

    /**
     * Processar veiculo na saida
     *
     * @param obj
     */
    private void processarVeiculoSaida(JsonObject obj) {

        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        String id = conteudo.get("id").getAsString();
        String tipo = conteudo.get("tipoVeiculo").getAsString();
        String entrada = conteudo.get("entrada").getAsString();
        JsonArray caminho = conteudo.getAsJsonArray("caminho");
        double tempoTotal = conteudo.get("tempoTotal").getAsDouble();

        SwingUtilities.invokeLater(() -> {
            dashboardFrame.getPainelVeiculos().adicionarVeiculoSaiu(id, tipo, entrada, caminho, tempoTotal);
            dashboardFrame.getPainelEstatisticas().incrementarSaidas();
        });

        DashLogger.log(TipoLog.VEICULO, "Veículo saiu: " + id + " (" + tipo + "), tempo total: " + tempoTotal + "s");
    }

    //  VEÍCULO GERADO
    private void processarVeiculoGerado(JsonObject obj) {
        String entrada = obj.get("origem").getAsString();

        SwingUtilities.invokeLater(() -> dashboardFrame.getPainelEstatisticas().incrementarGerado(entrada));

        DashLogger.log(TipoLog.GERADOR, "Veículo gerado em " + entrada);
    }

    //  MOVIMENTO (ANIMAÇÃO)
    private void processarVeiculoMovimento(JsonObject obj) {
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        String id = conteudo.get("id").getAsString();
        String tipo = conteudo.get("tipo").getAsString();
        String origem = conteudo.get("origem").getAsString();
        String destino = conteudo.get("destino").getAsString();

        SwingUtilities.invokeLater(() ->
                dashboardFrame.getPainelMapa()
                        .atualizarOuCriarVeiculo(id, tipo, origem, destino)
        );

        // ❌ SPAM — ANTES:
        // DashLogger.log(TipoLog.VEICULO, "Movimento: ...");

        // ✔ Apenas regista internamente (se precisares)
        // System.out.println("[DEBUG MOVIMENTO] " + id + " " + origem + " → " + destino);
    }

    //  ESTATÍSTICAS DE CRUZAMENTO (semafóros)
    private void processarEstatisticaCruzamento(JsonObject obj) {
        String cruzamento = obj.get("origem").getAsString();
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        if (conteudo != null && conteudo.has("estado")) {
            JsonObject estado = conteudo.getAsJsonObject("estado");

            if (estado.has("semaforos")) {
                JsonArray semaforos = estado.getAsJsonArray("semaforos");

                for (int i = 0; i < semaforos.size(); i++) {
                    JsonObject semaforo = semaforos.get(i).getAsJsonObject();

                    int id = semaforo.get("id").getAsInt();
                    String estadoSem = semaforo.get("estado").getAsString();
                    boolean verde = "VERDE".equals(estadoSem);

                    if (semaforo.has("origem") && semaforo.has("destino")) {
                        String origemSem = semaforo.get("origem").getAsString();
                        String destinoSem = semaforo.get("destino").getAsString();
                        String nomeSemaforo = origemSem + "→" + destinoSem;
                        int filaAtual = 0;

                        SwingUtilities.invokeLater(() -> dashboardFrame.getPainelMapa().registarSemaforoId(cruzamento, id, origemSem, destinoSem));
                        SwingUtilities.invokeLater(() -> dashboardFrame.getPainelEstatisticasCruzamentos().atualizarFila(cruzamento, nomeSemaforo, filaAtual));
                    }
                    SwingUtilities.invokeLater(() -> dashboardFrame.getPainelMapa().atualizarSemaforoPorId(cruzamento, id, verde) );
                }
            }

            // ❌ SPAM — antes imprimias “Estado atualizado” a torto e a direito.
            // DashLogger.log(TipoLog.CRUZAMENTO, "Estado atualizado: " + cruzamento);

            // ✔ Se quiseres manter *resumo*, usa isto:
            // DashLogger.log(TipoLog.CRUZAMENTO, "Cruzamento " + cruzamento + " recebeu estatísticas.");
        }
    }

    //  ENCERRAR
    public void parar() {
        ativo = false;
        interrupt();
        DashLogger.log(TipoLog.SISTEMA, "Servidor do Dashboard encerrado");
    }
}
