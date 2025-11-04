package Cruzamentos;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Responsável por conectar o cruzamento aos próximos nós.
 *
 */
public class GerirConexoes {

    private final String id;
    private final Map<String, Integer> destinos = new ConcurrentHashMap<>();
    private final Map<String, Socket> sockets = new ConcurrentHashMap<>();
    private final Map<String, ObjectOutputStream> streams = new ConcurrentHashMap<>();

    public GerirConexoes(String id) {
        this.id = id;
    }

    public void registarDestino(String nome, int porta) {
        destinos.put(nome, porta);
    }

    /**
     * Estabelece conexões TCP com os destinos e cria ObjectOutputStream.
     * Atribui streams aos semáforos correspondentes.
     */
    public void estabelecerConexoes(Map<String, Semaforo> semaforos) {
        destinos.forEach((destino, porta) -> {
            try {
                Socket socket = conectarComRetry("localhost", porta, 5);
                sockets.put(destino, socket);

                ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                streams.put(destino, oos);

                System.out.printf("[%s] ✓ Conectado a %s (porta %d)%n", id, destino, porta);

                semaforos.forEach((direcao, s) -> {
                    if (direcao.endsWith("para_" + destino)) {
                        s.setSocketSaida(oos);
                        System.out.printf("[%s] ✓ Semáforo '%s' configurado para enviar a %s%n",
                                id, direcao, destino);
                    }
                });
            } catch (IOException e) {
                System.err.printf("[%s] ✗ Falha ao conectar a %s: %s%n", id, destino, e.getMessage());
            }
        });
    }

    /**
     * Tenta conectar com retry exponencial.
     */
    private Socket conectarComRetry(String host, int porta, int tentativas) throws IOException {
        IOException ultima = null;
        for (int i = 1; i <= tentativas; i++) {
            try {
                return new Socket(host, porta);
            } catch (IOException e) {
                ultima = e;
                System.out.printf("[%s] Tentativa %d/%d falhou. Aguardando...%n", id, i, tentativas);
                try {
                    Thread.sleep(Math.min(1000 * i, 5000));
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw ultima;
    }

    /**
     * Fecha todos os streams e sockets de forma segura.
     */
    public void fecharTodas() {
        // Fecha streams primeiro
        streams.forEach((destino, oos) -> {
            try {
                oos.close();
            } catch (IOException e) {
                System.err.printf("[%s] Erro ao fechar stream para %s: %s%n",
                        id, destino, e.getMessage());
            }
        });

        // Depois fecha sockets
        sockets.forEach((destino, socket) -> {
            try {
                socket.close();
            } catch (IOException e) {
                System.err.printf("[%s] Erro ao fechar socket para %s: %s%n",
                        id, destino, e.getMessage());
            }
        });

        streams.clear();
        sockets.clear();
        System.out.printf("[%s] ✓ Todas as conexões fechadas%n", id);
    }
}