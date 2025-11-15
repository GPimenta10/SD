package Dashboard;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;

/**
 * Painel central melhorado com:
 * - Semáforos animados (verde/vermelho)
 * - Veículos que param quando semáforo vermelho
 * - Vias bidirecionais apenas entre Cr1↔Cr2 e Cr2↔Cr3
 */
public class PainelMapa extends JPanel {

    // chave é tipo "Cr3_E3-Cr3"
    private final Map<Integer, String> mapaIds = new HashMap<>();

    // === Veículos em trânsito ===
    private final List<VeiculoNoMapa> veiculosEmTransito = new CopyOnWriteArrayList<>();

    // === Estado dos semáforos (chave: "Cr3_E3-S", valor: true=VERDE, false=VERMELHO) ===
    private final Map<String, Boolean> estadosSemaforos = new ConcurrentHashMap<>();

    // === Posições dos nós ===
    private final Map<String, Point2D> posicoes = new HashMap<>();

    // === Posições dos semáforos (calculadas automaticamente) ===
    private final Map<String, Point2D> posicoesSemaforos = new HashMap<>();

    // Para garantir que cada veículo existe apenas uma vez
    private final Map<String, VeiculoNoMapa> veiculosPorId = new ConcurrentHashMap<>();


    // === Dimensões ===
    private static final int LARGURA_CRUZAMENTO = 60;
    private static final int ALTURA_CRUZAMENTO = 40;
    private static final int TAMANHO_VEICULO = 8;
    private static final int TAMANHO_SEMAFORO = 12;
    private static final int ESPACAMENTO_VIA_DUPLA = 4;

    private Timer animationTimer;

