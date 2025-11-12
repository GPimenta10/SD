package Dashboard;

import com.google.gson.JsonArray;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;

/**
 * Painel inferior que mostra os veículos que já saíram do sistema.
 */
public class PainelVeiculos extends JPanel {

    private DefaultTableModel modeloTabela;
    private JTable tabela;

    public PainelVeiculos() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Veículos que Saíram do Sistema"));

        String[] colunas = {"ID", "Tipo", "Entrada", "Percurso", "Tempo (s)"};
        modeloTabela = new DefaultTableModel(colunas, 0);
        tabela = new JTable(modeloTabela);

        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    public synchronized void adicionarVeiculo(String id, String tipo, String entrada, String percurso, double tempo) {
        modeloTabela.addRow(new Object[]{id, tipo, entrada, percurso, String.format("%.2f", tempo)});
    }

    public void adicionarVeiculoSaiu(String id, String tipo, String entrada, JsonArray caminho, double tempoTotal) {
        SwingUtilities.invokeLater(() -> {
            // Converter JsonArray em String legível
            StringBuilder percurso = new StringBuilder("[");
            for (int i = 0; i < caminho.size(); i++) {
                percurso.append(caminho.get(i).getAsString());
                if (i < caminho.size() - 1) percurso.append(", ");
            }
            percurso.append("]");

            modeloTabela.addRow(new Object[]{
                    id, tipo, entrada, percurso.toString(), String.format("%.2f", tempoTotal)
            });
        });
    }
}
