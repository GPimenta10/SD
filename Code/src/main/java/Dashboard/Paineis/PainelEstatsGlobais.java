package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;
import Utils.GestorEstatisticas.EstatisticasGlobais;

import javax.swing.*;
import java.awt.*;

/**
 * Painel de Estatísticas Globais (somente UI).
 * Não calcula nada — recebe dados prontos do GestorEstatisticas.
 */
public class PainelEstatsGlobais extends JPanel {

    private JLabel totalGeradoLabel;
    private JLabel e1Label;
    private JLabel e2Label;
    private JLabel e3Label;
    private JLabel totalSaidaLabel;

    /**
     * Construtor: constrói apenas a interface.
     */
    public PainelEstatsGlobais() {

        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Estatísticas Globais do Sistema",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        setBackground(UIManager.getColor("Panel.background"));
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0.5;
        gbc.weighty = 1.0;

        totalGeradoLabel = criarLabel("Total Gerado: 0", new Color(112, 74, 63));
        e1Label = criarLabel("E1: 0", new Color(59, 80, 87));
        e2Label = criarLabel("E2: 0", new Color(59, 80, 87));
        e3Label = criarLabel("E3: 0", new Color(112, 74, 63));
        totalSaidaLabel = criarLabel("Total Saída: 0", new Color(87, 59, 78));

        int row = 0;

        gbc.gridx = 0; gbc.gridy = row;
        add(totalGeradoLabel, gbc);
        gbc.gridx = 1;
        add(e1Label, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        add(e2Label, gbc);
        gbc.gridx = 1;
        add(e3Label, gbc);

        row++;
        gbc.gridx = 0; gbc.gridy = row;
        gbc.gridwidth = 2;
        add(totalSaidaLabel, gbc);
    }

    /**
     * Cria um label com estilo uniforme.
     */
    private JLabel criarLabel(String texto, Color corFundo) {

        JLabel label = new JLabel(texto, SwingConstants.CENTER);

        label.setFont(new Font("Consolas", Font.BOLD, 16));
        label.setForeground(UIManager.getColor("Label.foreground"));
        label.setOpaque(true);

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

    /**
     * Atualiza visualmente o painel com dados vindos do GestorEstatisticas.
     */
    public void atualizar(EstatisticasGlobais dados) {
        if (dados == null) return;

        SwingUtilities.invokeLater(() -> {
            totalGeradoLabel.setText("Total Gerado: " + dados.totalGerado);
            e1Label.setText("E1: " + dados.geradosE1);
            e2Label.setText("E2: " + dados.geradosE2);
            e3Label.setText("E3: " + dados.geradosE3);
            totalSaidaLabel.setText("Total Saída: " + dados.totalSaidas);
        });
    }
}