    public PainelMapa() {
        setPreferredSize(new Dimension(900, 550));
        setBackground(new Color(245, 245, 245));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "🗺️ Mapa do Sistema de Tráfego",
                0, 0,
                new Font("Arial", Font.BOLD, 14)
        ));

        inicializarPosicoes();
        inicializarPosicoesSemaforos();
        inicializarEstadosSemaforos();
        iniciarAnimacao();
    }

    private void inicializarPosicoes() {
        int margemX = 60;
        int margemY = 80;
        int espacoH = 300;
        int espacoV = 200;

        posicoes.put("E1", new Point2D.Double(margemX, margemY));
        posicoes.put("E2", new Point2D.Double(margemX + espacoH, margemY));
        posicoes.put("E3", new Point2D.Double(margemX + 2 * espacoH, margemY));

        posicoes.put("Cr1", new Point2D.Double(margemX, margemY + espacoV));
        posicoes.put("Cr2", new Point2D.Double(margemX + espacoH, margemY + espacoV));
        posicoes.put("Cr3", new Point2D.Double(margemX + 2 * espacoH, margemY + espacoV));

        posicoes.put("Cr4", new Point2D.Double(margemX, margemY + 2 * espacoV));
        posicoes.put("Cr5", new Point2D.Double(margemX + espacoH, margemY + 2 * espacoV));
        posicoes.put("S", new Point2D.Double(margemX + 2 * espacoH, margemY + 2 * espacoV));
    }

    /**
     * Calcula posições dos semáforos (logo antes de cada cruzamento)
     */
    private void inicializarPosicoesSemaforos() {
        posicoesSemaforos.put("Cr1_E1-Cr1", new Point2D.Double(45, 248));
        posicoesSemaforos.put("Cr1_Cr2-Cr1", new Point2D.Double(100, 260));

        posicoesSemaforos.put("Cr2_E2-Cr2", new Point2D.Double(345, 248));
        posicoesSemaforos.put("Cr2_Cr1-Cr2", new Point2D.Double(320, 300));
        posicoesSemaforos.put("Cr2_Cr3-Cr2", new Point2D.Double(400, 260));

        posicoesSemaforos.put("Cr3_E3-Cr3", new Point2D.Double(645, 248));
        posicoesSemaforos.put("Cr3_Cr2-Cr3", new Point2D.Double(620, 300));

        posicoesSemaforos.put("Cr4_Cr1-Cr4", new Point2D.Double(45, 448));

        posicoesSemaforos.put("Cr5_Cr2-Cr5", new Point2D.Double(345, 448));
        posicoesSemaforos.put("Cr5_Cr4-Cr5", new Point2D.Double(320, 490));
    }

    /**
     * Inicializa todos os semáforos como VERMELHO
     */
    private void inicializarEstadosSemaforos() {
        estadosSemaforos.put("Cr1_E1-Cr1", false);
        estadosSemaforos.put("Cr1_Cr2-Cr1", false);
        estadosSemaforos.put("Cr2_E2-Cr2", false);
        estadosSemaforos.put("Cr3_E3-Cr3", false);
        estadosSemaforos.put("Cr3_Cr2-Cr3", false);
        estadosSemaforos.put("Cr4_Cr1-Cr4", false);
        estadosSemaforos.put("Cr5_Cr2-Cr5", false);
        estadosSemaforos.put("Cr5_Cr4-Cr5", false);
    }

    /**
     * Atualiza estado de um semáforo (chamado pelo ThreadServidorDashboard)
     */
    public void atualizarSemaforo(String cruzamento, String origem, String destino, boolean verde) {
        String chave = cruzamento + "_" + origem + "-" + destino;
        estadosSemaforos.put(chave, verde);
        System.out.printf("[PainelMapa] Semáforo %s: %s%n", chave, verde ? "VERDE" : "VERMELHO");
    }

    private void iniciarAnimacao() {
        animationTimer = new Timer(16, e -> {
            Map<String, Integer> filaPorSemaforo = new HashMap<>();

            for (VeiculoNoMapa veiculo : veiculosEmTransito) {
                boolean semaforoVerde = estadosSemaforos.getOrDefault(veiculo.chaveSemaforo, true);

                // Se vermelho e não ultrapassou, conta na fila
                if (!semaforoVerde && !veiculo.ultrapassouSemaforo()) {
                    filaPorSemaforo.put(veiculo.chaveSemaforo,
                            filaPorSemaforo.getOrDefault(veiculo.chaveSemaforo, 0) + 1);
                }
            }

            // Atualiza cada veículo com sua posição na fila
            Map<String, Integer> contadorFila = new HashMap<>();

            for (VeiculoNoMapa veiculo : veiculosEmTransito) {
                boolean semaforoVerde = estadosSemaforos.getOrDefault(veiculo.chaveSemaforo, true);

                int posicaoFila = -1;
                if (!semaforoVerde && !veiculo.ultrapassouSemaforo()) {
                    posicaoFila = contadorFila.getOrDefault(veiculo.chaveSemaforo, 0);
                    contadorFila.put(veiculo.chaveSemaforo, posicaoFila + 1);
                }

                veiculo.atualizar(semaforoVerde, posicaoFila);
            }

            veiculosEmTransito.removeIf(VeiculoNoMapa::chegouAoDestino);
            repaint();
        });
        animationTimer.start();
    }

    /**
     * Adiciona veículo ao mapa
     */
    public void adicionarVeiculo(String id, String tipo, String origem, String destino) {
        Point2D posOrigem = posicoes.get(origem);
        Point2D posDestino = posicoes.get(destino);

        if (posOrigem != null && posDestino != null) {
            // Determina o semáforo a verificar
            String chaveSemaforo = destino + "_" + origem + "-" + destino;
            Point2D posSemaforo = posicoesSemaforos.get(chaveSemaforo);

            VeiculoNoMapa veiculo = new VeiculoNoMapa(id, tipo, posOrigem, posDestino,
                    posSemaforo, chaveSemaforo);
            veiculosEmTransito.add(veiculo);

            System.out.printf("[PainelMapa] Veículo %s (%s) adicionado: %s → %s (semáforo: %s)%n",
                    id, tipo, origem, destino, chaveSemaforo);
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        desenharVias(g2d);
        desenharNos(g2d);
        desenharSemaforos(g2d);
        desenharVeiculos(g2d);
        desenharLegenda(g2d);
    }

    private void desenharVias(Graphics2D g2d) {
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.setColor(new Color(100, 100, 100));

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
        g2d.setFont(new Font("Arial", Font.BOLD, 12));

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
        g2d.setStroke(new BasicStroke(2.0f));
        g2d.drawRoundRect(x, y, LARGURA_CRUZAMENTO, ALTURA_CRUZAMENTO, 10, 10);

        g2d.setColor(Color.WHITE);
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(label);
        int textX = x + (LARGURA_CRUZAMENTO - textWidth) / 2;
        int textY = y + (ALTURA_CRUZAMENTO + fm.getAscent()) / 2 - 2;
        g2d.drawString(label, textX, textY);
    }

    /**
     * Desenha os semáforos (círculos coloridos)
     */
    private void desenharSemaforos(Graphics2D g2d) {
        for (Map.Entry<String, Point2D> entry : posicoesSemaforos.entrySet()) {
            String chave = entry.getKey();
            Point2D pos = entry.getValue();
            boolean verde = estadosSemaforos.getOrDefault(chave, false);

            int x = (int) pos.getX() - TAMANHO_SEMAFORO / 2;
            int y = (int) pos.getY() - TAMANHO_SEMAFORO / 2;

            g2d.setColor(Color.WHITE);
            g2d.fillRect(x - 2, y - 2, TAMANHO_SEMAFORO + 4, TAMANHO_SEMAFORO + 4);

            // Cor do semáforo
            Color cor = verde ? new Color(76, 175, 80) : new Color(244, 67, 54);
            g2d.setColor(cor);
            g2d.fillOval(x, y, TAMANHO_SEMAFORO, TAMANHO_SEMAFORO);

            // Borda preta grossa
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawOval(x, y, TAMANHO_SEMAFORO, TAMANHO_SEMAFORO);
        }
    }

    private void desenharVeiculos(Graphics2D g2d) {
        for (VeiculoNoMapa veiculo : veiculosEmTransito) {
            int x = (int) veiculo.posicaoAtual.getX() - TAMANHO_VEICULO / 2;
            int y = (int) veiculo.posicaoAtual.getY() - TAMANHO_VEICULO / 2;

            g2d.setColor(veiculo.getCor());
            g2d.fillOval(x, y, TAMANHO_VEICULO, TAMANHO_VEICULO);

            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(1.0f));
            g2d.drawOval(x, y, TAMANHO_VEICULO, TAMANHO_VEICULO);

            // Indicador visual de veículo parado
            if (veiculo.parado) {
                g2d.setColor(Color.RED);
                g2d.setStroke(new BasicStroke(2.0f));
                g2d.drawRect(x - 2, y - 2, TAMANHO_VEICULO + 4, TAMANHO_VEICULO + 4);
            }
        }
    }

    private void desenharLegenda(Graphics2D g2d) {
        int x = 100;
        int y = 30;

        g2d.setFont(new Font("Arial", Font.PLAIN, 11));
        g2d.setColor(Color.DARK_GRAY);
        g2d.drawString("Legenda Veículos:", x, y);

        y += 20;
        desenharItemLegenda(g2d, x, y, new Color(255, 193, 7), "Mota");
        desenharItemLegenda(g2d, x + 80, y, new Color(33, 150, 243), "Carro");
        desenharItemLegenda(g2d, x + 160, y, new Color(244, 67, 54), "Camião");
    }

    private void desenharItemLegenda(Graphics2D g2d, int x, int y, Color cor, String label) {
        g2d.setColor(cor);
        g2d.fillOval(x, y - 6, 8, 8);
        g2d.setColor(Color.BLACK);
        g2d.drawOval(x, y - 6, 8, 8);
        g2d.drawString(label, x + 12, y);
    }

    public void pararAnimacao() {
        if (animationTimer != null) {
            animationTimer.stop();
        }
    }

    /**
     *
     *
     * @param cruzamento
     * @param id
     * @param verde
     */
    public void atualizarSemaforoPorId(String cruzamento, int id, boolean verde) {
        // O mapaIds contém o mapeamento de ID numérico para a chave visual.
        // Ex: 0 -> "Cr3_E3-Cr3"
        String chaveVisual = mapaIds.get(id);

        if (chaveVisual == null) {
            System.err.println("[PainelMapa] Falhou: semáforo ID " + id + " não registado!");
            return;
        }

        estadosSemaforos.put(chaveVisual, verde);

        // FORÇA A ATUALIZAÇÃO VISUAL
        repaint();

        System.out.println("[PainelMapa] Semáforo atualizado: " + chaveVisual + " = " + (verde ? "VERDE" : "VERMELHO"));
    }

    /**
     * Regista o ID de um semáforo com a sua chave visual no mapa.
     * Chamado uma única vez quando o dashboard recebe estatísticas iniciais.
     *
     * @param cruzamento
     * @param id
     * @param origem
     * @param destino
     */
    public void registarSemaforoId(String cruzamento, int id, String origem, String destino) {
        String chave = cruzamento + "_" + origem + "-" + destino;
        mapaIds.put(id, chave);
        // garante que o estado existe
        estadosSemaforos.putIfAbsent(chave, false);
        System.out.println("[PainelMapa] Registado ID=" + id + " -> " + chave);
    }

    public void criarVeiculo(String id, String tipo, String origem, String destino) {

        // Se já existe, não cria
        if (veiculosPorId.containsKey(id)) return;

        Point2D posOrigem = posicoes.get(origem);
        Point2D posDestino = posicoes.get(destino);

        if (posOrigem != null && posDestino != null) {

            String chaveSemaforo = destino + "_" + origem + "-" + destino;
            Point2D posSemaforo = posicoesSemaforos.get(chaveSemaforo);

            VeiculoNoMapa v = new VeiculoNoMapa(id, tipo, posOrigem, posDestino, posSemaforo, chaveSemaforo);
            veiculosEmTransito.add(v);
            veiculosPorId.put(id, v);

            System.out.printf("[PainelMapa] Veículo CRIADO: %s (%s) %s → %s%n",
                    id, tipo, origem, destino);
        }
    }

    public void atualizarDestino(String id, String origem, String destino) {

        VeiculoNoMapa v = veiculosPorId.get(id);
        if (v == null) return;

        Point2D posDestino = posicoes.get(destino);
        if (posDestino == null) return;

        // O DESTINO pode mudar sempre
        v.destino = posDestino;

        // Mas só podemos mudar a ORIGEM quando o movimento anterior terminou
        if (v.progresso >= 0.99) {
            v.origem = v.posicaoAtual;  // novo ponto real na estrada
            v.progresso = 0;            // reiniciar animação do novo segmento
            v.parado = false;
        }

        // Atualizar semáforo e posição de paragem
        String chaveSemaforo = destino + "_" + origem + "-" + destino;
        v.chaveSemaforo = chaveSemaforo;
        v.posicaoSemaforo = posicoesSemaforos.get(chaveSemaforo);
    }

    public void atualizarOuCriarVeiculo(String id, String tipo, String origem, String destino) {
        if (!veiculosPorId.containsKey(id)) {
            criarVeiculo(id, tipo, origem, destino);
        } else {
            atualizarDestino(id, origem, destino);
        }
    }
}