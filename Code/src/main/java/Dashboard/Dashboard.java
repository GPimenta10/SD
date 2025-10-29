package Dashboard;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Dashboard - Monitoriza o sistema de tráfego em tempo real.
 * Mostra número de veículos por entrada, percurso completo e tempos médios.
 * Mantém a janela aberta para visualização após o fim da simulação.
 */
public class Dashboard extends JFrame {

    private final int porta;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Componentes Swing
    private JLabel lblE1Count, lblE2Count, lblE3Count;
    private DefaultTableModel modeloVeiculos;
    private JTable tabelaVeiculos;
    private JTextArea areaEventos;

    // Contadores
    private int countE1 = 0, countE2 = 0, countE3 = 0;

    // Mapas de rastreamento
    private final Map<String, Integer> veiculosNaTabela = new HashMap<>();
    private final Map<String, StringBuilder> percursoVeiculos = new HashMap<>();

    public Dashboard(int porta) {
        this.porta = porta;
        inicializarInterface();
    }

    public static void main(String[] args) {
        int porta = args.length > 0 ? Integer.parseInt(args[0]) : 9000;
        Dashboard dash = new Dashboard(porta);
        dash.setVisible(true);
        new Thread(dash::iniciarServidor).start();
    }

    // ===================== INTERFACE =====================
    private void inicializarInterface() {
        setTitle("Dashboard - Sistema de Tráfego Urbano");
        setSize(1300, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        add(criarPainelEntradas(), BorderLayout.NORTH);
        add(criarPainelVeiculos(), BorderLayout.CENTER);
        add(criarPainelEventos(), BorderLayout.SOUTH);
    }

    private JPanel criarPainelEntradas() {
        JPanel painel = new JPanel(new GridLayout(1, 3, 10, 0));
        painel.setBorder(BorderFactory.createTitledBorder("Entradas de Veículos"));
        painel.setBackground(new Color(240, 240, 240));

        lblE1Count = criarPainelEntrada(painel, "E1", Color.BLUE);
        lblE2Count = criarPainelEntrada(painel, "E2", Color.GREEN.darker());
        lblE3Count = criarPainelEntrada(painel, "E3", Color.RED);
        return painel;
    }

    private JLabel criarPainelEntrada(JPanel painel, String nome, Color cor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(cor, 2));
        JLabel lblNome = new JLabel(nome, SwingConstants.CENTER);
        lblNome.setFont(new Font("Arial", Font.BOLD, 24));
        JLabel lblCount = new JLabel("0 veículos", SwingConstants.CENTER);
        lblCount.setFont(new Font("Arial", Font.PLAIN, 18));
        p.add(lblNome, BorderLayout.NORTH);
        p.add(lblCount, BorderLayout.CENTER);
        painel.add(p);
        return lblCount;
    }

    private JPanel criarPainelVeiculos() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Veículos em Circulação"));

