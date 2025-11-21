package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class PainelEstatsSaida extends JPanel {

    private final Map<String, EstatisticasTipo> estatisticasPorTipo;
    private final DefaultTableModel modeloDwellingTime;
    private final DefaultTableModel modeloQuantidades;
    private final JTable tabelaDwellingTime;
    private final JTable tabelaQuantidades;

    /**
     * Classe interna para estatísticas
     */
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

    /**
     *
     */
    public PainelEstatsSaida() {

        // Fundo moderno (tema controla)
        setLayout(new BorderLayout(5, 5));
        setBackground(UIManager.getColor("Panel.background"));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Estatísticas iniciais
        estatisticasPorTipo = new HashMap<>();
        estatisticasPorTipo.put("MOTA", new EstatisticasTipo());
        estatisticasPorTipo.put("CARRO", new EstatisticasTipo());
        estatisticasPorTipo.put("CAMIAO", new EstatisticasTipo());

        // PAINEL: DWELLING TIME
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

        // PAINEL: QUANTIDADES
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

        // Disposição principal
        add(painelDwelling, BorderLayout.NORTH);
        add(painelQuantidades, BorderLayout.CENTER);

        atualizarTabelas();
    }

    /**
     *
     * @param tipo
     * @param dwellingTimeSegundos
     */
    public void atualizarEstatisticasSaida(String tipo, long dwellingTimeSegundos) {
        EstatisticasTipo stats = estatisticasPorTipo.get(tipo);
        if (stats != null) {
            stats.registar(dwellingTimeSegundos);
            atualizarTabelas();
        }
    }

    /**
     *
     */
    private void atualizarTabelas() {
        modeloDwellingTime.setRowCount(0);
        modeloQuantidades.setRowCount(0);

        String[] tipos = {"MOTA", "CARRO", "CAMIAO"};

        for (String tipo : tipos) {
            EstatisticasTipo stats = estatisticasPorTipo.get(tipo);

            // Dwelling time
            if (stats.quantidade > 0) {
                modeloDwellingTime.addRow(new Object[]{
                        tipo,
                        stats.dwellingTimeMin,
                        String.format("%.1f", stats.getMedia()),
                        stats.dwellingTimeMax
                });
            } else {
                modeloDwellingTime.addRow(new Object[]{tipo, "-", "-", "-"});
            }

            // Quantidades
            modeloQuantidades.addRow(new Object[]{
                    tipo,
                    stats.quantidade
            });
        }
    }
}
