/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;
import Dashboard.DashboardFrame;
import Dashboard.Desenhar.*;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.awt.geom.Point2D;
import javax.swing.Timer;
import java.util.List;
import javax.swing.*;
import java.util.*;
import java.awt.*;

/**
 * 
 * 
 * 
 */
public class PainelMapa extends JPanel {

    private final GestorPosicoes gestorPosicoes = new GestorPosicoes();
    private final DesenharVias desenharVias = new DesenharVias();
    private final DesenharNos desenharNos = new DesenharNos();
    private final DesenharSemaforos desenharSemaforos = new DesenharSemaforos();
    private final DesenharVeiculos desenharVeiculos = new DesenharVeiculos();

    private final Map<Integer, String> mapaIds = new HashMap<>();
    private final List<VeiculoNoMapa> veiculosEmTransito = new CopyOnWriteArrayList<>();
    private final Map<String, VeiculoNoMapa> veiculosPorId = new ConcurrentHashMap<>();
    private final Map<String, Boolean> estadosSemaforos = new ConcurrentHashMap<>();

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

    private void atualizarDestino(String id, String origem, String destino) {
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

        // Desenho delegado:
        desenharVias.desenharTodas(g2, gestorPosicoes);
        desenharNos.desenharTodos(g2, gestorPosicoes);
        desenharSemaforos.desenharTodos(g2, gestorPosicoes, estadosSemaforos);
        desenharVeiculos.desenharTodos(g2, veiculosEmTransito);
    }
}