        String[] colunas = {"ID", "Tipo", "Origem", "Localização Atual", "Percurso", "Espera Total", "Status"};
        modeloVeiculos = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelaVeiculos = new JTable(modeloVeiculos);
        tabelaVeiculos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabelaVeiculos.setRowHeight(25);
        tabelaVeiculos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        painel.add(new JScrollPane(tabelaVeiculos), BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarPainelEventos() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("Log de Eventos"));
        painel.setPreferredSize(new Dimension(0, 150));
        areaEventos = new JTextArea();
        areaEventos.setEditable(false);
        areaEventos.setFont(new Font("Monospaced", Font.PLAIN, 11));
        areaEventos.setBackground(Color.BLACK);
        areaEventos.setForeground(Color.GREEN);
        painel.add(new JScrollPane(areaEventos), BorderLayout.CENTER);
        return painel;
    }

    // ===================== REDE =====================
    private void iniciarServidor() {
        adicionarEvento("✓ Dashboard ativo na porta " + porta);
        try (ServerSocket serverSocket = new ServerSocket(porta)) {
            while (true) {
                Socket socket = serverSocket.accept();
                new Thread(() -> processarMensagem(socket)).start();
            }
        } catch (IOException e) {
            adicionarEvento("❌ Erro no servidor: " + e.getMessage());
        }
    }

    private void processarMensagem(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String linha;
            while ((linha = in.readLine()) != null) processarEvento(linha);
        } catch (IOException ignored) {
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // ===================== LÓGICA PRINCIPAL =====================
    private void processarEvento(String mensagem) {
        adicionarEvento(mensagem);

        // Entradas
        if (mensagem.contains("[E1]") && mensagem.contains("Enviado")) atualizarContador("E1", ++countE1, lblE1Count, mensagem);
        else if (mensagem.contains("[E2]") && mensagem.contains("Enviado")) atualizarContador("E2", ++countE2, lblE2Count, mensagem);
        else if (mensagem.contains("[E3]") && mensagem.contains("Enviado")) atualizarContador("E3", ++countE3, lblE3Count, mensagem);

        // Cruzamentos
        if (mensagem.contains("Recebido:")) atualizarLocalizacao(mensagem);

        // Saída final (veículo chegou ao fim)
        if (mensagem.contains("[Saída]") && mensagem.contains("Saída:")) registrarSaida(mensagem);
    }

    private void atualizarContador(String origem, int novoValor, JLabel lbl, String msg) {
        SwingUtilities.invokeLater(() -> lbl.setText(novoValor + " veículos"));
        extrairVeiculo(msg, origem);
    }

    private void extrairVeiculo(String mensagem, String origem) {
        String id = extrairID(mensagem);
        String tipo = extrairTipo(mensagem);
        if (id == null || tipo == null) return;

        SwingUtilities.invokeLater(() -> {
            Object[] linha = {id, tipo, origem, origem, origem, "-", "Em trânsito"};
            modeloVeiculos.addRow(linha);
            int index = modeloVeiculos.getRowCount() - 1;
            veiculosNaTabela.put(id, index);
            percursoVeiculos.put(id, new StringBuilder(origem));
        });
    }

    private void atualizarLocalizacao(String mensagem) {
        String cruzamento = extrairCruzamento(mensagem);
        String id = extrairID(mensagem);
        if (id == null || !veiculosNaTabela.containsKey(id)) return;

        SwingUtilities.invokeLater(() -> {
            int linha = veiculosNaTabela.get(id);
            modeloVeiculos.setValueAt(cruzamento, linha, 3);
            StringBuilder percurso = percursoVeiculos.get(id);
            percurso.append(" → ").append(cruzamento);
            modeloVeiculos.setValueAt(percurso.toString(), linha, 4);
        });
    }

    private void registrarSaida(String mensagem) {
        String id = extrairID(mensagem);
        String tempoSistema = extrairTempo(mensagem, "no sistema");
        String tempoEspera = extrairTempo(mensagem, "de espera");
        if (id == null || !veiculosNaTabela.containsKey(id)) return;

        SwingUtilities.invokeLater(() -> {
            int linha = veiculosNaTabela.get(id);

            // Atualiza o percurso para incluir a saída no fim
            StringBuilder percurso = percursoVeiculos.get(id);
            if (percurso != null && !percurso.toString().endsWith("Saída")) {
                percurso.append(" → Saída");
                modeloVeiculos.setValueAt(percurso.toString(), linha, 4);
            }

            // Atualiza os restantes campos
            modeloVeiculos.setValueAt("SAÍDA", linha, 3);
            modeloVeiculos.setValueAt(tempoEspera, linha, 5);
            modeloVeiculos.setValueAt("✓ Concluído (" + tempoSistema + ")", linha, 6);
        });
    }


    private void adicionarEvento(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalTime.now().format(TIME_FORMAT);
            areaEventos.append("[" + timestamp + "] " + mensagem + "\n");
            if (areaEventos.getLineCount() > 120)
                areaEventos.setText(areaEventos.getText().replaceFirst("(?s)^.*?\n", ""));
            areaEventos.setCaretPosition(areaEventos.getDocument().getLength());
        });
    }

    // ===================== EXTRAÇÃO DE DADOS =====================
    private String extrairCruzamento(String msg) {
        for (int i = 1; i <= 5; i++)
            if (msg.contains("[Cruzamento" + i + "]")) return "Cruzamento" + i;
        return "?";
    }

    private String extrairID(String msg) {
        for (String p : msg.split("\\s+"))
            if (p.matches("E[1-3]-\\d{3}")) return p;
        return null;
    }

    private String extrairTipo(String msg) {
        int ini = msg.indexOf("("), fim = msg.indexOf(")");
        return (ini >= 0 && fim > ini) ? msg.substring(ini + 1, fim) : "?";
    }

    private String extrairTempo(String msg, String marcador) {
        if (!msg.contains(marcador)) return "-";
        try {
            int idx = msg.indexOf(marcador);
            int ini = msg.lastIndexOf("-", idx);
            if (ini >= 0) {
                String parte = msg.substring(ini + 1, idx).trim();
                return parte + (marcador.contains("espera") ? "s" : "");
            }
        } catch (Exception ignored) {}
        return "-";
    }
}
