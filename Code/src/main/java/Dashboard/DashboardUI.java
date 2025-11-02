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
 * Recebe mensagens de Entrada, Saída e Semáforos.
 * Atualiza os painéis em tempo real.
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
     * Processa cada conexão de cliente (Entrada, Saída, Cruzamento...).
     */
    private void processar(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = in.readLine()) != null) {
                processarMensagem(linha.trim());
            }
        } catch (Exception ignored) {}
    }

    /**
     * Processa mensagens recebidas e encaminha para os painéis correspondentes.
     */
    private void processarMensagem(String msg) {
        msg = msg.trim();

        if (msg.startsWith("[Entrada]")) {
            registrarEntrada(msg);
            painelMapa.adicionarVeiculo(msg);
        } else if (msg.startsWith("[Semaforo]")) {
            painelMapa.atualizarSemaforo(msg);
        } else if (msg.startsWith("[Saída]")) {
            registrarSaida(msg); // ✅ processa saídas aqui
        } else if (msg.startsWith("[Saída_Total]")) {
            atualizarTotalSaidas(msg);
        }
    }

    /**
     * Regista uma nova entrada de veículo.
     * Exemplo de mensagem: [Entrada] E3 tipo=CARRO
     */
    private void registrarEntrada(String msg) {
        totalEntradas.incrementAndGet(); // soma +1 ao total

        // Descobre qual entrada (E1, E2 ou E3)
        String[] partes = msg.split(" ");
        String entrada = partes.length > 1 ? partes[1].trim() : "Desconhecida";

        porEntrada.putIfAbsent(entrada, new AtomicInteger(0));
        porEntrada.get(entrada).incrementAndGet();

        painelResumo.atualizar(totalEntradas.get(), porEntrada, totalSaidas.get());
    }

    /**
     * Regista um veículo que saiu e atualiza contador e tabela.
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
        } catch (Exception ignored) {}
    }

    /**
     * Atualiza o total global de saídas (caso receba [Saída_Total]).
     */
    private void atualizarTotalSaidas(String msg) {
        try {
            int total = Integer.parseInt(msg.replace("[Saída_Total]", "").trim());
            totalSaidas.set(total);
            painelResumo.atualizar(totalEntradas.get(), porEntrada, totalSaidas.get());
        } catch (NumberFormatException ignored) {}
    }

    /**
     * Extrai um valor de uma mensagem baseada em chave.
     * Exemplo: extrairValor("[Saída] id=123 tipo=MOTO", "tipo=") → "MOTO"
     */
    private String extrairValor(String msg, String chave) {
        try {
            int i = msg.indexOf(chave);
            if (i == -1) return "";
            int start = i + chave.length();
            int end = msg.indexOf(' ', start);
            if (end == -1) end = msg.length();
            String valor = msg.substring(start, end).trim();

            // Remove eventuais símbolos extras ([ ] , :)
            valor = valor.replaceAll("[\\[\\]]", "");
            return valor;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Método principal — permite iniciar o Dashboard isoladamente ou via Main.
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DashboardUI ui = new DashboardUI();
            ui.setVisible(true);
        });
    }
}
