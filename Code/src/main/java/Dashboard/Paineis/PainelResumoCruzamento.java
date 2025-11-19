package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Painel que mostra quantos veículos de cada tipo passaram por cada cruzamento.
 * Tabela única agrupando todos os cruzamentos (Cr1 a Cr5).
 */
public class PainelResumoCruzamento extends JPanel {

    private final DefaultTableModel modelo;
    private final Map<String, Map<String, Integer>> contadores; // Cruzamento -> {Tipo -> Quantidade}

    public PainelResumoCruzamento() {

        this.contadores = new HashMap<>();

        // Inicializa contadores para Cr1..Cr5
        for (int i = 1; i <= 5; i++) {
            String cruzamento = "Cr" + i;
            Map<String, Integer> tipos = new HashMap<>();
            tipos.put("MOTA", 0);
            tipos.put("CARRO", 0);
            tipos.put("CAMIÃO", 0);
            contadores.put(cruzamento, tipos);
        }

        setLayout(new BorderLayout());

        // Fundo moderno do tema
        setBackground(UIManager.getColor("Panel.background"));

        // Border padrão FlatLaf com título na fonte/cor do tema
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Veículos por Cruzamento e Tipo",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        // Modelo da tabela
        String[] colunas = {"Cruzamento", "Motas", "Carros", "Camiões", "Total"};

        modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        // Criar linhas iniciais Cr1..Cr5
        for (int i = 1; i <= 5; i++) {
            modelo.addRow(new Object[]{"Cr" + i, 0, 0, 0, 0});
        }

        JTable tabela = new JTable(modelo);

        // Estilo global do dashboard
        DashboardUIUtils.configurarTabela(tabela);

        // Ajustes específicos deste painel
        tabela.getColumnModel().getColumn(0).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(80);

        JScrollPane scroll = new JScrollPane(tabela);

        // Estilo do scroll
        DashboardUIUtils.configurarScroll(scroll);

        // Força uma altura confortável
        scroll.setPreferredSize(new Dimension(0, 160));

        add(scroll, BorderLayout.CENTER);
    }

    /**
     * Regista passagem de um veículo por um cruzamento.
     */
    public void registarVeiculo(String cruzamento, String tipo) {

        String tipoNorm = tipo.toUpperCase().replace("CAMIAO", "CAMIÃO").trim();

        Map<String, Integer> tipos = contadores.get(cruzamento);
        if (tipos == null || !tipos.containsKey(tipoNorm)) return;

        tipos.put(tipoNorm, tipos.get(tipoNorm) + 1);
        atualizarTabela();
    }

    /**
     * Atualiza a tabela visualmente.
     */
    private void atualizarTabela() {

        for (int i = 0; i < modelo.getRowCount(); i++) {

            String cruz = (String) modelo.getValueAt(i, 0);
            Map<String, Integer> tipos = contadores.get(cruz);

            if (tipos != null) {

                int motas   = tipos.get("MOTA");
                int carros  = tipos.get("CARRO");
                int camioes = tipos.get("CAMIÃO");
                int total   = motas + carros + camioes;

                modelo.setValueAt(motas,   i, 1);
                modelo.setValueAt(carros,  i, 2);
                modelo.setValueAt(camioes, i, 3);
                modelo.setValueAt(total,   i, 4);
            }
        }
    }
}