package Dashboard.Paineis;

import Dashboard.DashboardFrame;
import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class PainelMapa extends JPanel {

    private final Map<Integer, String> mapaIds = new HashMap<>();
    private final List<VeiculoNoMapa> veiculosEmTransito = new CopyOnWriteArrayList<>();
    private final Map<String, VeiculoNoMapa> veiculosPorId = new ConcurrentHashMap<>();
    private final Map<String, Boolean> estadosSemaforos = new ConcurrentHashMap<>();
    private final Map<String, Point2D> posicoes = new HashMap<>();
    public static final Map<String, Point2D> posicoesSemaforos = new HashMap<>();

    private static final int LARGURA_CRUZAMENTO = 60;
    private static final int ALTURA_CRUZAMENTO = 40;
    private static final int TAMANHO_VEICULO = 8;
    private static final int TAMANHO_SEMAFORO = 12;
    private static final int ESPACAMENTO_VIA_DUPLA = 10;

    private Timer animationTimer;
    private DashboardFrame dashboard;

    public PainelMapa() {

        setPreferredSize(new Dimension(400, 480));

        // MODERNO:
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Mapa do Sistema de Tráfego",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        inicializarPosicoes();
        inicializarPosicoesSemaforos();
        inicializarEstadosSemaforos();
        iniciarAnimacao();
    }

    public void setDashboard(DashboardFrame dashboard) {
        this.dashboard = dashboard;
    }

    public void registarSemaforoId(String cruzamento, int id, String origem, String destino) {
        String chave = cruzamento + "_" + origem + "-" + destino;
        mapaIds.put(id, chave);
        estadosSemaforos.putIfAbsent(chave, false);
    }

    public void atualizarSemaforoPorId(String cruzamento, int id, boolean verde) {
        String chaveVisual = mapaIds.get(id);

        if (chaveVisual == null) {
            System.err.println("[PainelMapa] Falhou: semáforo ID " + id + " não registado!");
            return;
        }

        estadosSemaforos.put(chaveVisual, verde);
        repaint();
    }

    public void atualizarOuCriarVeiculo(String id, String tipo, String origem, String destino) {
        if (!veiculosPorId.containsKey(id)) {
            criarVeiculo(id, tipo, origem, destino);
        } else {
            atualizarDestino(id, origem, destino);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharVias(g2d);
        desenharNos(g2d);
        desenharSemaforos(g2d);
        desenharVeiculos(g2d);
        desenharLegenda(g2d);
    }

    private void inicializarPosicoes() {
        int larguraMapa = 500;
        int espacoH = 175;
        int espacoV = 150;

        int larguraRede = espacoH * 2;
        int margemX = (larguraMapa - larguraRede) / 2 + 50;
        int margemY = 60;

        posicoes.put("E1", new Point2D.Double(margemX, margemY));
        posicoes.put("E2", new Point2D.Double(margemX + espacoH, margemY));
        posicoes.put("E3", new Point2D.Double(margemX + 2 * espacoH, margemY));

        posicoes.put("Cr1", new Point2D.Double(margemX, margemY + espacoV));
        posicoes.put("Cr2", new Point2D.Double(margemX + espacoH, margemY + espacoV));
        posicoes.put("Cr3", new Point2D.Double(margemX + 2 * espacoH, margemY + espacoV));

        posicoes.put("Cr4", new Point2D.Double(margemX, margemY + 2 * espacoV));
        posicoes.put("Cr5", new Point2D.Double(margemX + espacoH, margemY + 2 * espacoV));
        posicoes.put("S",   new Point2D.Double(margemX + 2 * espacoH, margemY + 2 * espacoV));
    }

    private void inicializarPosicoesSemaforos() {
        posicoesSemaforos.put("Cr1_E1-Cr1",  new Point2D.Double(110, 178));
        posicoesSemaforos.put("Cr1_Cr2-Cr1", new Point2D.Double(168, 188));
        posicoesSemaforos.put("Cr2_E2-Cr2",  new Point2D.Double(288, 178));
        posicoesSemaforos.put("Cr2_Cr1-Cr2", new Point2D.Double(258, 233));
        posicoesSemaforos.put("Cr2_Cr3-Cr2", new Point2D.Double(342, 188));
        posicoesSemaforos.put("Cr3_E3-Cr3",  new Point2D.Double(460, 178));
        posicoesSemaforos.put("Cr3_Cr2-Cr3", new Point2D.Double(435, 233));
        posicoesSemaforos.put("Cr4_Cr1-Cr4", new Point2D.Double(110, 328));
        posicoesSemaforos.put("Cr5_Cr2-Cr5", new Point2D.Double(288, 328));
        posicoesSemaforos.put("Cr5_Cr4-Cr5", new Point2D.Double(258, 370));
    }

    private void inicializarEstadosSemaforos() {
        estadosSemaforos.put("Cr1_E1-Cr1",  false);
        estadosSemaforos.put("Cr1_Cr2-Cr1", false);
        estadosSemaforos.put("Cr2_E2-Cr2",  false);
        estadosSemaforos.put("Cr3_E3-Cr3",  false);
        estadosSemaforos.put("Cr3_Cr2-Cr3", false);
        estadosSemaforos.put("Cr4_Cr1-Cr4", false);
        estadosSemaforos.put("Cr5_Cr2-Cr5", false);
        estadosSemaforos.put("Cr5_Cr4-Cr5", false);
    }

    private void iniciarAnimacao() {
        animationTimer = new Timer(16, e -> {
            Map<String, Integer> filaPorSemaforo = new HashMap<>();

            for (VeiculoNoMapa veiculo : veiculosEmTransito) {
                String chave = veiculo.getChaveSemaforo();
                boolean semaforoVerde = estadosSemaforos.getOrDefault(chave, true);

                if (!semaforoVerde && !veiculo.ultrapassouSemaforo()) {
                    filaPorSemaforo.put(chave, filaPorSemaforo.getOrDefault(chave, 0) + 1);
                }
            }

            Map<String, Integer> contadorFila = new HashMap<>();

            for (VeiculoNoMapa veiculo : veiculosEmTransito) {
                String chave = veiculo.getChaveSemaforo();
                boolean semaforoVerde = estadosSemaforos.getOrDefault(chave, true);

                int posicaoFila = -1;
                if (!semaforoVerde && !veiculo.ultrapassouSemaforo()) {
                    posicaoFila = contadorFila.getOrDefault(chave, 0);
                    contadorFila.put(chave, posicaoFila + 1);
                }

                veiculo.atualizar(semaforoVerde, posicaoFila);
            }

            for (VeiculoNoMapa veiculo : veiculosEmTransito) {
                if (veiculo.terminouTodosSegmentos()) {
                    processarSaidaVeiculo(veiculo);
                }
            }

            veiculosEmTransito.removeIf(VeiculoNoMapa::terminouTodosSegmentos);

            repaint();
        });

        animationTimer.start();
    }

    private void processarSaidaVeiculo(VeiculoNoMapa veiculo) {
        if (dashboard == null) return;

        String id = veiculo.getId();
        String tipo = veiculo.getTipo();
        long dwellingTimeSegundos = veiculo.getDwellingTimeSegundos();

        dashboard.getPainelEstatisticasTipo().atualizarEstatisticasSaida(tipo, dwellingTimeSegundos);
        veiculosPorId.remove(id);
    }

    private void desenharVias(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(2.0f));

        // Cor moderna FlatLaf
        g2d.setColor(UIManager.getColor("Separator.foreground"));

        desenharViaSimples(g2d, "E1", "Cr1");
        desenharViaSimples(g2d, "E2", "Cr2");
        desenharViaSimples(g2d, "E3", "Cr3");

        desenharViaBidirecional(g2d, "Cr1", "Cr2");
        desenharViaBidirecional(g2d, "Cr2", "Cr3");

        desenharViaSimples(g2d, "Cr1", "Cr4");
        desenharViaSimples(g2d, "Cr2", "Cr5");
        desenharViaSimples(g2d, "Cr3", "S");

        desenharViaSimples(g2d, "Cr4", "Cr5");
        desenharViaSimples(g2d, "Cr5", "S");
    }

    private void desenharViaSimples(Graphics2D g2d, String origem, String destino) {
        Point2D p1 = posicoes.get(origem);
        Point2D p2 = posicoes.get(destino);

        if (p1 != null && p2 != null) {
            g2d.drawLine((int) p1.getX(), (int) p1.getY(),
                    (int) p2.getX(), (int) p2.getY());
            desenharSeta(g2d, p1, p2);
        }
    }

    private void desenharViaBidirecional(Graphics2D g2d, String no1, String no2) {
        Point2D p1 = posicoes.get(no1);
        Point2D p2 = posicoes.get(no2);

        if (p1 == null || p2 == null) return;

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double distancia = Math.sqrt(dx * dx + dy * dy);

        double perpX = -dy / distancia * ESPACAMENTO_VIA_DUPLA;
        double perpY = dx / distancia * ESPACAMENTO_VIA_DUPLA;

        Point2D p1a = new Point2D.Double(p1.getX() + perpX, p1.getY() + perpY);
        Point2D p2a = new Point2D.Double(p2.getX() + perpX, p2.getY() + perpY);

        g2d.drawLine((int) p1a.getX(), (int) p1a.getY(),
                (int) p2a.getX(), (int) p2a.getY());
        desenharSeta(g2d, p1a, p2a);

        Point2D p1b = new Point2D.Double(p1.getX() - perpX, p1.getY() - perpY);
        Point2D p2b = new Point2D.Double(p2.getX() - perpX, p2.getY() - perpY);

        g2d.drawLine((int) p1b.getX(), (int) p1b.getY(),
                (int) p2b.getX(), (int) p2b.getY());
        desenharSeta(g2d, p2b, p1b);
    }

    private void desenharSeta(Graphics2D g2d, Point2D origem, Point2D destino) {
        double dx = destino.getX() - origem.getX();
        double dy = destino.getY() - origem.getY();
        double angulo = Math.atan2(dy, dx);

        int tamanhoSeta = 10;

        int x = (int) (origem.getX() + dx * 0.7);
        int y = (int) (origem.getY() + dy * 0.7);

        int x1 = (int) (x - tamanhoSeta * Math.cos(angulo - Math.PI / 6));
        int y1 = (int) (y - tamanhoSeta * Math.sin(angulo - Math.PI / 6));

        int x2 = (int) (x - tamanhoSeta * Math.cos(angulo + Math.PI / 6));
        int y2 = (int) (y - tamanhoSeta * Math.sin(angulo + Math.PI / 6));

        g2d.drawLine(x, y, x1, y1);
        g2d.drawLine(x, y, x2, y2);
    }

    private void desenharNos(Graphics2D g2d) {
        g2d.setFont(UIManager.getFont("Label.font"));

        desenharNo(g2d, "E1", new Color(76, 175, 80), "E1");
        desenharNo(g2d, "E2", new Color(76, 175, 80), "E2");
        desenharNo(g2d, "E3", new Color(76, 175, 80), "E3");

        Color corCruzamento = new Color(33, 150, 243);

        desenharNo(g2d, "Cr1", corCruzamento, "Cr1");
        desenharNo(g2d, "Cr2", corCruzamento, "Cr2");
        desenharNo(g2d, "Cr3", corCruzamento, "Cr3");
        desenharNo(g2d, "Cr4", corCruzamento, "Cr4");
        desenharNo(g2d, "Cr5", corCruzamento, "Cr5");

        desenharNo(g2d, "S", new Color(244, 67, 54), "S");
    }

    private void desenharNo(Graphics2D g2d, String id, Color cor, String label) {
        Point2D pos = posicoes.get(id);
        if (pos == null) return;

        int x = (int) pos.getX() - LARGURA_CRUZAMENTO / 2;
        int y = (int) pos.getY() - ALTURA_CRUZAMENTO / 2;

        g2d.setColor(cor);
        g2d.fillRoundRect(x, y, LARGURA_CRUZAMENTO, ALTURA_CRUZAMENTO, 10, 10);

        g2d.setColor(cor.darker());
        g2d.drawRoundRect(x, y, LARGURA_CRUZAMENTO, ALTURA_CRUZAMENTO, 10, 10);

        // 👉 TEXTO A BRANCO
        g2d.setColor(Color.WHITE);

        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);

        int textX = x + (LARGURA_CRUZAMENTO - textWidth) / 2;
        int textY = y + (ALTURA_CRUZAMENTO + fm.getAscent()) / 2 - 2;

        g2d.drawString(label, textX, textY);
    }

    private void desenharSemaforos(Graphics2D g2d) {

        for (Map.Entry<String, Point2D> entry : posicoesSemaforos.entrySet()) {

            String chave = entry.getKey();
            Point2D pos = entry.getValue();
            boolean verde = estadosSemaforos.getOrDefault(chave, false);

            int x = (int) pos.getX() - TAMANHO_SEMAFORO / 2;
            int y = (int) pos.getY() - TAMANHO_SEMAFORO / 2;

            // Fundo no estilo OneDark
            g2d.setColor(UIManager.getColor("Panel.background"));
            g2d.fillOval(x - 4, y - 4, TAMANHO_SEMAFORO + 8, TAMANHO_SEMAFORO + 8);

            // Semáforo
            Color cor = verde ? new Color(76, 175, 80) : new Color(244, 67, 54);
            g2d.setColor(cor);
            g2d.fillOval(x, y, TAMANHO_SEMAFORO, TAMANHO_SEMAFORO);

            g2d.setColor(Color.DARK_GRAY);
            g2d.drawOval(x, y, TAMANHO_SEMAFORO, TAMANHO_SEMAFORO);
        }
    }

    private void desenharVeiculos(Graphics2D g2d) {

        for (VeiculoNoMapa veiculo : veiculosEmTransito) {

            Point2D pos = veiculo.getPosicaoAtual();

            int x = (int) pos.getX() - TAMANHO_VEICULO / 2;
            int y = (int) pos.getY() - TAMANHO_VEICULO / 2;

            g2d.setColor(veiculo.getCor());
            g2d.fillOval(x, y, TAMANHO_VEICULO, TAMANHO_VEICULO);

            g2d.setColor(Color.BLACK);
            g2d.drawOval(x, y, TAMANHO_VEICULO, TAMANHO_VEICULO);

            if (veiculo.isParado()) {
                g2d.setColor(Color.RED);
                g2d.drawRect(x - 2, y - 2, TAMANHO_VEICULO + 4, TAMANHO_VEICULO + 4);
            }
        }
    }

    private void desenharLegenda(Graphics2D g2d) {
        int x = 15;
        int y = 50;

        g2d.setFont(UIManager.getFont("Label.font"));
        g2d.setColor(UIManager.getColor("Label.foreground"));
        g2d.drawString("Legenda:", x, y);

        int yItem = y + 20;

        desenharItemLegenda(g2d, x, yItem, new Color(255, 193, 7), "Mota");
        desenharItemLegenda(g2d, x, yItem + 25, new Color(33, 150, 243), "Carro");
        desenharItemLegenda(g2d, x, yItem + 50, new Color(244, 67, 54), "Camião");
    }

    private void desenharItemLegenda(Graphics2D g2d, int x, int y, Color cor, String label) {
        g2d.setColor(UIManager.getColor("Panel.background"));
        g2d.fillRect(x - 2, y - 12, 20, 20);

        g2d.setColor(cor);
        g2d.fillOval(x, y - 6, 8, 8);

        g2d.setColor(UIManager.getColor("Label.foreground"));
        g2d.drawString(label, x + 12, y);
    }

    private void criarVeiculo(String id, String tipo, String origem, String destino) {
        if (veiculosPorId.containsKey(id)) return;

        Point2D posOrigem = posicoes.get(origem);
        Point2D posDestino = posicoes.get(destino);

        if (posOrigem != null && posDestino != null) {

            Point2D[] posicoesAjustadas = calcularPosicoesAjustadas(origem, destino);

            String chaveSemaforo = destino + "_" + origem + "-" + destino;
            Point2D posSemaforo = posicoesSemaforos.get(chaveSemaforo);

            VeiculoNoMapa veiculo = new VeiculoNoMapa(
                    id, tipo, posOrigem, posDestino, posSemaforo,
                    chaveSemaforo, posicoesAjustadas[0], posicoesAjustadas[1]
            );

            veiculosEmTransito.add(veiculo);
            veiculosPorId.put(id, veiculo);
        }
    }

    private void atualizarDestino(String idVeiculo, String origem, String destino) {
        VeiculoNoMapa veiculo = veiculosPorId.get(idVeiculo);
        if (veiculo == null) return;

        Point2D posDestino = posicoes.get(destino);
        if (posDestino == null) return;

        Point2D posOrigem = posicoes.get(origem);
        Point2D[] posicoesAjustadas = calcularPosicoesAjustadas(origem, destino);

        String chaveSemaforo = destino + "_" + origem + "-" + destino;
        Point2D posSemaforo = posicoesSemaforos.get(chaveSemaforo);

        veiculo.adicionarProximoSegmento(
                origem, destino, posOrigem, posDestino,
                chaveSemaforo, posSemaforo,
                posicoesAjustadas[0], posicoesAjustadas[1]
        );
    }

    private Point2D[] calcularPosicoesAjustadas(String origem, String destino) {
        Point2D p1 = posicoes.get(origem);
        Point2D p2 = posicoes.get(destino);

        if (p1 == null || p2 == null) return new Point2D[]{p1, p2};

        boolean isBidirecional =
                (origem.equals("Cr1") && destino.equals("Cr2")) ||
                        (origem.equals("Cr2") && destino.equals("Cr1")) ||
                        (origem.equals("Cr2") && destino.equals("Cr3")) ||
                        (origem.equals("Cr3") && destino.equals("Cr2"));

        if (!isBidirecional) return new Point2D[]{p1, p2};

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double distancia = Math.sqrt(dx*dx + dy*dy);

        double perpX = -dy / distancia * ESPACAMENTO_VIA_DUPLA;
        double perpY = dx / distancia * ESPACAMENTO_VIA_DUPLA;

        Point2D p1Adj = new Point2D.Double(p1.getX() + perpX, p1.getY() + perpY);
        Point2D p2Adj = new Point2D.Double(p2.getX() + perpX, p2.getY() + perpY);

        return new Point2D[]{p1Adj, p2Adj};
    }
}
