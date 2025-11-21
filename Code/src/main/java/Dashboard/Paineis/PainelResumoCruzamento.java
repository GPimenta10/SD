/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.table.DefaultTableModel;
import java.util.Map;
import javax.swing.*;
import java.awt.*;

/**
 * Painel que mostra quantos veículos de cada tipo passaram por cada cruzamento.
 * Não faz cálculos — recebe tudo pronto do GestorEstatisticas.
 */
public class PainelResumoCruzamento extends JPanel {

    private final DefaultTableModel modelo;

    public PainelResumoCruzamento() {

        setLayout(new BorderLayout());
        setBackground(UIManager.getColor("Panel.background"));

        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Veículos por Cruzamento e Tipo",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        String[] colunas = {"Cruzamento", "Motas", "Carros", "Camiões", "Total"};

        modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        // Criar as 5 linhas vazias Cr1..Cr5
        for (int i = 1; i <= 5; i++) {
            modelo.addRow(new Object[]{"Cr" + i, 0, 0, 0, 0});
        }

        JTable tabela = new JTable(modelo);
        DashboardUIUtils.configurarTabela(tabela);

        tabela.getColumnModel().getColumn(0).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(tabela);
        DashboardUIUtils.configurarScroll(scroll);
        scroll.setPreferredSize(new Dimension(0, 160));

        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Atualiza toda a tabela com os dados fornecidos pelo GestorEstatisticas.
     *
     */
    public void atualizar(Map<String, Map<String, Integer>> resumo) {
        if (resumo == null) return;

        SwingUtilities.invokeLater(() -> {

            for (int i = 0; i < modelo.getRowCount(); i++) {

                String cruz = (String) modelo.getValueAt(i, 0);

                Map<String, Integer> tipos = resumo.get(cruz);
                if (tipos == null) continue;

                int motas   = tipos.getOrDefault("MOTA", 0);
                int carros  = tipos.getOrDefault("CARRO", 0);
                int camioes = tipos.getOrDefault("CAMIAO", 0);
                int total   = motas + carros + camioes;

                modelo.setValueAt(motas,   i, 1);
                modelo.setValueAt(carros,  i, 2);
                modelo.setValueAt(camioes, i, 3);
                modelo.setValueAt(total,   i, 4);
            }
        });
    }
}
