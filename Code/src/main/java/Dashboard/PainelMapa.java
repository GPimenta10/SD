package Dashboard;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Painel gráfico com mapa 2D melhorado.
 *
 * ✅ Cruzamentos com cores distintas
 * ✅ Legenda lateral com identificação de cores
 * ✅ Linhas maiores para acomodar 10 veículos
 * ✅ Sem deslocamento lateral (todos os veículos na mesma linha)
 * ✅ Sem labels nos cruzamentos (cores identificam)
 */
public class PainelMapa extends JPanel {

    private final ConcurrentHashMap<String, VeiculoAnimado> veiculos = new ConcurrentHashMap<>();
    private final Map<String, SemaforoVisual> semaforos = new HashMap<>();

    // Cores dos cruzamentos (paleta distinta e visível)
    private static final Map<String, Color> CORES_CRUZAMENTOS = Map.of(
            "Cr1", new Color(30, 144, 255),    // Azul Dodger
            "Cr2", new Color(34, 139, 34),     // Verde Floresta
            "Cr3", new Color(220, 20, 60),     // Vermelho Carmesim
            "Cr4", new Color(255, 140, 0),     // Laranja Escuro
            "Cr5", new Color(138, 43, 226)     // Roxo Azul-Violeta
    );

    // Dimensões aumentadas para acomodar filas
    private static final int LARGURA_PAINEL = 1100;
    private static final int ALTURA_PAINEL = 550;
    private static final int LARGURA_LEGENDA = 180;
    private static final int OFFSET_MAPA_X = LARGURA_LEGENDA + 30;

    // Posições dos nós (ajustadas para o novo tamanho)
    private final Map<String, Point> nos = Map.ofEntries(
            Map.entry("E1", new Point(OFFSET_MAPA_X + 100, 80)),
            Map.entry("E2", new Point(OFFSET_MAPA_X + 400, 80)),
            Map.entry("E3", new Point(OFFSET_MAPA_X + 700, 80)),
            Map.entry("Cr1", new Point(OFFSET_MAPA_X + 100, 220)),
            Map.entry("Cr2", new Point(OFFSET_MAPA_X + 400, 220)),
            Map.entry("Cr3", new Point(OFFSET_MAPA_X + 700, 220)),
            Map.entry("Cr4", new Point(OFFSET_MAPA_X + 100, 380)),
            Map.entry("Cr5", new Point(OFFSET_MAPA_X + 400, 380)),
            Map.entry("S", new Point(OFFSET_MAPA_X + 700, 380))
    );

    public PainelMapa() {
        setPreferredSize(new Dimension(LARGURA_PAINEL, ALTURA_PAINEL));
        setBackground(new Color(245, 245, 250));
        setBorder(BorderFactory.createTitledBorder("Mapa 2D da Simulação"));

        inicializarSemaforos();

        // Timer para animar veículos (60 FPS)
        new Timer(16, e -> {
            animarVeiculos();
            repaint();
        }).start();
    }

    /** Inicializa todos os semáforos */
    /*private void inicializarSemaforos() {
        semaforos.put("Cr1_E1", new SemaforoVisual("Cr1_E1",
                new Point(nos.get("Cr1").x - 17, nos.get("Cr1").y - 35)));
        semaforos.put("Cr1_Cr2", new SemaforoVisual("Cr1_Cr2",
                new Point(nos.get("Cr1").x + 20, nos.get("Cr1").y - 20)));

        semaforos.put("Cr2_E2", new SemaforoVisual("Cr2_E2",
                new Point(nos.get("Cr2").x - 17, nos.get("Cr2").y - 35)));
        semaforos.put("Cr2_Cr1", new SemaforoVisual("Cr2_Cr1",
                new Point(nos.get("Cr2").x - 33, nos.get("Cr2").y + 5)));
        semaforos.put("Cr2_Cr3", new SemaforoVisual("Cr2_Cr3",
                new Point(nos.get("Cr2").x + 20, nos.get("Cr2").y - 20)));

        semaforos.put("Cr3_E3", new SemaforoVisual("Cr3_E3",
                new Point(nos.get("Cr3").x - 17, nos.get("Cr3").y - 35)));
        semaforos.put("Cr3_Cr2", new SemaforoVisual("Cr3_Cr2",
                new Point(nos.get("Cr3").x - 30, nos.get("Cr3").y + 5)));

        semaforos.put("Cr4_Cr1", new SemaforoVisual("Cr4_Cr1",
                new Point(nos.get("Cr4").x - 17, nos.get("Cr4").y - 35)));

        semaforos.put("Cr5_Cr2", new SemaforoVisual("Cr5_Cr2",
                new Point(nos.get("Cr5").x - 17, nos.get("Cr5").y - 35)));
        semaforos.put("Cr5_Cr4", new SemaforoVisual("Cr5_Cr4",
                new Point(nos.get("Cr5").x - 33, nos.get("Cr5").y + 5)));
    }*/

