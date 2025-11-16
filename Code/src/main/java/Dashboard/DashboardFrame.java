package Dashboard;

import javax.swing.*;
import java.awt.*;

/**
 * Janela principal do Dashboard.
 * Layout:
 *  - Coluna esquerda: Estatísticas globais + espaço vazio
 *  - Centro: Mapa do sistema
 *  - Coluna direita: Estatísticas por tipo (topo) + Lista de veículos que saíram (baixo)
 *  - Base: Logs do sistema
 */
public class DashboardFrame extends JFrame {

    private PainelEstatisticasGlobais painelEstatisticasGlobais;
    private PainelMapa painelMapa;
    private PainelEstatisticasSaida painelEstatisticasSaida;
    private PainelInfoSaidaVeiculos painelInfoSaidaVeiculos;
    private PainelLogs painelLogs;

    public DashboardFrame() {
        super("Dashboard - Sistema de Tráfego Urbano");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(10, 10));

        // === Criação dos painéis ===
        painelEstatisticasGlobais = new PainelEstatisticasGlobais();
        painelMapa = new PainelMapa();
        painelEstatisticasSaida = new PainelEstatisticasSaida();
        painelInfoSaidaVeiculos = new PainelInfoSaidaVeiculos();
        painelLogs = new PainelLogs();

        painelMapa.setDashboard(this);
        DashLogger.inicializar(painelLogs);

        // === Caixa vazia abaixo das estatísticas globais ===
        JPanel caixaVazia = new JPanel();
        caixaVazia.setPreferredSize(new Dimension(225, 150));
        caixaVazia.setBackground(new Color(40, 42, 54));
        caixaVazia.setBorder(BorderFactory.createTitledBorder(" "));

        // === Coluna esquerda: Estatísticas globais + caixa vazia ===
        JPanel colunaEsquerda = new JPanel(new BorderLayout());
        colunaEsquerda.setPreferredSize(new Dimension(250, 0));
        colunaEsquerda.add(painelEstatisticasGlobais, BorderLayout.NORTH);
        colunaEsquerda.add(caixaVazia, BorderLayout.CENTER);

        // === Coluna direita: Estatísticas por tipo + Lista de veículos ===
        JPanel colunaDireita = new JPanel(new BorderLayout(5, 5));
        colunaDireita.setPreferredSize(new Dimension(500, 0));
        colunaDireita.setBackground(new Color(40, 42, 54));

        // Ajusta tamanhos preferenciais
        painelEstatisticasSaida.setPreferredSize(new Dimension(0, 280));
        painelInfoSaidaVeiculos.setPreferredSize(new Dimension(0, 300));

        colunaDireita.add(painelEstatisticasSaida, BorderLayout.NORTH);
        colunaDireita.add(painelInfoSaidaVeiculos, BorderLayout.CENTER);

        // === Configura dimensões dos outros painéis ===
        painelMapa.setPreferredSize(new Dimension(700, 0));
        painelLogs.setPreferredSize(new Dimension(0, 250));

        // === Adiciona os painéis ao frame ===
        add(colunaEsquerda, BorderLayout.WEST);
        add(painelMapa, BorderLayout.CENTER);
        add(colunaDireita, BorderLayout.EAST);
        add(painelLogs, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public PainelEstatisticasGlobais getPainelEstatisticas() {
        return painelEstatisticasGlobais;
    }

    public PainelMapa getPainelMapa() {
        return painelMapa;
    }

    public PainelEstatisticasSaida getPainelEstatisticasTipo() {
        return painelEstatisticasSaida;
    }

    public PainelInfoSaidaVeiculos getPainelVeiculos() {
        return painelInfoSaidaVeiculos;
    }

    public PainelLogs getPainelLogs() {
        return painelLogs;
    }
}