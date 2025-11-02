package Dashboard;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Painel gráfico com mapa, veículos e semáforos animados e sincronizados com o estado real.
 */
public class PainelMapa extends JPanel {

    private final ConcurrentHashMap<String, VeiculoAnimado> veiculos = new ConcurrentHashMap<>();
    private final Map<String, SemaforoVisual> semaforos = new HashMap<>();

    // Posições fixas dos nós
    private final Map<String, Point> nos = Map.ofEntries(
            Map.entry("E1", new Point(150, 60)),
            Map.entry("E2", new Point(400, 60)),
            Map.entry("E3", new Point(650, 60)),
            Map.entry("Cr1", new Point(150, 180)),
            Map.entry("Cr2", new Point(400, 180)),
            Map.entry("Cr3", new Point(650, 180)),
            Map.entry("Cr4", new Point(150, 300)),
            Map.entry("Cr5", new Point(400, 300)),
            Map.entry("S", new Point(650, 300))
    );

    public PainelMapa() {
        setPreferredSize(new Dimension(950, 450));
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createTitledBorder("Mapa 2D da Simulação"));

        inicializarSemaforos();

        // Timer para animar veículos (60 FPS)
        new Timer(60, e -> animarVeiculos()).start();
    }

    /** Inicializa todos os semáforos (nas posições corretas) */
    private void inicializarSemaforos() {
        semaforos.put("Cr1_E1", new SemaforoVisual("Cr1_E1",
                new Point(nos.get("Cr1").x - 10, nos.get("Cr1").y - 35)));
        semaforos.put("Cr1_Cr2", new SemaforoVisual("Cr1_Cr2",
                new Point(nos.get("Cr1").x + 10, nos.get("Cr1").y - 10)));

        semaforos.put("Cr2_E2", new SemaforoVisual("Cr2_E2",
                new Point(nos.get("Cr2").x - 10, nos.get("Cr2").y - 35)));
        semaforos.put("Cr2_Cr1", new SemaforoVisual("Cr2_Cr1",
                new Point(nos.get("Cr2").x - 20, nos.get("Cr2").y)));
        semaforos.put("Cr2_Cr3", new SemaforoVisual("Cr2_Cr3",
                new Point(nos.get("Cr2").x + 10, nos.get("Cr2").y - 10)));

        semaforos.put("Cr3_E3", new SemaforoVisual("Cr3_E3",
                new Point(nos.get("Cr3").x - 10, nos.get("Cr3").y - 35)));
        semaforos.put("Cr3_Cr2", new SemaforoVisual("Cr3_Cr2",
                new Point(nos.get("Cr3").x - 20, nos.get("Cr3").y)));

        semaforos.put("Cr4_Cr1", new SemaforoVisual("Cr4_Cr1",
                new Point(nos.get("Cr4").x - 10, nos.get("Cr4").y - 35)));

        semaforos.put("Cr5_Cr2", new SemaforoVisual("Cr5_Cr2",
                new Point(nos.get("Cr5").x - 10, nos.get("Cr5").y - 35)));
        semaforos.put("Cr5_Cr4", new SemaforoVisual("Cr5_Cr4",
                new Point(nos.get("Cr5").x - 20, nos.get("Cr5").y)));
    }

    /** Adiciona veículo no mapa */
    public void adicionarVeiculo(String msg) {
        String entrada = msg.split(" ")[1];
        String id = UUID.randomUUID().toString().substring(0, 8);
        List<String> percurso = gerarPercursoSimples(entrada);
        Color cor = corPorTipo(msg);
        veiculos.put(id, new VeiculoAnimado(id, percurso, nos, semaforos, cor));
        repaint();
    }

    /** Remove veículo (ao sair) */
    public void removerVeiculo(String msg) {
        veiculos.clear(); // simplificado
        repaint();
    }

    /** Atualiza estado dos semáforos conforme mensagens reais */
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
            repaint();
        }
    }

    /** Move veículos */
    private void animarVeiculos() {
        for (VeiculoAnimado v : veiculos.values()) {
            v.mover();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        desenharMapa(g);
        desenharSemaforos(g);
        desenharVeiculos(g);
    }

    private void desenharMapa(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));
        g.setFont(new Font("Segoe UI", Font.BOLD, 14));
        g.setColor(Color.LIGHT_GRAY);

        // Estradas verticais
        g.drawLine(150, 60, 150, 300);
        g.drawLine(400, 60, 400, 300);
        g.drawLine(650, 60, 650, 300);

        // Estradas horizontais
        g.drawLine(150, 180, 650, 180);
        g.drawLine(150, 300, 650, 300);

        // Nós
        for (String nome : nos.keySet()) {
            desenharNo(g, nome);
        }
    }

    private void desenharNo(Graphics g, String nome) {
        Point p = nos.get(nome);
        g.setColor(Color.GRAY);
        g.fillOval(p.x - 10, p.y - 10, 20, 20);
        g.setColor(Color.BLACK);
        g.drawString(nome, p.x - 15, p.y - 15);
    }

    private void desenharSemaforos(Graphics g) {
        for (SemaforoVisual s : semaforos.values()) {
            s.desenhar(g);
        }
    }

    private void desenharVeiculos(Graphics g) {
        for (VeiculoAnimado v : veiculos.values()) {
            g.setColor(v.cor);
            Point p = v.posicaoAtual();
            g.fillOval(p.x - 6, p.y - 6, 12, 12);
        }
    }

    private List<String> gerarPercursoSimples(String entrada) {
        switch (entrada) {
            case "E1": return Arrays.asList("E1", "Cr1", "Cr4", "S");
            case "E2": return Arrays.asList("E2", "Cr2", "Cr5", "S");
            case "E3": return Arrays.asList("E3", "Cr3", "S");
            default: return List.of("E3", "Cr3", "S");
        }
    }

    private Color corPorTipo(String msg) {
        if (msg.toLowerCase().contains("moto")) return Color.BLUE;
        if (msg.toLowerCase().contains("carro")) return Color.GREEN.darker();
        if (msg.toLowerCase().contains("caminhao")) return Color.YELLOW.darker();
        return Color.DARK_GRAY;
    }

    // ====== CLASSES INTERNAS ======

    /** Representa um semáforo gráfico (com estado real) */
    private static class SemaforoVisual {
        private final String nome;
        private final Point pos;
        private boolean verde = false;

        public SemaforoVisual(String nome, Point pos) {
            this.nome = nome;
            this.pos = pos;
        }

        public String getNome() { return nome; }
        public boolean isVerde() { return verde; }
        public void setCor(boolean verde) { this.verde = verde; }

        public void desenhar(Graphics g) {
            g.setColor(verde ? Color.GREEN : Color.RED);
            g.fillOval(pos.x, pos.y, 10, 10);
        }
    }

    /** Representa um veículo em movimento (parando nos semáforos vermelhos) */
    private static class VeiculoAnimado {
        private final List<String> percurso;
        private final Map<String, Point> nos;
        private final Map<String, SemaforoVisual> semaforos;
        private int indiceAtual = 0;
        private double x, y;
        private final double velocidade = 2.0;
        public final Color cor;

        public VeiculoAnimado(String id, List<String> percurso,
                              Map<String, Point> nos, Map<String, SemaforoVisual> semaforos, Color cor) {
            this.percurso = percurso;
            this.nos = nos;
            this.semaforos = semaforos;
            this.cor = cor;
            Point p0 = nos.get(percurso.get(0));
            this.x = p0.x;
            this.y = p0.y;
        }

        public void mover() {
            if (indiceAtual >= percurso.size() - 1) return;

            String atual = percurso.get(indiceAtual);
            String proximo = percurso.get(indiceAtual + 1);

            // se existe um semáforo no destino e está vermelho → parar
            String semaforoKey = atual.startsWith("E3") ? "Cr3_E3"
                    : atual.startsWith("E2") ? "Cr2_E2"
                    : atual.startsWith("E1") ? "Cr1_E1"
                    : null;

            if (semaforoKey != null && semaforos.containsKey(semaforoKey)) {
                SemaforoVisual sem = semaforos.get(semaforoKey);
                Point destino = nos.get(proximo);

                // calcula distância até o semáforo (linha de travagem)
                double distAteDestino = Math.hypot(destino.x - x, destino.y - y);

                // se o semáforo está vermelho E o carro ainda não atingiu o cruzamento → parar
                if (!sem.isVerde() && distAteDestino > 25) {
                    return;
                }
            }

            Point origem = nos.get(atual);
            Point destino = nos.get(proximo);

            double dx = destino.x - x;
            double dy = destino.y - y;
            double dist = Math.sqrt(dx * dx + dy * dy);

            if (dist < velocidade) {
                indiceAtual++;
            } else {
                x += (dx / dist) * velocidade;
                y += (dy / dist) * velocidade;
            }
        }

        public Point posicaoAtual() { return new Point((int) x, (int) y); }
    }
}
