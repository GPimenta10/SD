package PontosEntrada;

import Logging.LogClienteDashboard;
import Dashboard.Logs.TipoLog;
import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;
import Rede.Mensagem;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerador de veículos com limite máximo.
 * Para automaticamente após gerar o número especificado de veículos.
 */
public class GeradorVeiculos extends Thread {

    private final PontoEntrada pontoEntrada;
    private final TipoCenario cenario;
    private final String ipPrimeiroCruzamento;
    private final int portaPrimeiroCruzamento;
    private final long intervaloGeracaoMs;
    private final int limiteVeiculos;

    private final com.google.gson.Gson gson = new com.google.gson.Gson();
    private volatile boolean ativo = true;

    private final AtomicInteger contadorGerados = new AtomicInteger(0);
    private static final AtomicInteger contadorIdsGlobal = new AtomicInteger(0);

    private final String ipDashboard;
    private final int portaDashboard;

    /**
     * Construtor do gerador de veículos.
     *
     * @param pontoEntrada Ponto de entrada associado
     * @param ipPrimeiroCruzamento IP do primeiro cruzamento
     * @param portaPrimeiroCruzamento Porta do primeiro cruzamento
     * @param intervaloGeracaoMs Intervalo entre gerações em ms
     * @param limiteVeiculos Número máximo de veículos a gerar
     * @param ipDashboard IP do Dashboard
     * @param portaDashboard Porta do Dashboard
     * @param cenario Tipo de cenário para geração de caminhos
     */
    public GeradorVeiculos(PontoEntrada pontoEntrada, String ipPrimeiroCruzamento,
                           int portaPrimeiroCruzamento, long intervaloGeracaoMs,
                           int limiteVeiculos, String ipDashboard, int portaDashboard,
                           TipoCenario cenario) {

        super("Gerador-" + pontoEntrada.name());
        this.pontoEntrada = pontoEntrada;
        this.cenario = cenario;
        this.ipDashboard = ipDashboard;
        this.portaDashboard = portaDashboard;
        this.ipPrimeiroCruzamento = ipPrimeiroCruzamento;
        this.portaPrimeiroCruzamento = portaPrimeiroCruzamento;
        this.intervaloGeracaoMs = intervaloGeracaoMs;
        this.limiteVeiculos = limiteVeiculos;
        setDaemon(false);
    }

    public int getTotalGerado() {
        return contadorGerados.get();
    }

    public void parar() {
        ativo = false;
        interrupt();
    }

    @Override
    public void run() {
        LogClienteDashboard.definirNomeProcesso(pontoEntrada.toString());
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Gerador " + pontoEntrada.name() +
                " iniciado (" + cenario.getDescricao() + "). Vai gerar " + limiteVeiculos + " veículos.");

        try {
            while (ativo && contadorGerados.get() < limiteVeiculos) {
                Veiculo veiculo = gerarVeiculo();

                String primeiroCruzamento = veiculo.getCaminho().isEmpty() ? "S" : veiculo.getCaminho().get(0);

                notificarMovimento(
                        veiculo.getId(),
                        veiculo.getTipo().name(),
                        pontoEntrada.name(),
                        primeiroCruzamento
                );

                enviarVeiculo(veiculo);
                notificarDashboard();

                int totalGerado = contadorGerados.incrementAndGet();

                if (totalGerado >= limiteVeiculos) {
                    break;
                }

                Thread.sleep(intervaloGeracaoMs);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        LogClienteDashboard.enviar(TipoLog.SUCESSO, "Gerador " + pontoEntrada.name() + " terminou (" +
                contadorGerados.get() + " veículos criados).");
    }

    /**
     * Gera um veículo com tipo aleatório e caminho baseado no cenário.
     *
     * @return Novo veículo criado
     */
    private Veiculo gerarVeiculo() {
        double p = ThreadLocalRandom.current().nextDouble();
        TipoVeiculo tipo;

        if (p < 0.4) {
            tipo = TipoVeiculo.CARRO;
        } else if (p < 0.8) {
            tipo = TipoVeiculo.MOTA;
        } else {
            tipo = TipoVeiculo.CAMIAO;
        }

        String id = String.format("%s-%03d", pontoEntrada.name(), contadorIdsGlobal.incrementAndGet());

        // Usa o cenário para gerar o caminho
        List<String> caminho = Caminhos.gerarCaminho(pontoEntrada, cenario);

        return new Veiculo(id, tipo, pontoEntrada, caminho);
    }

    /**
     * Envia um veículo para o primeiro cruzamento.
     */
    private void enviarVeiculo(Veiculo veiculo) {
        try (Socket socket = new Socket(ipPrimeiroCruzamento, portaPrimeiroCruzamento);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Mensagem mensagem = new Mensagem(
                    "VEICULO",
                    pontoEntrada.name(),
                    veiculo.getCaminho().isEmpty() ? "S" : veiculo.getCaminho().get(0),
                    Map.of("veiculo", veiculo)
            );

            String json = gson.toJson(mensagem);
            out.println(json);

        } catch (IOException e) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Falha ao enviar veículo " + veiculo.getId() +
                    " a partir de " + pontoEntrada.name() + ": " + e.getMessage());
        }
    }

    /**
     * Envia uma mensagem ao Dashboard via socket TCP.
     */
    private void enviarParaDashboard(Mensagem msg) {
        try (Socket socket = new Socket(ipDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
            out.println(gson.toJson(msg));
        } catch (IOException e) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Falha ao notificar Dashboard: " + e.getMessage());
        }
    }

    /**
     * Notifica o Dashboard que um veículo foi gerado.
     */
    private void notificarDashboard() {
        Mensagem msg = new Mensagem(
                "VEICULO_GERADO",
                pontoEntrada.name(),
                "Dashboard",
                Map.of("entrada", pontoEntrada.name())
        );
        enviarParaDashboard(msg);
    }

    /**
     * Notifica o Dashboard sobre o movimento de um veículo.
     */
    private void notificarMovimento(String idVeiculo, String tipo, String origem, String destino) {
        Map<String, Object> conteudo = new HashMap<>();
        conteudo.put("id", idVeiculo);
        conteudo.put("tipo", tipo);
        conteudo.put("origem", origem);
        conteudo.put("destino", destino);

        Mensagem msg = new Mensagem(
                "VEICULO_MOVIMENTO",
                origem,
                "Dashboard",
                conteudo
        );
        enviarParaDashboard(msg);
    }
}