package Dashboard;

import Rede.Servidor;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.SwingUtilities;

import Dashboard.Logs.DashLogger;
import Dashboard.Logs.TipoLog;
import Dashboard.Estatisticas.GestorEstatisticas;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class ServidorDashboard extends Servidor {

    private final DashboardFrame dashboardFrame;
    private final GestorEstatisticas gestor;
    private final Gson gson = new Gson();

    public ServidorDashboard(int porta, DashboardFrame dashboardFrame, GestorEstatisticas gestor) {
        super(porta, "ServidorDashboard");
        this.dashboardFrame = dashboardFrame;
        this.gestor = gestor;
    }

    @Override
    protected void tratarMensagem(String linha, BufferedReader leitor, PrintWriter escritor, Socket socket) {
        processarMensagem(linha, socket);
    }

    @Override
    protected void onInicio() {
        DashLogger.log(TipoLog.SISTEMA, "Dashboard a escutar na porta " + porta);
    }

    @Override
    protected void onEncerramento() {
        DashLogger.log(TipoLog.SISTEMA, "Servidor Dashboard encerrado");
    }

    private void processarMensagem(String json, Socket socket) {
        try {
            JsonObject obj = gson.fromJson(json, JsonObject.class);

            if (obj == null || !obj.has("tipo")) {
                DashLogger.log(TipoLog.AVISO, "JSON inválido recebido.");
                return;
            }

            String tipo = obj.get("tipo").getAsString();

            switch (tipo) {
                case "VEICULO_SAIU" -> processarVeiculoSaida(obj);
                case "VEICULO_GERADO" -> processarVeiculoGerado(obj);
                case "VEICULO_MOVIMENTO" -> processarVeiculoMovimento(obj);
                case "ESTATISTICA" -> processarEstatisticaCruzamento(obj);
                case "LOG" -> processarLog(obj, socket);
                default -> DashLogger.log(TipoLog.AVISO, "Mensagem desconhecida: " + tipo);
            }

        } catch (Exception e) {
            DashLogger.log(TipoLog.ERRO, "Erro ao interpretar JSON: " + e.getMessage());
        }
    }

    // =====================================================================
    //                                LOGS
    // =====================================================================

    private void processarLog(JsonObject jsonObjeto, Socket socketCliente) {
        try {

            if (!jsonObjeto.has("processo") || !jsonObjeto.has("nivel") || !jsonObjeto.has("mensagem")) {
                DashLogger.log(TipoLog.AVISO, "LOG recebido mas campos obrigatórios em falta.");
                return;
            }

            String processo = jsonObjeto.get("processo").getAsString();
            String nivelTxt = jsonObjeto.get("nivel").getAsString();
            String mensagem = jsonObjeto.get("mensagem").getAsString();

            TipoLog nivel;
            try { nivel = TipoLog.valueOf(nivelTxt); }
            catch (Exception e) { nivel = TipoLog.AVISO; }

            String ip = socketCliente.getInetAddress().getHostAddress();

            // Extrair porta
            Pattern regex = Pattern.compile("porta\\s+(\\d+)");
            Matcher matcher = regex.matcher(mensagem);

            if (matcher.find()) {
                int porta = Integer.parseInt(matcher.group(1));
                SwingUtilities.invokeLater(() ->
                        dashboardFrame.getPainelServidores().adicionarServidor(processo, ip, porta)
                );
            }

            DashLogger.log(nivel, "[" + processo + "] " + mensagem);

        } catch (Exception e) {
            DashLogger.log(TipoLog.ERRO, "Erro ao processar LOG: " + e.getMessage());
        }
    }

    // =====================================================================
    //                             VEÍCULO SAIU
    // =====================================================================

    private void processarVeiculoSaida(JsonObject obj) {

        JsonObject c = obj.getAsJsonObject("conteudo");

        String id = c.get("id").getAsString();
        String tipo = c.get("tipoVeiculo").getAsString();
        String entrada = c.get("entrada").getAsString();
        JsonArray caminhoJson = c.getAsJsonArray("caminho");
        long tempo = c.get("tempoTotal").getAsLong();

        // ----> Converter JsonArray -> List<String>
        List<String> caminhoList = new ArrayList<>();
        caminhoJson.forEach(e -> caminhoList.add(e.getAsString()));

        // ----> Atualizar o gestor
        JsonObject estat = new JsonObject();
        estat.addProperty("tipo", tipo);
        estat.addProperty("dwelling", tempo);
        estat.add("caminho", caminhoJson);

        gestor.registarSaidaVeiculoJSON(estat);

        // ----> Atualizar UI (agora com a List<String>, não JsonArray)
        SwingUtilities.invokeLater(() ->
                dashboardFrame.getPainelVeiculos().adicionarVeiculoSaiu(id, tipo, entrada, caminhoList, tempo)
        );

        DashLogger.log(TipoLog.VEICULO,
                "Veículo saiu: " + id + " (" + tipo + "), tempo=" + tempo + "s");
    }

    // =====================================================================
    //                           VEÍCULO GERADO
    // =====================================================================

    private void processarVeiculoGerado(JsonObject obj) {
        String entrada = obj.get("origem").getAsString();

        gestor.registarVeiculoGerado(entrada);

        DashLogger.log(TipoLog.GERADOR, "Veículo gerado em " + entrada);
    }

    // =====================================================================
    //                            MOVIMENTO
    // =====================================================================

    private void processarVeiculoMovimento(JsonObject obj) {

        JsonObject c = obj.getAsJsonObject("conteudo");

        String id = c.get("id").getAsString();
        String tipo = c.get("tipo").getAsString();
        String origem = c.get("origem").getAsString();
        String destino = c.get("destino").getAsString();

        SwingUtilities.invokeLater(() ->
                dashboardFrame.getPainelMapa().atualizarOuCriarVeiculo(id, tipo, origem, destino)
        );
    }

    // =====================================================================
    //                    ESTATÍSTICAS DE CRUZAMENTO
    // =====================================================================

    private void processarEstatisticaCruzamento(JsonObject obj) {

        String cruzamento = obj.get("origem").getAsString();
        JsonObject conteudo = obj.getAsJsonObject("conteudo");

        if (conteudo == null || !conteudo.has("estado")) return;

        JsonObject estado = conteudo.get("estado").getAsJsonObject();

        if (!estado.has("semaforos")) return;

        JsonArray semaforos = estado.get("semaforos").getAsJsonArray();

        for (int i = 0; i < semaforos.size(); i++) {

            JsonObject sm = semaforos.get(i).getAsJsonObject();

            int id = sm.get("id").getAsInt();
            String estadoSem = sm.get("estado").getAsString();
            boolean verde = estadoSem.equals("VERDE");

            String origem = sm.has("origem") ? sm.get("origem").getAsString() : null;
            String destino = sm.has("destino") ? sm.get("destino").getAsString() : null;

            int fila = sm.has("tamanhoFila") ? sm.get("tamanhoFila").getAsInt() : 0;

            if (origem != null && destino != null) {

                String nomeSemaforo = origem + "→" + destino;

                gestor.registarFilaAtualizada(cruzamento, nomeSemaforo, fila);

                SwingUtilities.invokeLater(() ->
                        dashboardFrame.getPainelMapa().registarSemaforoId(cruzamento, id, origem, destino)
                );
            }

            SwingUtilities.invokeLater(() ->
                    dashboardFrame.getPainelMapa().atualizarSemaforoPorId(cruzamento, id, verde)
            );
        }
    }
}
