package Cruzamentos;

import Dashboard.ComunicadorDashboard;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Processo que representa um cruzamento.
 *
 * ✅ Controla múltiplos semáforos
 * ✅ Monitoriza filas e reporta ao Dashboard
 * ✅ Implementa backpressure rejeitando veículos quando fila cheia
 */
public class Cruzamento {

    private final String identificador;
    private final int portaEntrada;
    private volatile boolean executando = true;

    // Estruturas de dados
    private final Map<String, FilaVeiculos> filasPorDirecao = new ConcurrentHashMap<>();
    private final Map<String, Semaforo> semaforosPorDirecao = new ConcurrentHashMap<>();

    private final GerirConexoes conexoes;
    private final ComunicadorDashboard dashboard = ComunicadorDashboard.getInstance();

    // Configurações de tempo (em milissegundos)
    private static final long TEMPO_VERDE_MS = 5000;      // Semáforo verde
    private static final long TEMPO_ALL_RED_MS = 1000;    // Todos vermelhos (segurança)
    private static final long TEMPO_PASSAGEM_MS = 500;    // Tempo de atravessar
    private static final long INTERVALO_MONITOR_MS = 1000; // Monitorização

    public Cruzamento(String identificador, int portaEntrada) {
        this.identificador = identificador;
        this.portaEntrada = portaEntrada;
        this.conexoes = new GerirConexoes(identificador);
    }

    /**
     * Adiciona uma direção de saída (fila + semáforo).
     */
    public void adicionarSaida(String direcao, String destino, int portaDestino) {
        String nomeFila = identificador + "_" + direcao;
        String nomeSemaforo = identificador + "_Sem_" + direcao;

        FilaVeiculos fila = new FilaVeiculos(nomeFila);
        Semaforo semaforo = new Semaforo(nomeSemaforo, fila, TEMPO_PASSAGEM_MS);

        filasPorDirecao.put(direcao, fila);
        semaforosPorDirecao.put(direcao, semaforo);
        conexoes.registarDestino(destino, portaDestino);

        System.out.printf("[%s] ✓ Saída configurada: %s → %s (porta %d)%n",
                identificador, direcao, destino, portaDestino);
    }

    /**
     * Inicia o cruzamento como processo independente.
     */
    public void iniciar() {
        System.out.printf("[%s] 🚦 Iniciando cruzamento...%n", identificador);

        try (ServerSocket serverSocket = new ServerSocket(portaEntrada)) {
            System.out.printf("[%s] ✓ Escutando na porta %d%n", identificador, portaEntrada);

            // 1. Estabelece conexões com destinos
            conexoes.estabelecerConexoes(semaforosPorDirecao);

            // 2. Inicia threads dos semáforos
            semaforosPorDirecao.values().forEach(Thread::start);
            System.out.printf("[%s] ✓ %d semáforo(s) iniciado(s)%n",
                    identificador, semaforosPorDirecao.size());

            // 3. Inicia controlador de semáforos (alterna verde/vermelho)
            new ControladorSemaforos(
                    identificador,
                    semaforosPorDirecao,
                    dashboard,
                    TEMPO_VERDE_MS,
                    TEMPO_ALL_RED_MS
            ).start();

            // 4. Inicia processador de veículos (recebe via socket)
            new ProcessadorVeiculos(serverSocket, filasPorDirecao, identificador).start();

            // 5. Inicia monitor de filas
            new Thread(this::monitorarFilas, identificador + "_Monitor").start();

            System.out.printf("[%s] ✅ Cruzamento operacional%n", identificador);

            // Mantém processo vivo
            while (executando) {
                Thread.sleep(500);
            }

        } catch (IOException e) {
            System.err.printf("[%s] ✗ Erro fatal: %s%n", identificador, e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            finalizar();
        }
    }

    /**
     * Monitora periodicamente as filas e reporta estado ao Dashboard.
     */
    private void monitorarFilas() {
        System.out.printf("[%s] 📊 Monitor de filas iniciado%n", identificador);

        while (executando) {
            try {
                for (Map.Entry<String, FilaVeiculos> entry : filasPorDirecao.entrySet()) {
                    String direcao = entry.getKey();
                    FilaVeiculos fila = entry.getValue();

                    int tamanho = fila.getTamanhoAtual();
                    boolean cheia = fila.estaCheia();

                    // Alerta se fila cheia
                    if (cheia) {
                        System.out.printf("[%s] ⚠️ FILA CHEIA: %s (%d veículos)%n",
                                identificador, direcao, tamanho);

                        dashboard.enviar(String.format(
                                "[FilaCheia] %s_%s=%d",
                                identificador, direcao, tamanho
                        ));
                    }

                    // Estatísticas gerais
                    dashboard.enviar(String.format(
                            "[Fila] %s_%s=%d/%d processados=%d",
                            identificador,
                            direcao,
                            tamanho,
                            10, // LIMITE_MAXIMO
                            fila.getTotalProcessados()
                    ));
                }

                Thread.sleep(INTERVALO_MONITOR_MS);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        System.out.printf("[%s] 🛑 Monitor de filas encerrado%n", identificador);
    }

    /**
     * Imprime estatísticas finais antes de encerrar.
     */
    private void imprimirEstatisticas() {
        System.out.printf("%n╔════════════════════════════════════════╗%n");
        System.out.printf("║  Estatísticas - %s                    ║%n", identificador);
        System.out.printf("╠════════════════════════════════════════╣%n");

        for (Map.Entry<String, FilaVeiculos> entry : filasPorDirecao.entrySet()) {
            FilaVeiculos fila = entry.getValue();
            System.out.printf("║ %s%n", fila.getEstatisticas());
        }

        System.out.printf("╚════════════════════════════════════════╝%n%n");

        // Estatísticas dos semáforos
        semaforosPorDirecao.forEach((direcao, semaforo) -> {
            System.out.printf("[%s] Semáforo: %s%n",
                    identificador, semaforo.getEstatisticas());
        });
    }

    /**
     * Encerra o cruzamento de forma segura.
     */
    public void finalizar() {
        if (!executando) return;

        System.out.printf("[%s] 🛑 Encerrando cruzamento...%n", identificador);
        executando = false;

        // Para semáforos
        semaforosPorDirecao.values().forEach(Semaforo::parar);

        // Aguarda threads terminarem
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ignored) {}

        // Fecha conexões
        conexoes.fecharTodas();

        // Imprime estatísticas
        imprimirEstatisticas();

        System.out.printf("[%s] ✅ Cruzamento finalizado%n", identificador);
    }

    /**
     * Exemplo de uso (Cr3 apenas).
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java Cruzamentos.Cruzamento <id> <porta>");
            System.exit(1);
        }

        String id = args[0];
        int porta = Integer.parseInt(args[1]);

        Cruzamento cruzamento = new Cruzamento(id, porta);

        // Configuração específica para Cr3
        if (id.equals("Cr3")) {
            cruzamento.adicionarSaida("de_E3_para_S", "S", 9100);
            cruzamento.adicionarSaida("de_Cr2_para_S", "S", 9100);
        }

        // Shutdown hook para finalização limpa
        Runtime.getRuntime().addShutdownHook(new Thread(cruzamento::finalizar));

        cruzamento.iniciar();
    }
}