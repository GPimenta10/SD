package Dashboard.Paineis;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.UIManager;

import Dashboard.Desenhar.DesenharLegenda;
import Dashboard.Desenhar.DesenharNos;
import Dashboard.Desenhar.DesenharSemaforos;
import Dashboard.Desenhar.DesenharVeiculos;
import Dashboard.Desenhar.DesenharVias;
import Dashboard.Desenhar.GestorPosicoes;
import Dashboard.Desenhar.VeiculoNoMapa;
import Dashboard.Utils.DashboardUIUtils;

/**
 * Painel que exibe o mapa do sistema de tráfego com animação de veículos.
 * Versão otimizada para suportar carga alta sem perder veículos.
 */
public class PainelMapa extends JPanel {

    private static final int INTERVALO_ANIMACAO_MS = 16; // ~60 FPS
    private static final int LARGURA_PREFERIDA = 400;
    private static final int ALTURA_PREFERIDA = 480;
    
    // Delay para processar novos veículos (evita sobrecarga)
    private static final int DELAY_PROCESSAR_FILA_MS = 50;

    // Componentes de desenho
    private final GestorPosicoes gestorPosicoes = new GestorPosicoes();
    private final DesenharVias desenharVias = new DesenharVias();
    private final DesenharNos desenharNos = new DesenharNos();
    private final DesenharSemaforos desenharSemaforos = new DesenharSemaforos();
    private final DesenharVeiculos desenharVeiculos = new DesenharVeiculos();

    // Estado dos veículos e semáforos
    private final Map<Integer, String> mapaIds = new ConcurrentHashMap<>();
    private final List<VeiculoNoMapa> veiculosEmTransito = new CopyOnWriteArrayList<>();
    private final Map<String, VeiculoNoMapa> veiculosPorId = new ConcurrentHashMap<>();
    private final Map<String, Boolean> estadosSemaforos = new ConcurrentHashMap<>();
    
    // Fila de eventos pendentes (para não perder movimentos)
    private final Queue<EventoVeiculo> filaPendentes = new ConcurrentLinkedQueue<>();

    private Timer animationTimer;
    private Timer processadorFilaTimer;
    private long ultimoProcessamento = 0;

    // Classe interna para eventos de veículos
    private record EventoVeiculo(String id, String tipo, String origem, String destino, long timestamp) {}

    public PainelMapa() {
        setPreferredSize(new Dimension(LARGURA_PREFERIDA, ALTURA_PREFERIDA));
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Mapa do Sistema de Tráfego",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        iniciarAnimacao();
        iniciarProcessadorFila();
    }

    private void iniciarAnimacao() {
        animationTimer = new Timer(INTERVALO_ANIMACAO_MS, e -> {
            atualizarVeiculos();
            repaint();
        });
        animationTimer.start();
    }
    
    private void iniciarProcessadorFila() {
        processadorFilaTimer = new Timer(DELAY_PROCESSAR_FILA_MS, e -> processarFilaPendentes());
        processadorFilaTimer.start();
    }
    
    private void processarFilaPendentes() {
        int processados = 0;
        int maxPorCiclo = 5; // Limitar para não sobrecarregar
        
        while (!filaPendentes.isEmpty() && processados < maxPorCiclo) {
            EventoVeiculo ev = filaPendentes.poll();
            if (ev != null) {
                processarEventoVeiculo(ev);
                processados++;
            }
        }
    }
    
    private void processarEventoVeiculo(EventoVeiculo ev) {
        if (!veiculosPorId.containsKey(ev.id)) {
            criarVeiculoInterno(ev.id, ev.tipo, ev.origem, ev.destino);
        } else {
            atualizarDestinoInterno(ev.id, ev.origem, ev.destino);
        }
    }

    private void atualizarVeiculos() {
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

        // Remover veículos que terminaram (com verificação de tempo mínimo)
        veiculosEmTransito.removeIf(v -> {
            if (v.terminouTodosSegmentos()) {
                veiculosPorId.remove(v.getId());
                return true;
            }
            return false;
        });
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
    }

    public void atualizarOuCriarVeiculo(String id, String tipo, String origem, String destino) {
        // Adicionar à fila para processamento controlado
        filaPendentes.offer(new EventoVeiculo(id, tipo, origem, destino, System.currentTimeMillis()));
    }

    private void criarVeiculoInterno(String id, String tipo, String origem, String destino) {
        Point2D posOrigem = gestorPosicoes.getPosicoes().get(origem);
        Point2D posDestino = gestorPosicoes.getPosicoes().get(destino);

        if (posOrigem == null || posDestino == null) return;

        Point2D[] ajust = gestorPosicoes.calcularPosicoesAjustadas(origem, destino);
        String chaveSem = destino + "_" + origem + "-" + destino;

        VeiculoNoMapa v = new VeiculoNoMapa(
                id, tipo,
                posOrigem, posDestino,
                gestorPosicoes.getPosicoesSemaforos().get(chaveSem),
                chaveSem,
                ajust[0], ajust[1]
        );

        veiculosPorId.put(id, v);
        veiculosEmTransito.add(v);
    }

    private void atualizarDestinoInterno(String id, String origem, String destino) {
        VeiculoNoMapa v = veiculosPorId.get(id);
        if (v == null) return;

        Point2D posOrigem = gestorPosicoes.getPosicoes().get(origem);
        Point2D posDestino = gestorPosicoes.getPosicoes().get(destino);
        if (posOrigem == null || posDestino == null) return;

        Point2D[] ajust = gestorPosicoes.calcularPosicoesAjustadas(origem, destino);
        String chaveSem = destino + "_" + origem + "-" + destino;

        v.adicionarProximoSegmento(
                origem, destino, posOrigem, posDestino,
                chaveSem, gestorPosicoes.getPosicoesSemaforos().get(chaveSem),
                ajust[0], ajust[1]
        );
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        desenharVias.desenharTodas(g2, gestorPosicoes);
        desenharNos.desenharTodos(g2, gestorPosicoes);
        desenharSemaforos.desenharTodos(g2, gestorPosicoes, estadosSemaforos);
        desenharVeiculos.desenharTodos(g2, veiculosEmTransito);
        DesenharLegenda.desenharLegenda(g2, this);
        
        // Mostrar contador de veículos ativos (debug visual)
        g2.setColor(UIManager.getColor("Label.foreground"));
        g2.setFont(DashboardUIUtils.FONTE_CONSOLE.deriveFont(11f));
        g2.drawString("Veículos no mapa: " + veiculosEmTransito.size(), 10, 25);
    }
}