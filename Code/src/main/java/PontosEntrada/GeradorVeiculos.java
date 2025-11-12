package PontosEntrada;

import Rede.Mensagem;
import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Classe responsável por gerar veículos num ponto de entrada
 * e enviá-los diretamente ao primeiro cruzamento do seu caminho.
 *
 * ATUALIZADO: Notifica Dashboard do movimento do veículo
 */
public class GeradorVeiculos extends Thread {

    private final PontoEntrada pontoEntrada;
    private final String ipPrimeiroCruzamento;
    private final int portaPrimeiroCruzamento;
    private final long intervaloGeracaoMs;

    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    private static long contadorIds = 0;

    public GeradorVeiculos(PontoEntrada pontoEntrada, String ipPrimeiroCruzamento,
                           int portaPrimeiroCruzamento, long intervaloGeracaoMs) {
        super("Gerador-" + pontoEntrada.name());
        this.pontoEntrada = pontoEntrada;
        this.ipPrimeiroCruzamento = ipPrimeiroCruzamento;
        this.portaPrimeiroCruzamento = portaPrimeiroCruzamento;
        this.intervaloGeracaoMs = intervaloGeracaoMs;
        setDaemon(true);
    }

    @Override
    public void run() {
        System.out.printf("[%s] Iniciado - geração a cada %d ms%n",
                pontoEntrada.name(), intervaloGeracaoMs);

        try {
            while (ativo) {
                Veiculo veiculo = gerarVeiculo();

                // Determina o primeiro cruzamento do caminho
                String primeiroCruzamento = veiculo.getCaminho().isEmpty() ? "S" : veiculo.getCaminho().get(0);

                // Notifica Dashboard do movimento
                notificarMovimento(veiculo.getId(), veiculo.getTipo().name(),
                        pontoEntrada.name(), primeiroCruzamento);

                // Envia veículo ao cruzamento
                enviarVeiculo(veiculo);

                // Notifica geração
                notificarDashboard();

                System.out.printf("[%s] Gerado e enviado veículo %s (%s) - Caminho: %s%n",
                        pontoEntrada.name(),
                        veiculo.getId(),
                        veiculo.getTipo(),
                        veiculo.getCaminho());

                Thread.sleep(intervaloGeracaoMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.printf("[%s] Interrompido.%n", pontoEntrada.name());
        }
    }

    private Veiculo gerarVeiculo() {
        double p = ThreadLocalRandom.current().nextDouble();
        TipoVeiculo tipo;

        if (p < 0.4) tipo = TipoVeiculo.CARRO;
        else if (p < 0.8) tipo = TipoVeiculo.MOTA;
        else tipo = TipoVeiculo.CAMIAO;

        String id = String.format("%s-%03d", pontoEntrada.name(), ++contadorIds);
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
            System.err.printf("[%s] Erro ao enviar veículo %s: %s%n",
                    pontoEntrada.name(), veiculo.getId(), e.getMessage());
        }
    }

    /**
     * Notifica o Dashboard que um veículo foi gerado
     */
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

        } catch (IOException e) {
            System.err.printf("[%s] Falha ao notificar Dashboard: %s%n",
                    pontoEntrada.name(), e.getMessage());
        }
    }

    /**
     * NOVO: Notifica o Dashboard do movimento do veículo no mapa
     */
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

        } catch (IOException e) {
            System.err.printf("[%s] Falha ao notificar movimento: %s%n",
                    pontoEntrada.name(), e.getMessage());
        }
    }

    public void parar() {
        ativo = false;
        interrupt();
    }
}