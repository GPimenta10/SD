package Cruzamentos;

import Veiculo.*;
import PontosEntrada.PontoEntrada;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Thread dedicada a receber veículos e colocá-los nas filas certas.
 */
public class ProcessadorVeiculos extends Thread {

    private final ServerSocket serverSocket;
    private final Map<String, FilaVeiculos> filas;
    private final String id;

    public ProcessadorVeiculos(ServerSocket serverSocket,
                               Map<String, FilaVeiculos> filas,
                               String id) {
        super(id + "_Processador");
        this.serverSocket = serverSocket;
        this.filas = filas;
        this.id = id;
        setDaemon(true);
    }

    @Override
    public void run() {
        try {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratarCliente(socket)).start();
            }
        } catch (IOException e) {
            System.err.printf("[%s] Erro no processador: %s%n", id, e.getMessage());
        }
    }

    private void tratarCliente(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = in.readLine()) != null) processarMensagem(linha);
        } catch (IOException e) {
            System.err.printf("[%s] Erro ao ler cliente: %s%n", id, e.getMessage());
        }
    }

    private void processarMensagem(String msg) {
        String[] p = msg.split("\\|");
        if (p.length < 5 || !p[0].equals("VEICULO")) return;

        String idVeiculo = p[1];
        TipoVeiculo tipo = TipoVeiculo.valueOf(p[2]);
        PontoEntrada entrada = PontoEntrada.valueOf(p[3]);
        String destino = p[4];

        Veiculo v = new Veiculo(idVeiculo, tipo, entrada, List.of(destino));

        filas.forEach((direcao, fila) -> {
            if (direcao.endsWith("para_" + destino)) fila.adicionarVeiculo(v);
        });

        System.out.printf("[%s] Recebido %s (%s) para %s%n", id, idVeiculo, tipo, destino);
    }
}

