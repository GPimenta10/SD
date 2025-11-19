package Dashboard.Paineis;

import Dashboard.DashboardFrame;
import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.geom.Point2D;
import java.util.*;
import java.util.List;
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
    private static final int TAMANHO_VEICULO = 10;
    private static final int TAMANHO_SEMAFORO = 12;
    private static final int ESPACAMENTO_VIA_DUPLA = 10;

    private Timer animationTimer;
    private DashboardFrame dashboard;

    public PainelMapa() {

        setPreferredSize(new Dimension(400, 480));

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

    private void iniciarAnimacao() {
        animationTimer = new Timer(0, e -> {

            Map<String, Integer> contadorFila = new HashMap<>();

            for (VeiculoNoMapa veiculo : veiculosEmTransito) {

                String chave = veiculo.getChaveSemaforo();
                boolean verde = estadosSemaforos.getOrDefault(chave, true);

                int posFila = -1;
                if (!verde && !veiculo.ultrapassouSemaforo()) {
                    posFila = contadorFila.getOrDefault(chave, 0);
                    contadorFila.put(chave, posFila + 1);
                }

                veiculo.atualizar(verde, posFila);
            }

            veiculosEmTransito.removeIf(VeiculoNoMapa::terminouTodosSegmentos);

            repaint();
        });

        animationTimer.start();
    }

    public void registarSemaforoId(String cruzamento, int id, String origem, String destino) {
        String chave = cruzamento + "_" + origem + "-" + destino;
        mapaIds.put(id, chave);
        estadosSemaforos.putIfAbsent(chave, false);
    }

    public void atualizarSemaforoPorId(String cruzamento, int id, boolean verde) {
        String chave = mapaIds.get(id);
        if (chave == null) return;

        estadosSemaforos.put(chave, verde);
        repaint();
    }

    public void atualizarOuCriarVeiculo(String id, String tipo, String origem, String destino) {

        if (!veiculosPorId.containsKey(id)) {
            criarVeiculo(id, tipo, origem, destino);
        } else {
            atualizarDestino(id, origem, destino);
        }
    }

    private void criarVeiculo(String id, String tipo, String origem, String destino) {

        Point2D posOrigem = posicoes.get(origem);
        Point2D posDestino = posicoes.get(destino);

        if (posOrigem == null || posDestino == null)
            return;

        Point2D[] ajust = calcularPosicoesAjustadas(origem, destino);
        String chaveSem = destino + "_" + origem + "-" + destino;

        VeiculoNoMapa v = new VeiculoNoMapa(
                id, tipo,
                posOrigem, posDestino,
                posicoesSemaforos.get(chaveSem),
                chaveSem,
                ajust[0], ajust[1]
        );

        veiculosPorId.put(id, v);
        veiculosEmTransito.add(v);
    }

    private void atualizarDestino(String id, String origem, String destino) {

        VeiculoNoMapa v = veiculosPorId.get(id);
        if (v == null) return;

        Point2D posOrigem = posicoes.get(origem);
        Point2D posDestino = posicoes.get(destino);

        if (posOrigem == null || posDestino == null)
            return;

        Point2D[] ajust = calcularPosicoesAjustadas(origem, destino);
        String chaveSem = destino + "_" + origem + "-" + destino;

        v.adicionarProximoSegmento(
                origem, destino, posOrigem, posDestino,
                chaveSem, posicoesSemaforos.get(chaveSem),
                ajust[0], ajust[1]
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharVias(g2);
        desenharNos(g2);
        desenharSemaforos(g2);
        desenharVeiculos(g2);
        desenharLegenda(g2);
    }

    private void inicializarPosicoes() {
        int espacoH = 225;
        int espacoV = 175;

        int margemX = 50;
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

        posicoesSemaforos.put("Cr1_E1-Cr1",  new Point2D.Double(35, 200));
        posicoesSemaforos.put("Cr1_Cr2-Cr1", new Point2D.Double(90, 210));

        posicoesSemaforos.put("Cr2_E2-Cr2",  new Point2D.Double(260, 200));
        posicoesSemaforos.put("Cr2_Cr1-Cr2", new Point2D.Double(233, 258));
        posicoesSemaforos.put("Cr2_Cr3-Cr2", new Point2D.Double(315, 210));

        posicoesSemaforos.put("Cr3_E3-Cr3",  new Point2D.Double(488, 200));
        posicoesSemaforos.put("Cr3_Cr2-Cr3", new Point2D.Double(458, 258));

        posicoesSemaforos.put("Cr4_Cr1-Cr4", new Point2D.Double(35, 375));

        posicoesSemaforos.put("Cr5_Cr2-Cr5", new Point2D.Double(260, 378));
        posicoesSemaforos.put("Cr5_Cr4-Cr5", new Point2D.Double(233, 425));
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
        double dist = Math.sqrt(dx*dx + dy*dy);

        double perpX = -dy / dist * ESPACAMENTO_VIA_DUPLA;
        double perpY = dx / dist * ESPACAMENTO_VIA_DUPLA;

        return new Point2D[]{
                new Point2D.Double(p1.getX() + perpX, p1.getY() + perpY),
                new Point2D.Double(p2.getX() + perpX, p2.getY() + perpY)
        };
    }

    private void desenharVias(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(2f));
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

    private void desenharViaSimples(Graphics2D g2d, String o, String d) {
        Point2D p1 = posicoes.get(o);
        Point2D p2 = posicoes.get(d);

        if (p1 == null || p2 == null) return;

        g2d.drawLine((int)p1.getX(), (int)p1.getY(), (int)p2.getX(), (int)p2.getY());
    }

    private void desenharViaBidirecional(Graphics2D g2d, String n1, String n2) {
        Point2D p1 = posicoes.get(n1);
        Point2D p2 = posicoes.get(n2);
        if (p1 == null || p2 == null) return;

        double dx = p2.getX() - p1.getX();
        double dy = p2.getY() - p1.getY();
        double dist = Math.sqrt(dx*dx + dy*dy);

        double perpX = -dy/dist * ESPACAMENTO_VIA_DUPLA;
        double perpY = dx/dist * ESPACAMENTO_VIA_DUPLA;

        Point2D a1 = new Point2D.Double(p1.getX() + perpX, p1.getY() + perpY);
        Point2D a2 = new Point2D.Double(p2.getX() + perpX, p2.getY() + perpY);

        g2d.drawLine((int)a1.getX(), (int)a1.getY(), (int)a2.getX(), (int)a2.getY());

        Point2D b1 = new Point2D.Double(p1.getX() - perpX, p1.getY() - perpY);
        Point2D b2 = new Point2D.Double(p2.getX() - perpX, p2.getY() - perpY);

        g2d.drawLine((int)b1.getX(), (int)b1.getY(), (int)b2.getX(), (int)b2.getY());
    }

    private void desenharNos(Graphics2D g2d) {

        desenharNo(g2d, "E1", new Color(76,175,80));
        desenharNo(g2d, "E2", new Color(76,175,80));
        desenharNo(g2d, "E3", new Color(76,175,80));

        Color azul = new Color(33,150,243);

        desenharNo(g2d, "Cr1", azul);
        desenharNo(g2d, "Cr2", azul);
        desenharNo(g2d, "Cr3", azul);
        desenharNo(g2d, "Cr4", azul);
        desenharNo(g2d, "Cr5", azul);

        desenharNo(g2d, "S", new Color(244,67,54));
    }

    private void desenharNo(Graphics2D g2d, String id, Color cor) {
        Point2D pos = posicoes.get(id);
        if (pos == null) return;

        int x = (int)pos.getX() - LARGURA_CRUZAMENTO/2;
        int y = (int)pos.getY() - ALTURA_CRUZAMENTO/2;

        g2d.setColor(cor);
        g2d.fillRoundRect(x,y,LARGURA_CRUZAMENTO,ALTURA_CRUZAMENTO,10,10);

        g2d.setColor(cor.darker());
        g2d.drawRoundRect(x,y,LARGURA_CRUZAMENTO,ALTURA_CRUZAMENTO,10,10);

        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int w = fm.stringWidth(id);

        g2d.drawString(id, x + (LARGURA_CRUZAMENTO-w)/2, y + ALTURA_CRUZAMENTO/2 + fm.getAscent()/2);
    }

    private void desenharSemaforos(Graphics2D g2d) {
        for (var entry : posicoesSemaforos.entrySet()) {

            String chave = entry.getKey();
            Point2D pos = entry.getValue();
            boolean verde = estadosSemaforos.getOrDefault(chave, false);

            int x = (int)pos.getX() - TAMANHO_SEMAFORO/2;
            int y = (int)pos.getY() - TAMANHO_SEMAFORO/2;

            g2d.setColor(UIManager.getColor("Panel.background"));
            g2d.fillOval(x-3,y-3,TAMANHO_SEMAFORO+6,TAMANHO_SEMAFORO+6);

            g2d.setColor(verde ? new Color(76,175,80) : new Color(244,67,54));
            g2d.fillOval(x,y,TAMANHO_SEMAFORO,TAMANHO_SEMAFORO);

            g2d.setColor(Color.DARK_GRAY);
            g2d.drawOval(x,y,TAMANHO_SEMAFORO,TAMANHO_SEMAFORO);
        }
    }

    private void desenharVeiculos(Graphics2D g2d) {
        for (VeiculoNoMapa v : veiculosEmTransito) {

            Point2D pos = v.getPosicaoAtual();
            int x = (int)pos.getX() - TAMANHO_VEICULO/2;
            int y = (int)pos.getY() - TAMANHO_VEICULO/2;

            g2d.setColor(v.getCor());
            g2d.fillOval(x,y,TAMANHO_VEICULO,TAMANHO_VEICULO);

            g2d.setColor(Color.BLACK);
            g2d.drawOval(x,y,TAMANHO_VEICULO,TAMANHO_VEICULO);

            if (v.isParado()) {
                g2d.setColor(Color.RED);
                g2d.drawRect(x-2,y-2,TAMANHO_VEICULO+4,TAMANHO_VEICULO+4);
            }
        }
    }

    private void desenharLegenda(Graphics2D g2d) {
        g2d.setFont(UIManager.getFont("Label.font"));
        g2d.setColor(UIManager.getColor("Label.foreground"));

        int y = getHeight() - 20;

        desenharItemLegenda(g2d, 50,  y, new Color(255,193,7), "Mota");
        desenharItemLegenda(g2d, 150, y, new Color(33,150,243), "Carro");
        desenharItemLegenda(g2d, 250, y, new Color(130,109,56), "Camião");
    }

    private void desenharItemLegenda(Graphics2D g2d, int x, int y, Color cor, String label) {

        g2d.setColor(UIManager.getColor("Panel.background"));
        g2d.fillRect(x-2, y-12, 20, 20);

        g2d.setColor(cor);
        g2d.fillOval(x, y-6, 8, 8);

        g2d.setColor(UIManager.getColor("Label.foreground"));
        g2d.drawString(label, x+12, y);
    }
}
