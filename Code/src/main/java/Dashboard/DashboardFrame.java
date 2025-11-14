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

        // === Configuração da janela ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 800);
        setLayout(new BorderLayout(10, 10));

        // === Criação dos painéis ===
        painelEstatisticas = new PainelEstatisticas();
        painelMapa = new PainelMapa();
        painelVeiculos = new PainelVeiculos();

        // 🔧 NOVO: Configurar tamanhos preferenciais
        painelEstatisticas.setPreferredSize(new Dimension(1000, 80));
        painelMapa.setPreferredSize(new Dimension(1000, 500));
        painelVeiculos.setPreferredSize(new Dimension(1000, 100));

        // === Adição à janela ===
        add(painelEstatisticas, BorderLayout.NORTH);
        add(painelMapa, BorderLayout.CENTER);
        add(painelVeiculos, BorderLayout.SOUTH);

        setLocationRelativeTo(null); // centrar
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