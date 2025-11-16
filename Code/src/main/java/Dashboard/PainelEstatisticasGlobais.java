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
public class PainelEstatisticasGlobais extends JPanel {

    private JLabel totalGeradoLabel;
    private JLabel e1Label;
    private JLabel e2Label;
    private JLabel e3Label;
    private JLabel totalSaidaLabel;

    private int totalGerado = 0;
    private int e1 = 0, e2 = 0, e3 = 0, saida = 0;

    public PainelEstatisticasGlobais() {

        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(98, 114, 164), 2),
                "Estatísticas Globais do Sistema",
                0,
                0,
                new Font("Arial", Font.BOLD, 14)
        ));

        ((javax.swing.border.TitledBorder) getBorder()).setTitleColor(Color.WHITE);

        setBackground(new Color(40, 42, 54));

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.weightx = 1.0;

        // Labels com estilo Dracula
        totalGeradoLabel = criarLabel("Total Gerado: 0");
        e1Label = criarLabel("E1: 0");
        e2Label = criarLabel("E2: 0");
        e3Label = criarLabel("E3: 0");
        totalSaidaLabel = criarLabel("Total Saída: 0");

        int row = 0;

        // Linha 1: Total Gerado | E1
        gbc.gridx = 0; gbc.gridy = row;
        add(totalGeradoLabel, gbc);
        gbc.gridx = 1;
        add(e1Label, gbc);

        // Linha 2: E2 | E3
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        add(e2Label, gbc);
        gbc.gridx = 1;
        add(e3Label, gbc);

        // Linha 3: Total Saída (ocupa 2 colunas)
        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        add(totalSaidaLabel, gbc);
    }

    /**
     * Cria um label com formatação personalizada
     */
    private JLabel criarLabel(String texto) {
        JLabel label = new JLabel(texto, SwingConstants.CENTER);

        label.setFont(new Font("Consolas", Font.BOLD, 12));
        label.setForeground(new Color(248, 248, 242));   // Branco
        label.setOpaque(true);
        label.setBackground(new Color(40, 42, 54));      // Dracula background

        // 🔵 Borda azul — igual à tabela
        label.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(98, 114, 164), 2),  // Azul Dracula
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
        System.out.printf("[PainelEstatisticasGlobais] Saídas atualizadas: %d%n", saida);
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