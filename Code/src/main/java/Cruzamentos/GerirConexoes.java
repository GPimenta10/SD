package Cruzamentos;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Responsável por conectar o cruzamento aos próximos nós.
 */
public class GerirConexoes {

    private final String id;
    private final Map<String, Integer> destinos = new ConcurrentHashMap<>();
    private final Map<String, Socket> sockets = new ConcurrentHashMap<>();

    public GerirConexoes(String id) {
        this.id = id;
    }

    public void registarDestino(String nome, int porta) {
        destinos.put(nome, porta);
    }

    public void estabelecerConexoes(Map<String, Semaforo> semaforos) {
        destinos.forEach((destino, porta) -> {
            try {
                Socket socket = conectarComRetry("localhost", porta, 5);
                sockets.put(destino, socket);
                System.out.printf("[%s] Conectado a %s (porta %d)%n", id, destino, porta);

                PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                semaforos.forEach((direcao, s) -> {
                    if (direcao.endsWith("para_" + destino)) s.setSocketSaida(out);
                });
            } catch (IOException e) {
                System.err.printf("[%s] Falha ao conectar a %s: %s%n", id, destino, e.getMessage());
            }
        });
    }

    private Socket conectarComRetry(String host, int porta, int tentativas) throws IOException {
        IOException ultima = null;
        for (int i = 1; i <= tentativas; i++) {
            try {
                return new Socket(host, porta);
            } catch (IOException e) {
                ultima = e;
                System.out.printf("[%s] Tentativa %d/%d falhou. A aguardar...\n", id, i, tentativas);
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
            }
        }
        throw ultima;
    }

    public void fecharTodas() {
        sockets.values().forEach(s -> { try { s.close(); } catch (IOException ignored) {} });
    }
}
