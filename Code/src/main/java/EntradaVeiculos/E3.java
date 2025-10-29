package EntradaVeiculos;

import OutrasClasses.ComunicadorSocket;
import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;
import java.io.*;
import java.net.Socket;
import java.util.List;
import java.util.Random;

/**
 * Entrada E3 - Gera veículos e envia para Cruzamento3.
 *
 * Execução independente com main().
 * Comunicação simples: apenas ENVIA veículos (sem ServerSocket).
 */
public class E3 {
    private final String hostCruzamento;
    private final int portaCruzamento;
    private final int totalVeiculos;
    private final int intervaloMs;
    private final Random random = new Random();

    private static final String NOME = "E3";
    private static final List<String> PERCURSO = List.of("E3", "Cr3", "S");

    public E3(String host, int port, int totalVeiculos, int intervalo) {
        this.hostCruzamento = host;
        this.portaCruzamento = port;
        this.totalVeiculos = totalVeiculos;
        this.intervaloMs = intervalo;
    }

    /**
     * MAIN - Executa E3 como processo independente.
     * Uso: java EntradaVeiculos.E3 <host> <porta> <numVeiculos> <intervalo>
     */
    public static void main(String[] args) {
        String host = "127.0.0.1";
        int porta = 6003;
        int numVeiculos = 20;
        int intervalo = 500;

        if (args.length >= 4) {
            host = args[0];
            porta = Integer.parseInt(args[1]);
            numVeiculos = Integer.parseInt(args[2]);
            intervalo = Integer.parseInt(args[3]);
        }

        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║        ENTRADA E3 - INICIANDO      ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Destino: " + host + ":" + porta);
        System.out.println("Veículos: " + numVeiculos);
        System.out.println("Intervalo: " + intervalo + "ms");
        System.out.println("─".repeat(50));

        E3 entrada = new E3(host, porta, numVeiculos, intervalo);
        entrada.iniciar();
    }

    /**
     * Inicia geração e envio de veículos.
     */
    public void iniciar() {
        try (Socket socket = tentarConectar();
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            log("✓ Conectado ao Cruzamento3");

            for (int i = 1; i <= totalVeiculos; i++) {
                Veiculo v = gerarVeiculo(i);

                v.setTimestampEntradaFila(System.currentTimeMillis());
                out.writeObject(v);
                out.flush();

                log("→ Enviado: " + v.getId() + " (" + v.getTipo() + ")");

                // Envia mensagem legível para o Dashboard (formato compatível)
                String mensagem = "[E3] Enviado: " + v.getId() + " (" + v.getTipo() + ")";
                ComunicadorSocket.enviarParaDashboard(mensagem, "127.0.0.1", 9000);

                // Intervalo entre veículos
                Thread.sleep(intervaloMs);
            }

            log("✓ Geração concluída (" + totalVeiculos + " veículos enviados)");
            // Enviar sinal de fim de geração
            ComunicadorSocket.enviarParaDashboard("[E3] FIM_GERACAO", "127.0.0.1", 9000);

        } catch (IOException e) {
            System.err.println("[" + NOME + "] ❌ Erro de comunicação: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("[" + NOME + "] ⚠️ Interrompido");
        }
    }

    /**
     * Gera um veículo com ID único e tipo variado.
     */
    private Veiculo gerarVeiculo(int sequencia) {
        String id = String.format("%s-%03d", NOME, sequencia);
        TipoVeiculo tipo = escolherTipo();
        return new Veiculo(id, tipo, NOME, PERCURSO);
    }

    /**
     * Escolhe tipo de veículo aleatoriamente.
     * Distribuição: 50% Carros, 30% Motos, 20% Camiões
     */
    private TipoVeiculo escolherTipo() {
        int rand = random.nextInt(100);
        if (rand < 30) return TipoVeiculo.MOTA;
        if (rand < 80) return TipoVeiculo.CARRO;
        return TipoVeiculo.CAMIAO;
    }

    private void log(String mensagem) {
        System.out.println("[" + NOME + "] " + mensagem);
    }

    private Socket tentarConectar() throws IOException {
        int tentativas = 3;
        for (int i = 1; i <= tentativas; i++) {
            try {
                return new Socket(hostCruzamento, portaCruzamento);
            } catch (IOException e) {
                log("Tentativa " + i + " falhou, a tentar novamente...");
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        throw new IOException("Não foi possível conectar ao Cruzamento3 após " + tentativas + " tentativas.");
    }
}