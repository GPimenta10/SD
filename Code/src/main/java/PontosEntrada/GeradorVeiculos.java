package PontosEntrada;

import Dashboard.TipoLog;
import Rede.Mensagem;
import Utils.EnviarLogs;
import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Gerador de veículos com limite máximo.
 * Para automaticamente após gerar o número especificado de veículos.
 */
public class GeradorVeiculos extends Thread {

    private final PontoEntrada pontoEntrada;
    private final String ipPrimeiroCruzamento;
    private final int portaPrimeiroCruzamento;
    private final long intervaloGeracaoMs;
    private final int limiteVeiculos;

    private final com.google.gson.Gson gson = new com.google.gson.Gson();
    private volatile boolean ativo = true;

    private final AtomicInteger contadorGerados = new AtomicInteger(0);
    private static final AtomicInteger contadorIdsGlobal = new AtomicInteger(0);

    public GeradorVeiculos(PontoEntrada pontoEntrada, String ipPrimeiroCruzamento, int portaPrimeiroCruzamento, long intervaloGeracaoMs,
                           int limiteVeiculos) {
        super("Gerador-" + pontoEntrada.name());
        this.pontoEntrada = pontoEntrada;
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
        EnviarLogs.definirNomeProcesso(pontoEntrada.toString());
        EnviarLogs.enviar(TipoLog.SISTEMA, "Gerador " + pontoEntrada.name() + " iniciado. Vai gerar " + limiteVeiculos + " veículos.");

        try {
            while (ativo && contadorGerados.get() < limiteVeiculos) {
                // Criar veículo
                Veiculo veiculo = gerarVeiculo();

                // Determinar o primeiro cruzamento
                String primeiroCruzamento = veiculo.getCaminho().isEmpty() ? "S" : veiculo.getCaminho().get(0);

                // Notificar Dashboard do movimento inicial
                notificarMovimento(
                        veiculo.getId(),
                        veiculo.getTipo().name(),
                        pontoEntrada.name(),
                        primeiroCruzamento
                );

                // Enviar veículo ao cruzamento
                enviarVeiculo(veiculo);

                // Enviar sinal de geração
                notificarDashboard();

                // Incrementar contador
                int totalGerado = contadorGerados.incrementAndGet();

                // Debug comentado
                /*
                System.out.printf("[%s] Veículo %d/%d gerado: %s (%s)%n",
                        pontoEntrada.name(), totalGerado, limiteVeiculos,
                        veiculo.getId(), veiculo.getTipo());
                */

                if (totalGerado >= limiteVeiculos) {
                    break;
                }

                Thread.sleep(intervaloGeracaoMs);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // Debug comentado:
            // System.out.printf("[%s] Interrompido.%n", pontoEntrada.name());
        }

        EnviarLogs.enviar(TipoLog.SUCESSO, "Gerador " + pontoEntrada.name() + " terminou (" +
                contadorGerados.get() + " veículos criados).");
    }

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

        List<String> caminho = Caminhos.gerarCaminho(pontoEntrada);

        return new Veiculo(id, tipo, pontoEntrada, caminho);
    }

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
            EnviarLogs.enviar(TipoLog.AVISO, "Falha ao enviar veículo " + veiculo.getId() +
                    " a partir de " + pontoEntrada.name() + ": " + e.getMessage());
        }
    }

    private void notificarDashboard() {
        try (Socket socket = new Socket("localhost", 6000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            Mensagem msg = new Mensagem(
                    "VEICULO_GERADO",
                    pontoEntrada.name(),
                    "Dashboard",
                    Map.of("entrada", pontoEntrada.name())
            );

            out.println(gson.toJson(msg));

        } catch (IOException ignored) {
        }
    }

    private void notificarMovimento(String idVeiculo, String tipo, String origem, String destino) {
        try (Socket socket = new Socket("localhost", 6000);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

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

            out.println(gson.toJson(msg));

        } catch (IOException ignored) {
        }
    }
}
