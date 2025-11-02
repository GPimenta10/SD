package Cruzamentos;

import Dashboard.ComunicadorDashboard;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Processo que representa um cruzamento.
 * Controla múltiplos semáforos de forma concorrente e envia estado ao Dashboard.
 *
 */
public class Cruzamento {

    private final String identificador;
    private final int portaEntrada;
    private volatile boolean executando = true;

    private final Map<String, FilaVeiculos> filasPorDirecao = new ConcurrentHashMap<>();
    private final Map<String, Semaforo> semaforosPorDirecao = new ConcurrentHashMap<>();

    private final GerirConexoes conexoes;
    private final ComunicadorDashboard dashboard = ComunicadorDashboard.getInstance();

    // Configurações de tempo
    private static final long TEMPO_VERDE_MS = 5000;
    private static final long TEMPO_ALL_RED_MS = 1000;
    private static final long TEMPO_PASSAGEM_MS = 500;

    /**
     * Construtor da classe
     *
     * @param identificador
     * @param portaEntrada
     */
    public Cruzamento(String identificador, int portaEntrada) {
        this.identificador = identificador;
        this.portaEntrada = portaEntrada;
        this.conexoes = new GerirConexoes(identificador);
    }

    /**
     * Adiciona uma direção de saída (fila + semáforo).
     * @param direcao
     * @param destino
     * @param portaDestino
     */
    public void adicionarSaida(String direcao, String destino, int portaDestino) {
        FilaVeiculos fila = new FilaVeiculos(identificador + "_" + direcao);
        Semaforo semaforo = new Semaforo(identificador + "_Sem_" + direcao, fila, TEMPO_PASSAGEM_MS);

        filasPorDirecao.put(direcao, fila);
        semaforosPorDirecao.put(direcao, semaforo);
        conexoes.registarDestino(destino, portaDestino);

        System.out.printf("[%s] Saída configurada: %s -> %s (porta %d)%n",
                identificador, direcao, destino, portaDestino);
    }

    /**
     * Inicia o cruzamento como processo independente.
     *
     */
    public void iniciar() {
        try (ServerSocket serverSocket = new ServerSocket(portaEntrada)) {
            System.out.printf("[%s] Cruzamento iniciado na porta %d%n", identificador, portaEntrada);

            conexoes.estabelecerConexoes(semaforosPorDirecao);

            semaforosPorDirecao.values().forEach(Thread::start);
            System.out.printf("[%s] %d semáforos iniciados%n", identificador, semaforosPorDirecao.size());

            new ControladorSemaforos(identificador, semaforosPorDirecao, dashboard,
                    TEMPO_VERDE_MS, TEMPO_ALL_RED_MS).start();

            new ProcessadorVeiculos(serverSocket, filasPorDirecao, identificador).start();

            while (executando) Thread.sleep(500);

        } catch (Exception e) {
            System.err.printf("[%s] Erro: %s%n", identificador, e.getMessage());
        } finally {
            finalizar();
        }
    }

    /**
     *
     */
    public void finalizar() {
        executando = false;
        semaforosPorDirecao.values().forEach(Semaforo::parar);
        conexoes.fecharTodas();
        System.out.printf("[%s] Cruzamento finalizado.%n", identificador);
    }

    /** Exemplo mínimo (Cr3). */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: java Cruzamentos.Cruzamento <id> <porta>");
            System.exit(1);
        }

        String id = args[0];
        int porta = Integer.parseInt(args[1]);
        Cruzamento cr = new Cruzamento(id, porta);

        if (id.equals("Cr3")) {
            cr.adicionarSaida("de_E3_para_S", "S", 9100);
            cr.adicionarSaida("de_Cr2_para_S", "S", 9100);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(cr::finalizar));
        cr.iniciar();
    }
}
