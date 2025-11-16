package Dashboard;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Painel que exibe estatísticas por tipo de veículo:
 * - Dwelling time (mínimo, médio, máximo)
 * - Quantidade de veículos que cruzaram o sistema
 */
public class PainelEstatisticasSaida extends JPanel {

    private final Map<String, EstatisticasTipo> estatisticasPorTipo;
    private final DefaultTableModel modeloDwellingTime;
    private final DefaultTableModel modeloQuantidades;
    private final JTable tabelaDwellingTime;
    private final JTable tabelaQuantidades;

    // Classe interna para guardar estatísticas de cada tipo
    private static class EstatisticasTipo {
        long dwellingTimeMin = Long.MAX_VALUE;
        long dwellingTimeMax = 0;
        long dwellingTimeTotal = 0;
        int quantidade = 0;

        void registar(long dwellingTime) {
            quantidade++;
            dwellingTimeTotal += dwellingTime;
            if (dwellingTime < dwellingTimeMin) dwellingTimeMin = dwellingTime;
            if (dwellingTime > dwellingTimeMax) dwellingTimeMax = dwellingTime;
        }

        double getMedia() {
            return quantidade > 0 ? (double) dwellingTimeTotal / quantidade : 0;
        }
    }

    public PainelEstatisticasSaida() {
        setLayout(new BorderLayout(5, 5));
        setBackground(new Color(40, 42, 54));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        estatisticasPorTipo = new HashMap<>();
        estatisticasPorTipo.put("MOTA", new EstatisticasTipo());
        estatisticasPorTipo.put("CARRO", new EstatisticasTipo());
        estatisticasPorTipo.put("CAMIAO", new EstatisticasTipo());

        // === Painel de Dwelling Time ===
        JPanel painelDwelling = new JPanel(new BorderLayout(5, 5));
        painelDwelling.setBackground(new Color(40, 42, 54));
        painelDwelling.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(98, 114, 164), 2),
                "⏱️ Dwelling Time (segundos)",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.WHITE
        ));

        String[] colunasDwelling = {"Tipo", "Mínimo", "Médio", "Máximo"};
        modeloDwellingTime = new DefaultTableModel(colunasDwelling, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaDwellingTime = new JTable(modeloDwellingTime);
        configurarTabela(tabelaDwellingTime);

        JScrollPane scrollDwelling = new JScrollPane(tabelaDwellingTime);
        scrollDwelling.setPreferredSize(new Dimension(0, 120));
        configurarScrollPane(scrollDwelling);
        painelDwelling.add(scrollDwelling, BorderLayout.CENTER);

        // === Painel de Quantidades ===
        JPanel painelQuantidades = new JPanel(new BorderLayout(5, 5));
        painelQuantidades.setBackground(new Color(40, 42, 54));
        painelQuantidades.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(98, 114, 164), 2),
                "📊 Veículos por Tipo",
                0, 0,
                new Font("Arial", Font.BOLD, 12),
                Color.WHITE
        ));

        String[] colunasQuantidade = {"Tipo", "Quantidade"};
        modeloQuantidades = new DefaultTableModel(colunasQuantidade, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaQuantidades = new JTable(modeloQuantidades);
        configurarTabela(tabelaQuantidades);

        JScrollPane scrollQuantidades = new JScrollPane(tabelaQuantidades);
        scrollQuantidades.setPreferredSize(new Dimension(0, 120));
        configurarScrollPane(scrollQuantidades);
        painelQuantidades.add(scrollQuantidades, BorderLayout.CENTER);

        // === Layout principal ===
        add(painelDwelling, BorderLayout.NORTH);
        add(painelQuantidades, BorderLayout.CENTER);

        // Inicializa as tabelas
        atualizarTabelas();
    }

    private void configurarTabela(JTable tabela) {
        tabela.setFont(new Font("Monospaced", Font.PLAIN, 14));
        tabela.setRowHeight(25);
        tabela.setBackground(new Color(68, 71, 90));
        tabela.setForeground(new Color(248, 248, 242));
        tabela.setSelectionBackground(new Color(98, 114, 164));
        tabela.setSelectionForeground(Color.WHITE);
        tabela.setGridColor(new Color(98, 114, 164));
        tabela.getTableHeader().setBackground(new Color(68, 71, 90));
        tabela.getTableHeader().setForeground(Color.WHITE);
        tabela.getTableHeader().setFont(new Font("Arial", Font.BOLD, 11));

        // Centraliza o conteúdo das células
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }
    }

    private void configurarScrollPane(JScrollPane scroll) {
        scroll.setBackground(new Color(40, 42, 54));
        scroll.getViewport().setBackground(new Color(68, 71, 90));
        scroll.setBorder(BorderFactory.createLineBorder(new Color(98, 114, 164), 1));
    }

    /**
     * Registra o dwelling time de um veículo que saiu do sistema
     */
    public void registarVeiculoSaida(String tipo, long dwellingTimeSegundos) {
        EstatisticasTipo stats = estatisticasPorTipo.get(tipo);
        if (stats != null) {
            stats.registar(dwellingTimeSegundos);
            atualizarTabelas();
        }
    }

    /**
     * Atualiza as tabelas com os dados atuais
     */
    private void atualizarTabelas() {
        // Limpa as tabelas
        modeloDwellingTime.setRowCount(0);
        modeloQuantidades.setRowCount(0);

        // Ordem de exibição
        String[] tipos = {"MOTA", "CARRO", "CAMIAO"};
        String[] emojis = {"🏍️", "🚗", "🚛"};

        for (int i = 0; i < tipos.length; i++) {
            String tipo = tipos[i];
            String emoji = emojis[i];
            EstatisticasTipo stats = estatisticasPorTipo.get(tipo);

            if (stats.quantidade > 0) {
                // Dwelling Time
                modeloDwellingTime.addRow(new Object[]{
                        emoji + " " + tipo,
                        stats.dwellingTimeMin,
                        String.format("%.1f", stats.getMedia()),
                        stats.dwellingTimeMax
                });

                // Quantidade
                modeloQuantidades.addRow(new Object[]{
                        emoji + " " + tipo,
                        stats.quantidade
                });
            } else {
                // Se não houver dados, mostra zeros
                modeloDwellingTime.addRow(new Object[]{
                        emoji + " " + tipo,
                        "-",
                        "-",
                        "-"
                });

                modeloQuantidades.addRow(new Object[]{
                        emoji + " " + tipo,
                        0
                });
            }
        }
    }

    /**
     * Reseta todas as estatísticas
     */
    public void resetar() {
        for (EstatisticasTipo stats : estatisticasPorTipo.values()) {
            stats.dwellingTimeMin = Long.MAX_VALUE;
            stats.dwellingTimeMax = 0;
            stats.dwellingTimeTotal = 0;
            stats.quantidade = 0;
        }
        atualizarTabelas();
    }
}