    /**
     * Inicializa todos os semáforos com posições de desenho personalizadas,
     * mantendo os pontos de paragem centrados na linha.
     */
    private void inicializarSemaforos() {
        // Distância de paragem (antes do centro do nó) para os VEÍCULOS
        final int OFFSET_PARAGEM_VERTICAL = 30;   // Pára 30px acima do nó
        final int OFFSET_PARAGEM_HORIZONTAL = 30; // Pára 30px ao lado do nó

        // --- Pontos de referência dos nós ---
        Point pCr1 = nos.get("Cr1");
        Point pCr2 = nos.get("Cr2");
        Point pCr3 = nos.get("Cr3");
        Point pCr4 = nos.get("Cr4");
        Point pCr5 = nos.get("Cr5");

        // --- Definições dos Semáforos ---
        // A lógica é: new SemaforoVisual("Nome", PONTO_DE_PARAGEM, PONTO_DE_DESENHO)

        // --- Semáforos do Cr1 ---
        semaforos.put("Cr1_E1", new SemaforoVisual("Cr1_E1",
                new Point(pCr1.x, pCr1.y - OFFSET_PARAGEM_VERTICAL), // Paragem (na linha)
                new Point(pCr1.x - 17, pCr1.y - 35)                  // Desenho (as suas coords)
        ));
        semaforos.put("Cr1_Cr2", new SemaforoVisual("Cr1_Cr2",
                new Point(pCr1.x + OFFSET_PARAGEM_HORIZONTAL, pCr1.y), // Paragem (na linha)
                new Point(pCr1.x + 20, pCr1.y - 20)                    // Desenho (as suas coords)
        ));

        // --- Semáforos do Cr2 ---
        semaforos.put("Cr2_E2", new SemaforoVisual("Cr2_E2",
                new Point(pCr2.x, pCr2.y - OFFSET_PARAGEM_VERTICAL), // Paragem (na linha)
                new Point(pCr2.x - 17, pCr2.y - 35)                  // Desenho (as suas coords)
        ));
        semaforos.put("Cr2_Cr1", new SemaforoVisual("Cr2_Cr1",
                new Point(pCr2.x - OFFSET_PARAGEM_HORIZONTAL, pCr2.y), // Paragem (na linha)
                new Point(pCr2.x - 33, pCr2.y + 5)                     // Desenho (as suas coords)
        ));
        semaforos.put("Cr2_Cr3", new SemaforoVisual("Cr2_Cr3",
                new Point(pCr2.x + OFFSET_PARAGEM_HORIZONTAL, pCr2.y), // Paragem (na linha)
                new Point(pCr2.x + 20, pCr2.y - 20)                    // Desenho (as suas coords)
        ));

        // --- Semáforos do Cr3 ---
        semaforos.put("Cr3_E3", new SemaforoVisual("Cr3_E3",
                new Point(pCr3.x, pCr3.y - OFFSET_PARAGEM_VERTICAL), // Paragem (na linha)
                new Point(pCr3.x - 17, pCr3.y - 35)                  // Desenho (as suas coords)
        ));
        semaforos.put("Cr3_Cr2", new SemaforoVisual("Cr3_Cr2",
                new Point(pCr3.x - OFFSET_PARAGEM_HORIZONTAL, pCr3.y), // Paragem (na linha)
                new Point(pCr3.x - 30, pCr3.y + 5)                     // Desenho (as suas coords)
        ));

        // --- Semáforos do Cr4 ---
        semaforos.put("Cr4_Cr1", new SemaforoVisual("Cr4_Cr1",
                new Point(pCr4.x, pCr4.y - OFFSET_PARAGEM_VERTICAL), // Paragem (na linha)
                new Point(pCr4.x - 17, pCr4.y - 35)                  // Desenho (as suas coords)
        ));

        // --- Semáforos do Cr5 ---
        semaforos.put("Cr5_Cr2", new SemaforoVisual("Cr5_Cr2",
                new Point(pCr5.x, pCr5.y - OFFSET_PARAGEM_VERTICAL), // Paragem (na linha)
                new Point(pCr5.x - 17, pCr5.y - 35)                  // Desenho (as suas coords)
        ));
        semaforos.put("Cr5_Cr4", new SemaforoVisual("Cr5_Cr4",
                new Point(pCr5.x - OFFSET_PARAGEM_HORIZONTAL, pCr5.y), // Paragem (na linha)
                new Point(pCr5.x - 33, pCr5.y + 5)                     // Desenho (as suas coords)
        ));
    }

