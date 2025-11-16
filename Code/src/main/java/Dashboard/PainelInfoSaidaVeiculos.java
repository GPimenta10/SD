package Dashboard;

import com.google.gson.JsonArray;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.*;

/**
 * Painel que mostra os veículos que já saíram do sistema.
 *
 */
public class PainelInfoSaidaVeiculos extends JPanel {

    private DefaultTableModel modeloTabela;
    private JTable tabela;
    private JScrollPane scrollPane;

    public PainelInfoSaidaVeiculos() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Veículos que Saíram do Sistema"));
        ((javax.swing.border.TitledBorder) getBorder()).setTitleColor(Color.WHITE);

        setBackground(new Color(40, 42, 54));

        String[] colunas = {"ID", "Tipo", "Percurso", "Tempo Total"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(modeloTabela);

        // === 🎨 Dracula Theme ===
        tabela.setBackground(new Color(40, 42, 54));       // Fundo da tabela
        tabela.setForeground(new Color(248, 248, 242));    // Texto principal
        tabela.setSelectionBackground(new Color(68, 71, 90));
        tabela.setSelectionForeground(new Color(248, 248, 242));
        tabela.setGridColor(new Color(68, 71, 90));        // Linhas da tabela

        tabela.setFont(new Font("Consolas", Font.PLAIN, 13));

        // Header estilo Dracula
        JTableHeader header = tabela.getTableHeader();
        header.setBackground(new Color(62, 72, 164));      // Azul Dracula
        header.setForeground(Color.WHITE);
        header.setFont(new Font("Consolas", Font.BOLD, 14));

        //Configurações de aparência da tabela
        tabela.setFillsViewportHeight(true);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabela.getTableHeader().setReorderingAllowed(false);

        // Ajusta larguras das colunas
        tabela.getColumnModel().getColumn(0).setPreferredWidth(100); // ID
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);  // Tipo
        tabela.getColumnModel().getColumn(2).setPreferredWidth(250); // Percurso
        tabela.getColumnModel().getColumn(3).setPreferredWidth(90);  // Tempo

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modeloTabela);
        tabela.setRowSorter(sorter);

        //Ordenar colunas
        sorter.setSortable(0, true);  // ID
        sorter.setSortable(1, true);  // Tipo
        sorter.setSortable(2, false); // Percurso → desativado
        sorter.setSortable(3, true);

        //ScrollPane com scroll vertical sempre visível
        scrollPane = new JScrollPane(tabela);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Adiciona veículo que saiu do sistema à tabela com dados detalhados do JSON
     *
     * @param id - ID Veiculo
     * @param tipo - Tipo Veiculo
     * @param entrada - Local por onde o veículo entrou
     * @param caminho - Caminho percorrido até à saida do sistema
     * @param tempoTotal - Tempo que demorou a percorer o sistema
     */
    public void adicionarVeiculoSaiu(String id, String tipo, String entrada, JsonArray caminho, double tempoTotal) {
        SwingUtilities.invokeLater(() -> {
            // Converter JsonArray em String legível
            StringBuilder percurso = new StringBuilder();
            for (int i = 0; i < caminho.size(); i++) {
                percurso.append(caminho.get(i).getAsString());
                if (i < caminho.size() - 1) percurso.append(" → ");
            }

            StringBuilder percursoCompleto = new StringBuilder();
            percursoCompleto.append(entrada);

            for (int i = 0; i < caminho.size(); i++) {
                percursoCompleto.append(" → ").append(caminho.get(i).getAsString());
            }

            modeloTabela.addRow(new Object[]{
                    id,
                    tipo,
                    percursoCompleto.toString(),
                    String.format("%.2f", tempoTotal)
            });

            int ultimaLinha = modeloTabela.getRowCount() - 1;
            if (ultimaLinha >= 0) {
                tabela.scrollRectToVisible(tabela.getCellRect(ultimaLinha, 0, true));
            }
        });
    }
}