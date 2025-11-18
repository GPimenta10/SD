package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;
import com.google.gson.JsonArray;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;

public class PainelInfoSaidaVeiculos extends JPanel {

    private DefaultTableModel modeloTabela;
    private JTable tabela;
    private JScrollPane scrollPane;

    public PainelInfoSaidaVeiculos() {

        // Layout e fundo modernos
        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        // Border moderna do FlatLaf
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Veículos que Saíram do Sistema",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        // ============================
        // Modelo da tabela
        // ============================
        String[] colunas = {"ID", "Tipo", "Percurso", "Tempo Total"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tabela = new JTable(modeloTabela);

        // Estilo moderno aplicado
        DashboardUIUtils.configurarTabela(tabela);

        // Larguras das colunas
        tabela.getColumnModel().getColumn(0).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(250);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(90);

        // Ordenação
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modeloTabela);
        tabela.setRowSorter(sorter);
        sorter.setSortable(2, false); // percurso não é ordenável

        // ============================
        // Scroll moderno
        // ============================
        scrollPane = new JScrollPane(tabela);
        DashboardUIUtils.configurarScroll(scrollPane);

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Adiciona veículo à tabela
     */
    public void adicionarVeiculoSaiu(String id, String tipo, String entrada, JsonArray caminho, double tempoTotal) {

        SwingUtilities.invokeLater(() -> {

            // Percurso formatado
            StringBuilder percurso = new StringBuilder(entrada);
            for (int i = 0; i < caminho.size(); i++) {
                percurso.append(" → ").append(caminho.get(i).getAsString());
            }

            modeloTabela.addRow(new Object[]{
                    id,
                    tipo,
                    percurso.toString(),
                    String.format("%.2f", tempoTotal)
            });

            // Scroll automático para a última linha
            int row = modeloTabela.getRowCount() - 1;
            if (row >= 0) {
                tabela.scrollRectToVisible(tabela.getCellRect(row, 0, true));
            }
        });
    }
}
