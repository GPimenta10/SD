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
public class PainelEstatsCruzamentos extends JPanel {

    private final Map<String, Map<String, EstatisticasSemaforo>> estatisticas;
    private final Map<String, DefaultTableModel> modelosTabelas;

    /**
     *
     *
     */
    private static class EstatisticasSemaforo {
        int atual = 0;
        int maximo = 0;
        int minimo = Integer.MAX_VALUE;
        long soma = 0;
        long contagem = 0;

        void registar(int novoValor) {
            atual = novoValor;
            if (novoValor > maximo) maximo = novoValor;
            if (novoValor < minimo) minimo = novoValor;
            soma += novoValor;
            contagem++;
        }

        double media() {
            return contagem > 0 ? (double) soma / contagem : 0.0;
        }
    }

    /**
     *
     *
     */
    public PainelEstatsCruzamentos() {
        this.estatisticas = new HashMap<>();
        this.modelosTabelas = new HashMap<>();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIManager.getColor("Panel.background"));

        for (int i = 1; i <= 5; i++) {
            adicionarPainelCruzamento("Cr" + i);
        }
    }

    /**
     *
     *
     * @param cruzamento
     */
    private void adicionarPainelCruzamento(String cruzamento) {

        estatisticas.put(cruzamento, new HashMap<>());

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(UIManager.getColor("Panel.background")); // ✔ moderno

        // ======================
        // Border moderna do FlatLaf
        // ======================
        painel.setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Estatísticas — " + cruzamento,
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        // ======================
        // Modelo moderno da tabela
        // ======================
        String[] colunas = {"Semáforo", "Atual", "Mínimo", "Médio", "Máximo"};

        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        modelosTabelas.put(cruzamento, modelo);

        JTable tabela = new JTable(modelo);
        DashboardUIUtils.configurarTabela(tabela);

        JScrollPane scroll = new JScrollPane(tabela);
        DashboardUIUtils.configurarScroll(scroll);

        painel.add(scroll, BorderLayout.CENTER);

        add(painel);
        add(Box.createRigidArea(new Dimension(0, 10)));
    }

    /**
     *
     *
     * @param cruzamento
     * @param semaforo
     * @param filaAtual
     */
    public void atualizarFila(String cruzamento, String semaforo, int filaAtual) {

        estatisticas.putIfAbsent(cruzamento, new HashMap<>());
        Map<String, EstatisticasSemaforo> mapa = estatisticas.get(cruzamento);

        EstatisticasSemaforo stats =
                mapa.computeIfAbsent(semaforo, k -> new EstatisticasSemaforo());

        stats.registar(filaAtual);

        atualizarTabela(cruzamento);
    }

    /**
     *
     *
     * @param cruzamento
     */
    private void atualizarTabela(String cruzamento) {
        DefaultTableModel modelo = modelosTabelas.get(cruzamento);
        Map<String, EstatisticasSemaforo> mapa = estatisticas.get(cruzamento);

        modelo.setRowCount(0);

        for (String semaforo : mapa.keySet()) {
            EstatisticasSemaforo s = mapa.get(semaforo);

            modelo.addRow(new Object[]{
                    semaforo,
                    s.atual,
                    s.minimo == Integer.MAX_VALUE ? "-" : s.minimo,
                    String.format("%.1f", s.media()),
                    s.maximo
            });
        }
    }
}
