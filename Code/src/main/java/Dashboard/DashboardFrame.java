package Dashboard;

import javax.swing.*;
import java.awt.*;

/**
 * Janela principal do Dashboard.
 * Contém:
 *  - Estatísticas globais (topo)
 *  - Mapa do sistema (centro)
 *  - Lista de veículos que saíram (base)
 */
public class DashboardFrame extends JFrame {

    private PainelEstatisticas painelEstatisticas;
    private PainelMapa painelMapa;
    private PainelVeiculos painelVeiculos;

    public DashboardFrame() {
        super("Dashboard - Sistema de Tráfego Urbano");

        // === Configuração da janela ===
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(900, 700);
        setLayout(new BorderLayout(10, 10));

        // === Criação dos painéis ===
        painelEstatisticas = new PainelEstatisticas();
        painelMapa = new PainelMapa();
        painelVeiculos = new PainelVeiculos();

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
