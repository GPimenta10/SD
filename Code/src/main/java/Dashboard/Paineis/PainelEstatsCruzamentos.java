package Dashboard.Paineis;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import Dashboard.Estatisticas.EstatisticasFila;
import Dashboard.Utils.DashboardUIUtils;

public class PainelEstatsCruzamentos extends JPanel {

    private final Map<String, DefaultTableModel> modelosTabelas;

    public PainelEstatsCruzamentos() {

        modelosTabelas = new HashMap<>();

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(UIManager.getColor("Panel.background"));

        // Criar 5 blocos: Cr1..Cr5
        for (int i = 1; i <= 5; i++) {
            adicionarPainelCruzamento("Cr" + i);
        }
    }

    /**
     * Cria a secção visual para um cruzamento com uma tabela vazia.
     */
    private void adicionarPainelCruzamento(String cruzamento) {

        JPanel painel = new JPanel(new BorderLayout());
        painel.setBackground(UIManager.getColor("Panel.background"));

        painel.setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Estatísticas — " + cruzamento,
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        String[] colunas = {"Semáforo", "Atual", "Médio", "Máximo"};

        DefaultTableModel modelo = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
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
     * Atualiza a tabela com estatísticas vindas diretamente do GestorEstatisticas.
     *
     * Chamado pelo DashboardFrame quando o GestorEstatisticas notifica
     * onEstatisticasCruzamentoAtualizadas(cruzamento, filas)
     */
    public void atualizarCruzamento(String cruzamento,
                                    Map<String, EstatisticasFila> filas) {

        DefaultTableModel modelo = modelosTabelas.get(cruzamento);
        if (modelo == null) return;

        modelo.setRowCount(0); // limpar tabela

        for (Map.Entry<String, EstatisticasFila> entry : filas.entrySet()) {

            String semaforo = entry.getKey();
            EstatisticasFila s = entry.getValue();

            modelo.addRow(new Object[]{
                    semaforo,
                    s.getAtual(),
                    String.format("%.1f", s.getMedia()),
                    s.getMaximo()
            });
        }
    }
}