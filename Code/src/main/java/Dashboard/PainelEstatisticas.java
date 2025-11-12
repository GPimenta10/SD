package Dashboard;

import javax.swing.*;
import java.awt.*;

/**
 * Painel superior com estatísticas globais:
 * - Total de veículos gerados
 * - Total por entrada (E1, E2, E3)
 * - Total de veículos que saíram
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
        setLayout(new GridLayout(1, 5, 10, 10));
        setBorder(BorderFactory.createTitledBorder("Estatísticas Globais"));

        totalGeradoLabel = new JLabel("Total Gerado: 0");
        e1Label = new JLabel("E1: 0");
        e2Label = new JLabel("E2: 0");
        e3Label = new JLabel("E3: 0");
        totalSaidaLabel = new JLabel("Total Saída: 0");

        add(totalGeradoLabel);
        add(e1Label);
        add(e2Label);
        add(e3Label);
        add(totalSaidaLabel);
    }

    // === Atualizações ===
    public synchronized void incrementarGerado(String entrada) {
        totalGerado++;
        switch (entrada) {
            case "E1" -> e1++;
            case "E2" -> e2++;
            case "E3" -> e3++;
        }
        atualizar();
    }

    public synchronized void incrementarSaidas() {
        saida++;
        atualizar();
    }

    private void atualizar() {
        totalGeradoLabel.setText("Total Gerado: " + totalGerado);
        e1Label.setText("E1: " + e1);
        e2Label.setText("E2: " + e2);
        e3Label.setText("E3: " + e3);
        totalSaidaLabel.setText("Total Saída: " + saida);
    }
}
