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
 * Mostra número de veículos por entrada, semáforos, percurso e tempos.
 */
public class Dashboard extends JFrame {

    private final int porta;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    // Labels de contagem
    private JLabel lblE1Count, lblE2Count, lblE3Count;
    private JLabel lblAtivos, lblConcluidos;

    // Tabelas
    private DefaultTableModel modeloAtivos, modeloConcluidos;
    private JTable tabelaAtivos, tabelaConcluidos;
    private JTextArea areaEventos;

    // Contadores
    private int countE1 = 0, countE2 = 0, countE3 = 0;
    private int countAtivos = 0, countConcluidos = 0;

    // Mapas sincronizados
    private final Map<String, Integer> linhaPorVeiculo = new HashMap<>();
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
        setSize(1400, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(10, 10));
        setLocationRelativeTo(null);

        add(criarPainelEntradas(), BorderLayout.NORTH);
        add(criarPainelTabelas(), BorderLayout.CENTER);
        add(criarPainelEventos(), BorderLayout.SOUTH);
    }

    private JPanel criarPainelEntradas() {
        JPanel painel = new JPanel(new GridLayout(2, 3, 10, 5));
        painel.setBorder(BorderFactory.createTitledBorder("Resumo de Entradas e Estado Geral"));
        painel.setBackground(new Color(245, 245, 245));

        lblE1Count = criarPainelEntrada(painel, "E1", Color.BLUE);
        lblE2Count = criarPainelEntrada(painel, "E2", new Color(0, 120, 0));
        lblE3Count = criarPainelEntrada(painel, "E3", Color.RED);

        lblAtivos = criarPainelEstado(painel, "Em Trânsito", new Color(255, 165, 0));
        lblConcluidos = criarPainelEstado(painel, "Concluídos", new Color(0, 150, 0));

        return painel;
    }

    private JLabel criarPainelEntrada(JPanel painel, String nome, Color cor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(cor, 2));
        JLabel lblNome = new JLabel(nome, SwingConstants.CENTER);
        lblNome.setFont(new Font("Arial", Font.BOLD, 20));
        JLabel lblCount = new JLabel("0 veículos", SwingConstants.CENTER);
        lblCount.setFont(new Font("Arial", Font.PLAIN, 16));
        p.add(lblNome, BorderLayout.NORTH);
        p.add(lblCount, BorderLayout.CENTER);
        painel.add(p);
        return lblCount;
    }

    private JLabel criarPainelEstado(JPanel painel, String nome, Color cor) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createLineBorder(cor, 2));
        JLabel lblNome = new JLabel(nome, SwingConstants.CENTER);
        lblNome.setFont(new Font("Arial", Font.BOLD, 18));
        JLabel lblValor = new JLabel("0", SwingConstants.CENTER);
        lblValor.setFont(new Font("Arial", Font.PLAIN, 16));
        p.add(lblNome, BorderLayout.NORTH);
        p.add(lblValor, BorderLayout.CENTER);
        painel.add(p);
        return lblValor;
    }

    private JPanel criarPainelTabelas() {
        JPanel painel = new JPanel(new GridLayout(2, 1, 0, 10));

        painel.add(criarTabelaAtivos());
        painel.add(criarTabelaConcluidos());

        return painel;
    }

    private JPanel criarTabelaAtivos() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("🚗 Veículos em Circulação"));

        String[] colunas = {"ID", "Tipo", "Origem", "Localização Atual", "Percurso", "Espera Total", "Status"};
        modeloAtivos = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelaAtivos = new JTable(modeloAtivos);
        tabelaAtivos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabelaAtivos.setRowHeight(25);
        tabelaAtivos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        painel.add(new JScrollPane(tabelaAtivos), BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarTabelaConcluidos() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("✅ Veículos Concluídos"));

        String[] colunas = {"ID", "Tipo", "Origem", "Percurso", "Tempo no Sistema (s)", "Tempo de Espera (s)"};
        modeloConcluidos = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelaConcluidos = new JTable(modeloConcluidos);
        tabelaConcluidos.setFont(new Font("Monospaced", Font.PLAIN, 12));
        tabelaConcluidos.setRowHeight(25);
        tabelaConcluidos.getTableHeader().setFont(new Font("Arial", Font.BOLD, 12));
        painel.add(new JScrollPane(tabelaConcluidos), BorderLayout.CENTER);
        return painel;
    }

    private JPanel criarPainelEventos() {
        JPanel painel = new JPanel(new BorderLayout());
        painel.setBorder(BorderFactory.createTitledBorder("📜 Log de Eventos"));
        painel.setPreferredSize(new Dimension(0, 160));
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
            while ((linha = in.readLine()) != null) processarEvento(linha.trim());
        } catch (IOException ignored) {
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // ===================== LÓGICA PRINCIPAL =====================
    private synchronized void processarEvento(String mensagem) {
        adicionarEvento(mensagem);

        // 1️⃣ Entradas
        if (mensagem.contains("[E1]") && mensagem.contains("Enviado")) atualizarContador("E1", ++countE1, lblE1Count, mensagem);
        else if (mensagem.contains("[E2]") && mensagem.contains("Enviado")) atualizarContador("E2", ++countE2, lblE2Count, mensagem);
        else if (mensagem.contains("[E3]") && mensagem.contains("Enviado")) atualizarContador("E3", ++countE3, lblE3Count, mensagem);

            // 2️⃣ Semáforos (ex: [Cruzamento3] [Semáforo E2] Veículo E2-004 - VERMELHO)
        else if (mensagem.contains("[Semáforo")) processarSemaforo(mensagem);

            // 3️⃣ Cruzamentos normais
        else if (mensagem.contains("Recebido:")) atualizarLocalizacao(mensagem);

            // 4️⃣ Saída final
        else if (mensagem.contains("[Saída]") && mensagem.contains("Saída:")) registrarSaida(mensagem);
    }

    private void atualizarContador(String origem, int novoValor, JLabel lbl, String msg) {
        SwingUtilities.invokeLater(() -> lbl.setText(novoValor + " veículos"));
        extrairVeiculo(msg, origem);
    }

    private void extrairVeiculo(String mensagem, String origem) {
        String id = extrairID(mensagem);
        String tipo = extrairTipo(mensagem);
        if (id == null || tipo == null) return;

        synchronized (this) {
            if (linhaPorVeiculo.containsKey(id)) return; // evitar duplicados

            SwingUtilities.invokeLater(() -> {
                Object[] linha = {id, tipo, origem, origem, origem, "-", "Em trânsito"};
                modeloAtivos.addRow(linha);
                int index = modeloAtivos.getRowCount() - 1;
                linhaPorVeiculo.put(id, index);
                percursoVeiculos.put(id, new StringBuilder(origem));
                lblAtivos.setText(String.valueOf(++countAtivos));
            });
        }
    }

    private void processarSemaforo(String mensagem) {
        String id = extrairID(mensagem);
        String cruzamento = extrairCruzamento(mensagem);
        String semaforo = extrairSemaforo(mensagem);
        String estado = mensagem.toUpperCase().contains("VERMELHO") ? "Vermelho" : "Verde";

        if (id == null || !linhaPorVeiculo.containsKey(id)) return;

        SwingUtilities.invokeLater(() -> {
            Integer linha = linhaPorVeiculo.get(id);
            if (linha == null || linha >= modeloAtivos.getRowCount()) return;

            String localizacao = cruzamento + " - Semáforo " + semaforo + " (" + estado + ")";
            modeloAtivos.setValueAt(localizacao, linha, 3);
            StringBuilder percurso = percursoVeiculos.get(id);
            if (percurso != null && !percurso.toString().contains(cruzamento + "(" + semaforo + ")"))
                percurso.append(" → ").append(cruzamento).append("(").append(semaforo).append(")");
            modeloAtivos.setValueAt(percurso.toString(), linha, 4);
            modeloAtivos.setValueAt(
                    estado.equals("Vermelho") ? "Aguardando verde" : "Passando",
                    linha, 6
            );
        });
    }

    private void atualizarLocalizacao(String mensagem) {
        String cruzamento = extrairCruzamento(mensagem);
        String id = extrairID(mensagem);
        if (id == null || !linhaPorVeiculo.containsKey(id)) return;

        SwingUtilities.invokeLater(() -> {
            Integer linha = linhaPorVeiculo.get(id);
            if (linha == null || linha >= modeloAtivos.getRowCount()) return;

            modeloAtivos.setValueAt(cruzamento, linha, 3);
            StringBuilder percurso = percursoVeiculos.get(id);
            if (percurso != null) {
                percurso.append(" → ").append(cruzamento);
                modeloAtivos.setValueAt(percurso.toString(), linha, 4);
            }
        });
    }

    private void registrarSaida(String mensagem) {
        String id = extrairID(mensagem);
        String tempoSistema = extrairTempo(mensagem, "no sistema");
        String tempoEspera = extrairTempo(mensagem, "de espera");
        if (id == null) return;

        SwingUtilities.invokeLater(() -> {
            Integer linha = linhaPorVeiculo.get(id);
            if (linha == null || linha >= modeloAtivos.getRowCount()) return;

            String tipo = (String) modeloAtivos.getValueAt(linha, 1);
            String origem = (String) modeloAtivos.getValueAt(linha, 2);
            String percurso = percursoVeiculos.getOrDefault(id, new StringBuilder("?")).append(" → Saída").toString();

            modeloConcluidos.addRow(new Object[]{id, tipo, origem, percurso, tempoSistema, tempoEspera});

            modeloAtivos.removeRow(linha);
            linhaPorVeiculo.remove(id);
            percursoVeiculos.remove(id);
            countAtivos--;
            countConcluidos++;
            lblAtivos.setText(String.valueOf(countAtivos));
            lblConcluidos.setText(String.valueOf(countConcluidos));
        });
    }

    // ===================== EXTRAÇÃO =====================
    private void adicionarEvento(String mensagem) {
        SwingUtilities.invokeLater(() -> {
            String timestamp = LocalTime.now().format(TIME_FORMAT);
            areaEventos.append("[" + timestamp + "] " + mensagem + "\n");
            if (areaEventos.getLineCount() > 200)
                areaEventos.setText(areaEventos.getText().replaceFirst("(?s)^.*?\n", ""));
            areaEventos.setCaretPosition(areaEventos.getDocument().getLength());
        });
    }

    private String extrairCruzamento(String msg) {
        for (int i = 1; i <= 5; i++)
            if (msg.contains("[Cruzamento" + i + "]")) return "Cruzamento" + i;
        return "?";
    }

    private String extrairSemaforo(String msg) {
        int i = msg.indexOf("[Semáforo ");
        if (i < 0) return "?";
        int f = msg.indexOf("]", i);
        if (f < 0) return "?";
        return msg.substring(i + 10, f);
    }

    private String extrairID(String msg) {
        for (String p : msg.split("\\s+"))
            if (p.matches("E[1-3]-\\d{3,}")) return p;
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
                return parte.replace("s", "") + "s";
            }
        } catch (Exception ignored) {}
        return "-";
    }
}
