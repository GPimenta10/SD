package Dashboard;

import javax.swing.*;
import java.awt.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class PainelResumo extends JPanel {

    private JLabel lblTotal;
    private JLabel lblE1;
    private JLabel lblE2;
    private JLabel lblE3;
    private JLabel lblSaidas;


    public PainelResumo() {
        setLayout(new GridLayout(1, 5));
        setBackground(new Color(240, 248, 255));

        lblTotal = criarLabel("Total Entraram: 0", new Color(0, 102, 204));
        lblE1 = criarLabel("E1: 0", Color.DARK_GRAY);
        lblE2 = criarLabel("E2: 0", Color.DARK_GRAY);
        lblE3 = criarLabel("E3: 0", Color.DARK_GRAY);
        lblSaidas = criarLabel("Saíram: 0", new Color(204, 0, 0));

        add(lblTotal);
        add(lblE1);
        add(lblE2);
        add(lblE3);
        add(lblSaidas);
    }

    private JLabel criarLabel(String texto, Color cor) {
        JLabel lbl = new JLabel(texto, SwingConstants.CENTER);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lbl.setForeground(cor);
        return lbl;
    }

    public void atualizar(int total, Map<String, AtomicInteger> entradas, int saidas) {
        lblTotal.setText("Total Entraram: " + total);
        lblE1.setText("Entrada 1: " + entradas.getOrDefault("E1", new AtomicInteger(0)).get());
        lblE2.setText("Entrada 2: " + entradas.getOrDefault("E2", new AtomicInteger(0)).get());
        lblE3.setText("Entrada 3: " + entradas.getOrDefault("E3", new AtomicInteger(0)).get());
        lblSaidas.setText("Saíram: " + saidas);
    }
}
