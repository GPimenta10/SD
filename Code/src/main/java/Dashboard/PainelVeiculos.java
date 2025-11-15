package Dashboard;

import com.google.gson.JsonArray;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;

/**
 * Painel inferior que mostra os veículos que já saíram do sistema.
 *
 * 🔧 CORREÇÃO:
 * - Adicionado scroll automático
 * - Layout otimizado para ocupar menos espaço
 * - Tabela com altura fixa e scroll vertical
 */
public class PainelVeiculos extends JPanel {

    private DefaultTableModel modeloTabela;
    private JTable tabela;
    private JScrollPane scrollPane;

    public PainelVeiculos() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Veículos que Saíram do Sistema"));

        String[] colunas = {"ID", "Tipo", "Percurso", "Tempo Total"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(modeloTabela);

        // 🔧 NOVO: Configurações de aparência da tabela
        tabela.setFillsViewportHeight(true);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        tabela.getTableHeader().setReorderingAllowed(false); // Não permite reordenar colunas

        // Ajusta larguras das colunas
        tabela.getColumnModel().getColumn(0).setPreferredWidth(100); // ID
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);  // Tipo
        tabela.getColumnModel().getColumn(2).setPreferredWidth(250); // Percurso
        tabela.getColumnModel().getColumn(3).setPreferredWidth(90);  // Tempo

        // 🔧 NOVO: ScrollPane com scroll vertical sempre visível
        scrollPane = new JScrollPane(tabela);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Adiciona um veículo à tabela de saídas
     * Não está a ser usado
     *
    public synchronized void adicionarVeiculo(String id, String tipo, String entrada, String percurso, double tempo) {
        SwingUtilities.invokeLater(() -> {
            modeloTabela.addRow(new Object[]{id, tipo, entrada, percurso, String.format("%.2f", tempo)});

            // 🔧 NOVO: Scroll automático para a última linha
            int ultimaLinha = modeloTabela.getRowCount() - 1;
            if (ultimaLinha >= 0) {
                tabela.scrollRectToVisible(tabela.getCellRect(ultimaLinha, 0, true));
            }
        });
    }*/

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