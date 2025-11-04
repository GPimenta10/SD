package Cruzamentos;

import Veiculo.Veiculo;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

/**
 * Thread dedicada a receber veículos serializados e colocá-los nas filas.
 *
 * ✅ NOVO: Rejeita veículos se fila estiver cheia (gera SocketException no emissor)
 * ✅ Routing inteligente baseado no caminho do veículo
 * ✅ Thread-safe
 */
public class ProcessadorVeiculos extends Thread {

    private final ServerSocket serverSocket;
    private final Map<String, FilaVeiculos> filas;
    private final String id;

    public ProcessadorVeiculos(ServerSocket serverSocket, Map<String, FilaVeiculos> filas, String id) {
        super(id + "_Processador");
        this.serverSocket = serverSocket;
        this.filas = filas;
        this.id = id;
        setDaemon(true);
    }

    @Override
    public void run() {
        System.out.printf("[%s] 📡 Processador iniciado%n", id);

        try {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> tratarCliente(socket), id + "_Cliente").start();
            }
        } catch (IOException e) {
            if (!serverSocket.isClosed()) {
                System.err.printf("[%s] ✗ Erro no processador: %s%n", id, e.getMessage());
            }
        }
    }

    /**
     * Trata cada conexão de cliente (recebe veículos serializados).
     */
    private void tratarCliente(Socket socket) {
        try (ObjectInputStream ois = new ObjectInputStream(socket.getInputStream())) {
            System.out.printf("[%s] 🔌 Cliente conectado%n", id);

            while (true) {
                try {
                    Object obj = ois.readObject();

                    if (!(obj instanceof Veiculo veiculo)) {
                        System.out.printf("[%s] ⚠️ Objeto não-Veiculo ignorado: %s%n",
                                id, obj == null ? "null" : obj.getClass().getName());
                        continue;
                    }

                    // ✅ Processa e verifica se foi aceito
                    boolean aceito = processarVeiculo(veiculo);

                    if (!aceito) {
                        // ⚠️ Fecha conexão para sinalizar backpressure
                        System.err.printf("[%s] 🚫 Fila cheia! Rejeitando %s e fechando conexão%n",
                                id, veiculo.getId());
                        break; // Sai do loop, fecha socket
                    }

                } catch (EOFException e) {
                    System.out.printf("[%s] 🔌 Cliente desconectado%n", id);
                    break;
                } catch (ClassNotFoundException e) {
                    System.err.printf("[%s] ✗ Classe Veiculo não encontrada: %s%n",
                            id, e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.printf("[%s] ✗ Erro ao processar cliente: %s%n", id, e.getMessage());
        }
    }

    /**
     * Processa veículo recebido e coloca na fila apropriada.
     *
     * @return true se aceito, false se fila cheia
     */
    private boolean processarVeiculo(Veiculo veiculo) {
        String chave = determinarFila(veiculo);

        if (chave == null) {
            System.err.printf("[%s] ✗ Nenhuma fila encontrada para %s (caminho: %s)%n",
                    id, veiculo.getId(), veiculo.getCaminho());
            return false;
        }

        FilaVeiculos fila = filas.get(chave);
        if (fila == null) {
            System.err.printf("[%s] ✗ Fila '%s' não existe para %s%n",
                    id, chave, veiculo.getId());
            return false;
        }

        // ✅ Tenta adicionar (retorna false se cheia)
        boolean sucesso = fila.adicionarVeiculo(veiculo);

        if (sucesso) {
            System.out.printf("[%s] ✓ Veículo %s → fila '%s' [%d/%d]%n",
                    id, veiculo.getId(), chave,
                    fila.getTamanhoAtual(), 10);
        } else {
            System.err.printf("[%s] 🚫 FILA CHEIA! Veículo %s rejeitado na fila '%s'%n",
                    id, veiculo.getId(), chave);
        }

        return sucesso;
    }

    /**
     * Determina a fila correta baseada no caminho do veículo.
     *
     * Estratégia:
     * 1. Localiza posição atual no caminho
     * 2. Define origem e destino
     * 3. Monta chave "de_<origem>_para_<destino>"
     */
    private String determinarFila(Veiculo veiculo) {
        List<String> caminho = veiculo.getCaminho();
        int idxAtual = caminho.indexOf(id);

        String origem;
        String destino;

        if (idxAtual >= 0) {
            // Cruzamento encontrado no caminho
            origem = (idxAtual == 0)
                    ? veiculo.getPontoEntrada().name()
                    : caminho.get(idxAtual - 1);

            destino = (idxAtual + 1 < caminho.size())
                    ? caminho.get(idxAtual + 1)
                    : "S";
        } else {
            // Fallback: usa próximo nó do veículo
            origem = veiculo.getPontoEntrada().name();
            destino = veiculo.getProximoNo();

            System.out.printf("[%s] ⚠️ Cruzamento não encontrado no caminho %s. " +
                            "Usando fallback: %s → %s%n",
                    id, caminho, origem, destino);
        }

        String chaveExata = "de_" + origem + "_para_" + destino;

        // Tenta encontrar chave exata
        if (filas.containsKey(chaveExata)) {
            return chaveExata;
        }
        // Fallback: procura chave compatível (case-insensitive)
        return encontrarChaveCompativel(origem, destino);
    }

    /**
     * Procura chave compatível ignorando diferenças triviais.
     */
    private String encontrarChaveCompativel(String origem, String destino) {
        String alvoLower = ("de_" + origem + "_para_" + destino).toLowerCase(Locale.ROOT);

        // Tentativa 1: match exato (case-insensitive)
        for (String chave : filas.keySet()) {
            if (chave != null && chave.toLowerCase(Locale.ROOT).equals(alvoLower)) {
                return chave;
            }
        }
        // Tentativa 2: match parcial
        for (String chave : filas.keySet()) {
            if (chave == null) continue;
            String chaveLower = chave.toLowerCase(Locale.ROOT);

            if (chaveLower.contains(origem.toLowerCase()) &&
                    chaveLower.contains(destino.toLowerCase())) {
                System.out.printf("[%s] ⚠️ Usando match parcial: '%s' para origem=%s destino=%s%n",
                        id, chave, origem, destino);
                return chave;
            }
        }
        // Tentativa 3: qualquer fila que termina em destino
        for (String chave : filas.keySet()) {
            if (chave != null && chave.endsWith("para_" + destino)) {
                System.out.printf("[%s] ⚠️ Usando fallback por destino: '%s'%n", id, chave);
                return chave;
            }
        }
        return null; // Nenhuma fila encontrada
    }
}