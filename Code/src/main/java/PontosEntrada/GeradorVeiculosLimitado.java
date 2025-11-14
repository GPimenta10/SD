package PontosEntrada;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.Gson;

import Rede.Mensagem;
import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;

/**
 * Gerador de veículos com limite máximo.
 * Para automaticamente após gerar o número especificado de veículos.
 */
public class GeradorVeiculosLimitado extends Thread {

    private final PontoEntrada pontoEntrada;
    private final String ipPrimeiroCruzamento;
    private final int portaPrimeiroCruzamento;
    private final long intervaloGeracaoMs;
    private final int limiteVeiculos; // NOVO: limite de veículos

    private final Gson gson = new Gson();
    private volatile boolean ativo = true;

    private final AtomicInteger contadorGerados = new AtomicInteger(0);
    private static AtomicInteger contadorIdsGlobal = new AtomicInteger(0);

    public GeradorVeiculosLimitado(PontoEntrada pontoEntrada, String ipPrimeiroCruzamento, int portaPrimeiroCruzamento, long intervaloGeracaoMs, int limiteVeiculos) {
        super("Gerador-" + pontoEntrada.name());
        this.pontoEntrada = pontoEntrada;
        this.ipPrimeiroCruzamento = ipPrimeiroCruzamento;
        this.portaPrimeiroCruzamento = portaPrimeiroCruzamento;
        this.intervaloGeracaoMs = intervaloGeracaoMs;
        this.limiteVeiculos = limiteVeiculos;
        setDaemon(false); // NÃO daemon para manter processo vivo
    }

    @Override
    public void run() {
        System.out.printf("[%s] Iniciado - gerará %d veículos a cada %d ms%n",
                pontoEntrada.name(), limiteVeiculos, intervaloGeracaoMs);

        try {
            while (ativo && contadorGerados.get() < limiteVeiculos) {
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

                int totalGerado = contadorGerados.incrementAndGet();

                System.out.printf("[%s] ✓ Veículo %d/%d gerado: %s (%s) - Caminho: %s%n",
                        pontoEntrada.name(),
                        totalGerado,
                        limiteVeiculos,
                        veiculo.getId(),
                        veiculo.getTipo(),
                        veiculo.getCaminho());

                // Se atingiu o limite, para
                if (totalGerado >= limiteVeiculos) {
                    System.out.printf("[%s] ✓ LIMITE ATINGIDO: %d veículos gerados. Encerrando...%n",
                            pontoEntrada.name(), totalGerado);
                    break;
                }

                Thread.sleep(intervaloGeracaoMs);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.printf("[%s] Interrompido.%n", pontoEntrada.name());
        }

        System.out.printf("[%s] ✓ Encerrado. Total gerado: %d veículos%n",
                pontoEntrada.name(), contadorGerados.get());
    }

    private Veiculo gerarVeiculo() {
        double p = ThreadLocalRandom.current().nextDouble();
        TipoVeiculo tipo;

        if (p < 0.4) tipo = TipoVeiculo.CARRO;
        else if (p < 0.8) tipo = TipoVeiculo.MOTA;
        else tipo = TipoVeiculo.CAMIAO;

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
            System.err.printf("[%s] ❌ Erro ao enviar veículo %s: %s%n",
                    pontoEntrada.name(), veiculo.getId(), e.getMessage());
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

        } catch (IOException e) {
            // Falha silenciosa
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

        } catch (IOException e) {
            // Falha silenciosa
        }
    }

    public void parar() {
        ativo = false;
        interrupt();
    }

    public int getTotalGerado() {
        return contadorGerados.get();
    }
}