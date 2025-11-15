package Dashboard;

import javax.swing.*;
import java.awt.*;

/**
 * Painel superior com estatísticas globais:
 * - Total de veículos gerados
 * - Total por entrada (E1, E2, E3)
 * - Total de veículos que saíram
 *
 * 🔧 MELHORIAS: Visual mais claro com cores e fonte maior
 */
public class PainelEstatisticas extends JPanel {

    private JLabel totalGeradoLabel;
    private JLabel e1Label;
    private JLabel e2Label;
    private JLabel e3Label;
    private JLabel totalSaidaLabel;

    private int totalGerado = 0;
    private int e1 = 0, e2 = 0, e3 = 0, saida = 0;

    public PainelEstatisticas() {
        setLayout(new GridLayout(5, 2, 15, 10));
        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY, 2),
                "📊 Estatísticas Globais do Sistema",
                0,
                0,
                new Font("Arial", Font.BOLD, 14)
        ));
        setBackground(new Color(240, 240, 240));

        // 🔧 NOVO: Labels com melhor formatação
        totalGeradoLabel = criarLabel("Total Gerado: 0", new Color(33, 150, 243));
        e1Label = criarLabel("E1: 0", new Color(76, 175, 80));
        e2Label = criarLabel("E2: 0", new Color(255, 152, 0));
        e3Label = criarLabel("E3: 0", new Color(156, 39, 176));
        totalSaidaLabel = criarLabel("Total Saída: 0", new Color(244, 67, 54));

        add(totalGeradoLabel);
        add(e1Label);
        add(e2Label);
        add(e3Label);
        add(totalSaidaLabel);
    }

    /**
     * Cria um label com formatação personalizada
     */
    private JLabel criarLabel(String texto, Color cor) {
        JLabel label = new JLabel(texto, SwingConstants.CENTER);
        label.setFont(new Font("Arial", Font.BOLD, 16));
        label.setForeground(cor);
        label.setOpaque(true);
        label.setBackground(Color.WHITE);
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        return label;
    }

    /**
     * Incrementa contador de veículos gerados por entrada
     */
    public synchronized void incrementarGerado(String entrada) {
        totalGerado++;
        switch (entrada) {
            case "E1" -> e1++;
            case "E2" -> e2++;
            case "E3" -> e3++;
        }
        atualizar();
    }

    /**
     * Incrementa contador de veículos que saíram
     * 🔧 CORREÇÃO: Agora atualiza corretamente
     */
    public synchronized void incrementarSaidas() {
        saida++;
        atualizar();
        System.out.printf("[PainelEstatisticas] Saídas atualizadas: %d%n", saida);
    }

    /**
     * Atualiza todos os labels no EDT (Event Dispatch Thread)
     */
    private void atualizar() {
        SwingUtilities.invokeLater(() -> {
            totalGeradoLabel.setText(String.format("Total Gerado: %d", totalGerado));
            e1Label.setText(String.format("E1: %d", e1));
            e2Label.setText(String.format("E2: %d", e2));
            e3Label.setText(String.format("E3: %d", e3));
            totalSaidaLabel.setText(String.format("Total Saída: %d", saida));
        });
    }
}