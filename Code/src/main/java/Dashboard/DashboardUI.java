package Dashboard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Janela principal do dashboard gráfico.
 * CORREÇÃO: Agora remove veículos do mapa quando chegam à saída.
 */
public class DashboardUI extends JFrame {

    private static final int PORTA = 9000;

    private final PainelResumo painelResumo;
    private final PainelMapa painelMapa;
    private final DefaultTableModel modeloTabela;
    private final JTable tabelaSaidas;

    private final ConcurrentHashMap<String, AtomicInteger> porEntrada = new ConcurrentHashMap<>();
    private final AtomicInteger totalEntradas = new AtomicInteger(0);
    private final AtomicInteger totalSaidas = new AtomicInteger(0);

    public DashboardUI() {
        super("🚦 Dashboard - Simulação de Tráfego Urbano");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        painelResumo = new PainelResumo();
        painelMapa = new PainelMapa();

        modeloTabela = new DefaultTableModel(
                new String[]{"ID", "Tipo", "Percurso", "Tempo (s)"}, 0);
        tabelaSaidas = new JTable(modeloTabela);

        JScrollPane scroll = new JScrollPane(tabelaSaidas);
        scroll.setBorder(BorderFactory.createTitledBorder("🚗 Veículos que saíram"));

        add(painelResumo, BorderLayout.NORTH);
        add(scroll, BorderLayout.CENTER);
        add(painelMapa, BorderLayout.SOUTH);

        iniciarServidor();
    }

    /**
     * Inicia o servidor socket para receber mensagens dos nós do sistema.
     */
    private void iniciarServidor() {
        Thread servidor = new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
                System.out.println("[Dashboard] Servidor gráfico ativo na porta " + PORTA);
                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(() -> processar(socket)).start();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        servidor.setDaemon(true);
        servidor.start();
    }

    /**
     * Processa cada conexão de cliente.
     */
    private void processar(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = in.readLine()) != null) {
                processarMensagem(linha.trim());
            }
        } catch (Exception e) {
            // Ignora erros de desconexão
        }
    }

    /**
     * Processa mensagens recebidas e encaminha para os painéis.
     */
    private void processarMensagem(String msg) {
        msg = msg.trim();

        if (msg.startsWith("[Entrada]")) {
            registrarEntrada(msg);
            painelMapa.adicionarVeiculo(msg);

        } else if (msg.startsWith("[Semaforo]")) {
            painelMapa.atualizarSemaforo(msg);

        } else if (msg.startsWith("[Saída]") && !msg.startsWith("[Saída_Total]")) {
            registrarSaida(msg);
            painelMapa.removerVeiculo(msg); // ✅ REMOVE veículo do mapa

        } else if (msg.startsWith("[Saída_Total]")) {
            atualizarTotalSaidas(msg);
        }
    }

    /**
     * Regista uma nova entrada de veículo.
     */
    private void registrarEntrada(String msg) {
        totalEntradas.incrementAndGet();

        String[] partes = msg.split(" ");
        String entrada = partes.length > 1 ? partes[1].trim() : "Desconhecida";

        porEntrada.putIfAbsent(entrada, new AtomicInteger(0));
        porEntrada.get(entrada).incrementAndGet();

        painelResumo.atualizar(totalEntradas.get(), porEntrada, totalSaidas.get());
    }

    /**
     * Regista um veículo que saiu e atualiza contador e tabela.
     * ✅ CORREÇÃO: Adiciona atraso antes de remover do mapa (para animação chegar ao nó S)
     */
    private void registrarSaida(String msg) {
        totalSaidas.incrementAndGet();
        painelResumo.atualizar(totalEntradas.get(), porEntrada, totalSaidas.get());

        try {
            String id = extrairValor(msg, "id=");
            String tipo = extrairValor(msg, "tipo=");
            String percurso = extrairValor(msg, "percurso=");
            String tempo = extrairValor(msg, "tempo=");

            SwingUtilities.invokeLater(() ->
                    modeloTabela.addRow(new Object[]{id, tipo, percurso, tempo})
            );

            System.out.printf("[Dashboard] ✅ Registrado saída: %s (%s) - %ss | Total: %d%n",
                    id, tipo, tempo, totalSaidas.get());

            // ✅ Remove do mapa com atraso de 2 segundos (tempo para animação chegar ao nó S)
            String finalId = id;
            new Thread(() -> {
                try {
                    Thread.sleep(2000); // 2 segundos = tempo suficiente para animação
                    painelMapa.removerVeiculo("[Saída] id=" + finalId);
                } catch (InterruptedException ignored) {}
            }).start();

        } catch (Exception e) {
            System.err.printf("[Dashboard] Erro ao processar saída: %s%n", e.getMessage());
        }
    }

    /**
     * Atualiza o total global de saídas.
     */
    private void atualizarTotalSaidas(String msg) {
        try {
            int total = Integer.parseInt(msg.replace("[Saída_Total]", "").trim());
            totalSaidas.set(total);
            painelResumo.atualizar(totalEntradas.get(), porEntrada, totalSaidas.get());
        } catch (NumberFormatException e) {
            System.err.printf("[Dashboard] Erro ao parsear total de saídas: %s%n", msg);
        }
    }

    /**
     * Extrai valor de uma mensagem baseada em chave.
     */
    private String extrairValor(String msg, String chave) {
        try {
            int i = msg.indexOf(chave);
            if (i == -1) return "";
            int start = i + chave.length();
            int end = msg.indexOf(' ', start);
            if (end == -1) end = msg.length();
            String valor = msg.substring(start, end).trim();
            return valor.replaceAll("[\\[\\],:]", "");
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Método principal – permite iniciar o Dashboard isoladamente ou via Main.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DashboardUI ui = new DashboardUI();
            ui.setVisible(true);
        });
    }
}