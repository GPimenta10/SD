package Dashboard;

import javax.swing.*;
import java.awt.*;

/**
 * Janela principal do Dashboard.
 * Contém:
 *  - Estatísticas globais (topo) - 10%
 *  - Mapa do sistema (centro) - 70%
 *  - Lista de veículos que saíram (base) - 20%
 *
 * 🔧 CORREÇÃO: Proporções ajustadas para dar mais destaque ao mapa
 */
public class DashboardFrame extends JFrame {

    private PainelEstatisticas painelEstatisticas;
    private PainelMapa painelMapa;
    private PainelVeiculos painelVeiculos;

    public DashboardFrame() {
        super("Dashboard - Sistema de Tráfego Urbano");

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 800);
        setLayout(new BorderLayout(10, 10));

        // === Criação dos painéis já existentes ===
        painelEstatisticas = new PainelEstatisticas();
        painelMapa = new PainelMapa();
        painelVeiculos = new PainelVeiculos();

        // === Novo: Painel de LOGS (simples por agora) ===
        JPanel painelLogs = new JPanel();
        painelLogs.setPreferredSize(new Dimension(0, 150));  // altura do bloco
        painelLogs.setBackground(new Color(230, 230, 230));   // cinza claro
        painelLogs.setBorder(
                BorderFactory.createTitledBorder("Eventos Importantes")
        );

        // === NOVO: Caixa vazia abaixo das estatísticas ===
        JPanel caixaVazia = new JPanel();
        caixaVazia.setPreferredSize(new Dimension(250, 150));
        caixaVazia.setBackground(new Color(240, 240, 240));
        caixaVazia.setBorder(
                BorderFactory.createTitledBorder(" ")
        );

        // === Wrapper da coluna esquerda (estatísticas + caixa vazia) ===
        JPanel colunaEsquerda = new JPanel(new BorderLayout());
        colunaEsquerda.setPreferredSize(new Dimension(250, 0));

        colunaEsquerda.add(painelEstatisticas, BorderLayout.NORTH);
        colunaEsquerda.add(caixaVazia, BorderLayout.CENTER);

        // === Tamanhos dos 3 painéis principais ===
        painelMapa.setPreferredSize(new Dimension(350, 0));
        painelVeiculos.setPreferredSize(new Dimension(550, 0));

        // === Adiciona os painéis nos mesmos sítios ===
        add(colunaEsquerda, BorderLayout.WEST);  // substitui painelEstatisticas direto
        add(painelMapa, BorderLayout.CENTER);
        add(painelVeiculos, BorderLayout.EAST);

        // === Adiciona o painel de logs por baixo ===
        add(painelLogs, BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    public PainelEstatisticas getPainelEstatisticas() {
        return painelEstatisticas;
    }

    public PainelMapa getPainelMapa() {
        return painelMapa;
    }

    public PainelVeiculos getPainelVeiculos() {
        return painelVeiculos;
    }
}