    /**
     * Adiciona veículo no mapa usando ID REAL extraído da mensagem.
     */
    public void adicionarVeiculo(String msg) {
        try {
            String[] partes = msg.split(" ");
            String entrada = partes.length > 1 ? partes[1] : "E3";

            String id = extrairValor(msg, "id=");
            if (id.isEmpty()) {
                id = entrada + "_" + System.currentTimeMillis() % 10000;
                System.out.printf("[Dashboard/Mapa] ⚠️ ID não encontrado, usando: %s%n", id);
            }

            if (veiculos.containsKey(id)) {
                System.out.printf("[Dashboard/Mapa] ⚠️ Veículo %s já existe (ignorando duplicata)%n", id);
                return;
            }

            List<String> percurso = gerarPercursoSimples(entrada);
            Color cor = corPorTipo(msg);
            boolean isMoto = msg.toLowerCase().contains("moto") || msg.toLowerCase().contains("mota");

            veiculos.put(id, new VeiculoAnimado(id, percurso, nos, semaforos, cor, isMoto));
            System.out.printf("[Dashboard/Mapa] ➕ Veículo %s adicionado | Total no mapa: %d%n",
                    id, veiculos.size());

        } catch (Exception e) {
            System.err.printf("[Dashboard/Mapa] ✗ Erro ao adicionar veículo: %s%n", e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Remove veículo quando chega à saída.
     */
    public void removerVeiculo(String msg) {
        try {
            String id = extrairValor(msg, "id=");
            if (!id.isEmpty()) {
                VeiculoAnimado removido = veiculos.remove(id);
                if (removido != null) {
                    System.out.printf("[Dashboard/Mapa] ➖ Veículo %s removido | Restantes: %d%n",
                            id, veiculos.size());
                } else {
                    System.out.printf("[Dashboard/Mapa] Tentativa de remover %s (não encontrado no mapa)%n", id);
                }
            } else {
                System.out.printf("[Dashboard/Mapa] ID não encontrado na mensagem: %s%n", msg);
            }
        } catch (Exception e) {
            System.err.printf("[Dashboard/Mapa] ✗ Erro ao remover veículo: %s%n", e.getMessage());
        }
    }

    /** Atualiza estado dos semáforos */
    public void atualizarSemaforo(String msg) {
        msg = msg.replace("[Semaforo]", "").trim();
        String[] partes = msg.split("=");
        if (partes.length == 2) {
            String nome = partes[0].trim();
            boolean estadoVerde = partes[1].trim().equalsIgnoreCase("VERDE");

            semaforos.values().forEach(s -> {
                if (s.getNome().equals(nome) ||
                        nome.replaceAll("_Sem_de_", "_").replaceAll("_para_.*", "").equals(s.getNome())) {
                    s.setCor(estadoVerde);
                }
            });
        }
    }

    /**
     * Move veículos e forma filas quando semáforo está vermelho.
     *
     */
    private void animarVeiculos() {
        Map<String, List<VeiculoAnimado>> grupos = new HashMap<>();

        for (VeiculoAnimado v : veiculos.values()) {
            v.limparAlvo();
            String atual = v.getAtual();
            String proximo = v.getProximo();
            if (atual == null || proximo == null) continue;
            grupos.computeIfAbsent(atual + "->" + proximo, k -> new ArrayList<>()).add(v);
        }

        // Organiza filas nos semáforos vermelhos
        for (Map.Entry<String, List<VeiculoAnimado>> e : grupos.entrySet()) {
            List<VeiculoAnimado> lista = e.getValue();
            if (lista.isEmpty()) continue;

            String atual = lista.get(0).getAtual();
            String semKey = atual.startsWith("E3") ? "Cr3_E3"
                    : atual.startsWith("E2") ? "Cr2_E2"
                    : atual.startsWith("E1") ? "Cr1_E1"
                    : null;
            if (semKey == null || !semaforos.containsKey(semKey)) continue;

            SemaforoVisual sem = semaforos.get(semKey);
            if (sem.isVerde()) continue;

            Point stop = sem.getPosParagem();
            Point pAtual = nos.get(atual);
            Point pProx = nos.get(lista.get(0).getProximo());
            if (pAtual == null || pProx == null) continue;

            double vx = stop.x - pAtual.x;
            double vy = stop.y - pAtual.y;
            double n = Math.hypot(vx, vy);
            if (n == 0) continue;
            vx /= n; vy /= n;

            lista.sort(Comparator.comparingDouble(v ->
                    Math.hypot(stop.x - v.x, stop.y - v.y)));

            final int espacoUniforme = 18;
            final int offsetLinha = 15;

            int idx = 0;
            for (VeiculoAnimado v : lista) {
                double tx = stop.x - vx * (offsetLinha + idx * espacoUniforme);
                double ty = stop.y - vy * (offsetLinha + idx * espacoUniforme);

                v.setAlvoTemporario(new Point((int) tx, (int) ty));
                idx++;
            }
        }

        // Move todos os veículos
        Iterator<Map.Entry<String, VeiculoAnimado>> it = veiculos.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, VeiculoAnimado> entry = it.next();
            VeiculoAnimado v = entry.getValue();
            v.mover();

            if (v.chegouAoDestino()) {
                System.out.printf("[Dashboard/Mapa] 🏁 Veículo %s chegou ao destino (remoção automática)%n",
                        entry.getKey());
                it.remove();
            }
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharLegenda(g2);
        desenharMapa(g2);
        desenharSemaforos(g2);
        desenharVeiculos(g2);
        desenharContador(g2);
    }

    /**
     * ✅ NOVO: Desenha legenda lateral com cores dos cruzamentos
     */
    private void desenharLegenda(Graphics2D g2) {
        int x = 10;
        int y = 40;
        int tamanhoForma = 20; // Alterado de tamanhoCirculo para tamanhoForma
        int espacamento = 35;

        g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g2.setColor(Color.BLACK);
        g2.drawString("Legenda", x, y - 10);

        g2.setStroke(new BasicStroke(1.5f));

        // Cruzamentos com cores (agora como quadrados)
        for (Map.Entry<String, Color> entry : CORES_CRUZAMENTOS.entrySet()) {
            String nome = entry.getKey();
            Color cor = entry.getValue();

            g2.setColor(cor);
            g2.fillRect(x, y, tamanhoForma, tamanhoForma); // Alterado para fillRect
            g2.setColor(cor.darker());
            g2.setStroke(new BasicStroke(2f));
            g2.drawRect(x, y, tamanhoForma, tamanhoForma); // Alterado para drawRect

            g2.setColor(Color.BLACK);
            g2.drawString(nome, x + tamanhoForma + 8, y + 15);
            y += espacamento;
        }

        // Linha separadora
        g2.setColor(new Color(200, 200, 200));
        g2.setStroke(new BasicStroke(1f));
        g2.drawLine(LARGURA_LEGENDA, 0, LARGURA_LEGENDA, getHeight());
    }

    /**
     * Desenha mapa com linhas mais grossas
     */
    private void desenharMapa(Graphics2D g2) {
        g2.setStroke(new BasicStroke(3f)); // ✅ Linhas mais grossas
        g2.setColor(new Color(200, 200, 200));

        int x1 = OFFSET_MAPA_X + 100;
        int x2 = OFFSET_MAPA_X + 400;
        int x3 = OFFSET_MAPA_X + 700;
        int y1 = 80;
        int y2 = 220;
        int y3 = 380;

        // Linhas verticais
        g2.drawLine(x1, y1, x1, y3);
        g2.drawLine(x2, y1, x2, y3);
        g2.drawLine(x3, y1, x3, y3);

        // Linhas horizontais
        g2.drawLine(x1, y2, x3, y2);
        g2.drawLine(x1, y3, x3, y3);

        // Desenha nós
        for (String nome : nos.keySet()) {
            desenharNo(g2, nome);
        }
    }

    /**
     * Desenha nós:
     *
     *
     */
    private void desenharNo(Graphics2D g2, String nome) {
        Point p = nos.get(nome);
        int tamanho = 30;
        int offset = tamanho / 2;

        Color cor;

        if (nome.startsWith("E") || nome.equals("S")) {
            // Entradas e Saída são CÍRCULOS com LABEL
            cor = nome.equals("S") ? new Color(139, 0, 0) : Color.GRAY;

            // Desenha círculo
            g2.setColor(cor);
            g2.fillOval(p.x - offset, p.y - offset, tamanho, tamanho);
            g2.setColor(cor.darker());
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawOval(p.x - offset, p.y - offset, tamanho, tamanho);

            // Adiciona label (E1, E2, E3, S)
            g2.setColor(Color.BLACK);
            g2.setFont(new Font("Segoe UI", Font.BOLD, 12));

            // Centra o texto horizontalmente e posiciona acima do nó
            FontMetrics fm = g2.getFontMetrics();
            int stringWidth = fm.stringWidth(nome);
            g2.drawString(nome, p.x - stringWidth / 2, p.y - offset - 5);

        } else {
            // Cruzamentos (Cr) são QUADRADOS sem LABEL
            cor = CORES_CRUZAMENTOS.getOrDefault(nome, Color.DARK_GRAY);

            // Desenha quadrado
            g2.setColor(cor);
            g2.fillRect(p.x - offset, p.y - offset, tamanho, tamanho);
            g2.setColor(cor.darker());
            g2.setStroke(new BasicStroke(2.5f));
            g2.drawRect(p.x - offset, p.y - offset, tamanho, tamanho);
        }
    }

    private void desenharSemaforos(Graphics2D g2) {
        for (SemaforoVisual s : semaforos.values()) {
            s.desenhar(g2);
        }
    }

    private void desenharVeiculos(Graphics2D g2) {
        for (VeiculoAnimado v : veiculos.values()) {
            g2.setColor(v.cor);
            Point p = v.posicaoAtual();

            if (v.isMoto()) {
                // Triângulo para motas
                int s = 10;
                int[] xs = { p.x, p.x - s/2, p.x + s/2 };
                int[] ys = { p.y - s/2, p.y + s/2, p.y + s/2 };
                g2.fillPolygon(xs, ys, 3);
            } else {
                // Círculo para carros/camiões
                g2.fillOval(p.x - 6, p.y - 6, 12, 12);
            }
        }
    }

    private void desenharContador(Graphics2D g2) {
        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        g2.drawString("🚗 Veículos no mapa: " + veiculos.size(),
                OFFSET_MAPA_X, 20);
    }

    private List<String> gerarPercursoSimples(String entrada) {
        switch (entrada) {
            case "E1": return Arrays.asList("E1", "Cr1", "Cr4", "Cr5", "S");
            case "E2": return Arrays.asList("E2", "Cr2", "Cr5", "S");
            case "E3": return Arrays.asList("E3", "Cr3", "S");
            default: return List.of("E3", "Cr3", "S");
        }
    }

    private Color corPorTipo(String msg) {
        String msgLower = msg.toLowerCase();
        if (msgLower.contains("moto") || msgLower.contains("mota")) return Color.BLUE;
        if (msgLower.contains("carro")) return new Color(0, 128, 0);
        if (msgLower.contains("camiao") || msgLower.contains("caminhao")) return new Color(255, 140, 0);
        return Color.DARK_GRAY;
    }

    private String extrairValor(String msg, String chave) {
        try {
            int i = msg.indexOf(chave);
            if (i == -1) return "";
            int start = i + chave.length();
            int end = msg.indexOf(' ', start);
            if (end == -1) end = msg.length();
            return msg.substring(start, end).trim().replaceAll("[\\[\\],:]", "");
        } catch (Exception e) {
            return "";
        }
    }

    // ====== CLASSES INTERNAS ======

    private static class SemaforoVisual {
        private final String nome;
        private final Point posParagem;
        private final Point posDesenho;
        private boolean verde = false;

        /**
         * Construtor atualizado para ter duas posições.
         * @param nome O ID do semáforo (ex: "Cr1_E1")
         * @param posParagem Onde os veículos devem parar (na linha)
         * @param posDesenho Onde o semáforo é desenhado (ao lado da linha)
         */
        public SemaforoVisual(String nome, Point posParagem, Point posDesenho) {
            this.nome = nome;
            this.posParagem = posParagem;
            this.posDesenho = posDesenho;
        }

        public String getNome() { return nome; }
        public Point getPosParagem() { return posParagem; } // ✅ Renomeado de getPos()
        public boolean isVerde() { return verde; }
        public void setCor(boolean verde) { this.verde = verde; }

        public void desenhar(Graphics g) {
            g.setColor(verde ? Color.GREEN : Color.RED);

            // ✅ Usa posDesenho para desenhar
            g.fillOval(posDesenho.x, posDesenho.y, 12, 12);
            g.setColor(Color.BLACK);
            g.drawOval(posDesenho.x, posDesenho.y, 12, 12);
        }
    }

    private static class VeiculoAnimado {
        private final String id;
        private final List<String> percurso;
        private final Map<String, Point> nos;
        private final Map<String, SemaforoVisual> semaforos;
        private int indiceAtual = 0;
        private double x, y;
        private final double velocidade = 2.0;
        public final Color cor;
        private Point alvoTemporario = null;
        private final boolean isMoto;

        public VeiculoAnimado(String id, List<String> percurso,
                              Map<String, Point> nos, Map<String, SemaforoVisual> semaforos,
                              Color cor, boolean isMoto) {
            this.id = id;
            this.percurso = percurso;
            this.nos = nos;
            this.semaforos = semaforos;
            this.cor = cor;
            this.isMoto = isMoto;
            Point p0 = nos.get(percurso.get(0));
            this.x = p0.x;
            this.y = p0.y;
        }

        public String getId() { return id; }
        public boolean isMoto() { return isMoto; }

        public String getAtual() {
            return indiceAtual < percurso.size() ? percurso.get(indiceAtual) : percurso.get(percurso.size()-1);
        }

        public String getProximo() {
            return indiceAtual+1 < percurso.size() ? percurso.get(indiceAtual+1) : percurso.get(percurso.size()-1);
        }

        public void setAlvoTemporario(Point p) { this.alvoTemporario = p; }
        public void limparAlvo() { this.alvoTemporario = null; }

        public void mover() {
            if (alvoTemporario != null) {
                double dx = alvoTemporario.x - x;
                double dy = alvoTemporario.y - y;
                double dist = Math.hypot(dx, dy);
                if (dist <= velocidade) {
                    x = alvoTemporario.x;
                    y = alvoTemporario.y;
                } else {
                    x += (dx/dist)*velocidade;
                    y += (dy/dist)*velocidade;
                }
                return;
            }

            if (indiceAtual >= percurso.size() - 1) return;

            String atual = percurso.get(indiceAtual);
            String proximo = percurso.get(indiceAtual + 1);

            String semaforoKey = atual.startsWith("E3") ? "Cr3_E3"
                    : atual.startsWith("E2") ? "Cr2_E2"
                    : atual.startsWith("E1") ? "Cr1_E1"
                    : null;

            if (semaforoKey != null && semaforos.containsKey(semaforoKey)) {
                SemaforoVisual sem = semaforos.get(semaforoKey);

                Point pontoDeParagem = sem.getPosParagem();
                double distAteParagem = Math.hypot(pontoDeParagem.x - x, pontoDeParagem.y - y);

                if (!sem.isVerde() && distAteParagem < velocidade) {
                    return;
                }
            }

            Point destino = nos.get(proximo);
            double dx = destino.x - x;
            double dy = destino.y - y;
            double dist = Math.sqrt(dx*dx + dy*dy);

            if (dist < velocidade) {
                indiceAtual++;
                x = destino.x;
                y = destino.y;
            } else {
                x += (dx/dist)*velocidade;
                y += (dy/dist)*velocidade;
            }
        }

        public boolean chegouAoDestino() {
            return indiceAtual >= percurso.size() - 1;
        }

        public Point posicaoAtual() {
            return new Point((int) x, (int) y);
        }
    }
}