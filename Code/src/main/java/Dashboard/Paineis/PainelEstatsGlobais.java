package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import java.awt.*;

/**
 * Painel superior com estatísticas globais modernizado para FlatLaf:
 */
public class PainelEstatsGlobais extends JPanel {

    private JLabel totalGeradoLabel;
    private JLabel e1Label;
    private JLabel e2Label;
    private JLabel e3Label;
    private JLabel totalSaidaLabel;

    private int totalGerado = 0;
    private int e1 = 0, e2 = 0, e3 = 0, saida = 0;

    /**
     *
     */
    public PainelEstatsGlobais() {
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Estatísticas Globais do Sistema",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        // ==== Fundo baseado no tema ====
        setBackground(UIManager.getColor("Panel.background"));

        // ==== Layout ====
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.5; // Peso igual para ambas as colunas
        gbc.weighty = 1.0;

        // ==== Labels com tons suaves (cores temáticas mas modernas) ====
        totalGeradoLabel = criarLabel("Total Gerado: 0", new Color(112, 74, 63));
        e1Label = criarLabel("E1: 0", new Color(59, 80, 87));
        e2Label = criarLabel("E2: 0", new Color(59, 80, 87));
        e3Label = criarLabel("E3: 0", new Color(112, 74, 63));
        totalSaidaLabel = criarLabel("Total Saída: 0", new Color(87, 59, 78));

        int row = 0;

        // Linha 1: Total Gerado | E1
        gbc.gridx = 0; gbc.gridy = row; gbc.gridwidth = 1;
        add(totalGeradoLabel, gbc);
        gbc.gridx = 1;
        add(e1Label, gbc);

        // Linha 2: E2 | E3
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        add(e2Label, gbc);
        gbc.gridx = 1;
        add(e3Label, gbc);

        // Linha 3: Total Saída
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        add(totalSaidaLabel, gbc);
    }

    /**
     *
     *
     * @param texto
     * @param corFundo
     * @return
     */
    private JLabel criarLabel(String texto, Color corFundo) {

        JLabel label = new JLabel(texto, SwingConstants.CENTER);

        label.setFont(new Font("Consolas", Font.BOLD, 16));
        label.setForeground(UIManager.getColor("Label.foreground"));
        label.setOpaque(true);

        // Garantir que a cor de fundo seja completamente opaca
        Color corOpaca = new Color(corFundo.getRed(), corFundo.getGreen(), corFundo.getBlue(), 255);
        label.setBackground(corOpaca);

        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(corOpaca.darker(), 1, true),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        label.setMinimumSize(new Dimension(0, 45));
        label.setPreferredSize(new Dimension(200, 45));
        return label;
    }

    // ======================
    // Atualizações dos valores
    // ======================
    /**
     *
     * @param entrada
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
     *
     */
    public synchronized void incrementarSaidas() {
        saida++;
        atualizar();
        System.out.printf("[PainelEstatsGlobais] Saídas atualizadas: %d%n", saida);
    }

    /**
     *
     */
    private void atualizar() {
        SwingUtilities.invokeLater(() -> {
            totalGeradoLabel.setText("Total Gerado: " + totalGerado);
            e1Label.setText("E1: " + e1);
            e2Label.setText("E2: " + e2);
            e3Label.setText("E3: " + e3);
            totalSaidaLabel.setText("Total Saída: " + saida);
        });
    }
}