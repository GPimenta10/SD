package Dashboard.Paineis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import Dashboard.Estatisticas.EstatisticasSaida;
import Dashboard.Utils.DashboardUIUtils;

/**
 * Painel de estatísticas de saída (dwelling time e quantidades).
 * Não calcula nada — recebe dados prontos do GestorEstatisticas.
 */
public class PainelEstatsSaida extends JPanel {

    private final DefaultTableModel modeloDwellingTime;
    private final DefaultTableModel modeloQuantidades;
    private final JTable tabelaDwellingTime;
    private final JTable tabelaQuantidades;

    public PainelEstatsSaida() {

        setLayout(new BorderLayout(5, 5));
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // ============= PAINEL DWELLING TIME =============
        JPanel painelDwelling = new JPanel(new BorderLayout(5, 5));
        painelDwelling.setBackground(UIManager.getColor("Panel.background"));
        painelDwelling.setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Tempo de permanência no sistema",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        modeloDwellingTime = new DefaultTableModel(
                new String[]{"Tipo", "Mínimo", "Médio", "Máximo"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelaDwellingTime = new JTable(modeloDwellingTime);
        DashboardUIUtils.configurarTabela(tabelaDwellingTime);

        JScrollPane scrollDwelling = new JScrollPane(tabelaDwellingTime);
        scrollDwelling.setPreferredSize(new Dimension(0, 120));
        DashboardUIUtils.configurarScroll(scrollDwelling);

        painelDwelling.add(scrollDwelling, BorderLayout.CENTER);

        // ============= PAINEL QUANTIDADES =============
        JPanel painelQuantidades = new JPanel(new BorderLayout(5, 5));
        painelQuantidades.setBackground(UIManager.getColor("Panel.background"));
        painelQuantidades.setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Veículos por Tipo",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        modeloQuantidades = new DefaultTableModel(
                new String[]{"Tipo", "Quantidade"}, 0
        ) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabelaQuantidades = new JTable(modeloQuantidades);
        DashboardUIUtils.configurarTabela(tabelaQuantidades);

        JScrollPane scrollQuantidades = new JScrollPane(tabelaQuantidades);
        scrollQuantidades.setPreferredSize(new Dimension(0, 120));
        DashboardUIUtils.configurarScroll(scrollQuantidades);

        painelQuantidades.add(scrollQuantidades, BorderLayout.CENTER);

        // ============= DISPOSIÇÃO FINAL ==============
        add(painelDwelling, BorderLayout.NORTH);
        add(painelQuantidades, BorderLayout.CENTER);
    }

    /**
     * Atualiza o painel com estatísticas recebidas do GestorEstatisticas.
     *
     * Chamado pelo DashboardFrame:
     *   painelEstatsSaida.atualizar(estatisticasPorTipo);
     */
    public void atualizar(Map<String, EstatisticasSaida> estatisticas) {
        if (estatisticas == null) return;

        SwingUtilities.invokeLater(() -> {

            modeloDwellingTime.setRowCount(0);
            modeloQuantidades.setRowCount(0);

            String[] tipos = {"MOTA", "CARRO", "CAMIAO"};

            for (String tipo : tipos) {

                EstatisticasSaida stats = estatisticas.get(tipo);

                if (stats != null) {

                    // Dwelling Time
                    if (stats.getQuantidade() > 0) {
                        modeloDwellingTime.addRow(new Object[]{
                                tipo,
                                stats.getMinimo(),
                                String.format("%.1f", stats.getMedia()),
                                stats.getMaximo()
                        });
                    } else {
                        modeloDwellingTime.addRow(new Object[]{tipo, "-", "-", "-"});
                    }

                    // Quantidades
                    modeloQuantidades.addRow(new Object[]{
                            tipo,
                            stats.getQuantidade()
                    });
                }
            }
        });
    }
}