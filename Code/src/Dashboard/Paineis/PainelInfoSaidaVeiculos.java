/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.util.List;
import javax.swing.*;
import java.awt.*;

/**
 * 
 * 
 */
public class PainelInfoSaidaVeiculos extends JPanel {

    private final DefaultTableModel modeloTabela;
    private final JTable tabela;
    
    /**
     * 
     * 
     */
    public PainelInfoSaidaVeiculos() {

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Veículos que Saíram do Sistema",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        // Modelo da tabela
        String[] colunas = {"ID", "Tipo", "Percurso", "Tempo Total"};
        modeloTabela = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        tabela = new JTable(modeloTabela);
        DashboardUIUtils.configurarTabela(tabela);

        // Larguras
        tabela.getColumnModel().getColumn(0).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(90);

        // Ordenação
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modeloTabela);
        tabela.setRowSorter(sorter);
        sorter.setSortable(2, false); // percurso não ordenável

        JScrollPane scrollPane = new JScrollPane(tabela);
        DashboardUIUtils.configurarScroll(scrollPane);

        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Adiciona um veículo que saiu ao registo visual.
     * Chamado diretamente pelo ServidorDashboard.
     * 
     * @param id
     * @param tipo
     * @param origem
     * @param percurso
     * @param tempoTotalSegundos 
     */
    public void adicionarVeiculoSaiu(String id, String tipo, String origem, List<String> percurso, long tempoTotalSegundos) {
        SwingUtilities.invokeLater(() -> {

            // Constrói string do percurso
            StringBuilder sb = new StringBuilder(origem);

            for (String p : percurso) {
                sb.append(" → ").append(p);
            }

            modeloTabela.addRow(new Object[]{
                    id,
                    tipo,
                    sb.toString(),
                    tempoTotalSegundos + " s"
            });

            // Scroll automático para última entrada
            int row = modeloTabela.getRowCount() - 1;
            if (row >= 0) {
                tabela.scrollRectToVisible(tabela.getCellRect(row, 0, true));
            }
        });
    }
}

