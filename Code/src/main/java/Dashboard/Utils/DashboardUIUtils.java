package Dashboard.Utils;

import com.google.gson.JsonArray;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.util.List;
import javax.swing.*;
import java.awt.*;

/**
 * 
 * 
 */
public class DashboardUIUtils {
    
    /**
     * Fontes de letra a serem usadas
     */
    public static final Font FONTE_TITULO = new Font("Consolas", Font.BOLD, 16);
    public static final Font FONTE_CONSOLE = new Font("Consolas", Font.PLAIN, 13);

    /**
     * Método para padronizar titulos dos paineis
     * 
     * @param panel Painel onde se quer aplicar uniformização
     * @param titulo Titulo a inserir
     */
    public static void aplicarTituloPainel(JPanel panel, String titulo) {
        panel.setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                titulo,
                0, 0,
                FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));
    }

    /**
     * Método para padronizar tabelas
     * 
     * @param tabela Tabela a aplicar alterações
     */
    public static void configurarTabela(JTable tabela) {
        tabela.setFont(FONTE_CONSOLE.deriveFont(Font.BOLD, 12f));
        tabela.setRowHeight(26);

        tabela.setShowGrid(false);
        tabela.setIntercellSpacing(new Dimension(0, 0));

        JTableHeader header = tabela.getTableHeader();
        header.setFont(FONTE_CONSOLE.deriveFont(Font.BOLD, 14f));
        header.setOpaque(true);

        // Renderer moderno
        DefaultTableCellRenderer renderer = criarRendererPadrao();

        for (int i = 0; i < tabela.getColumnCount(); i++) {
            tabela.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }
    
    /**
     * 
     * 
     * @return 
     */
    public static DefaultTableCellRenderer criarRendererPadrao() {
        return new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int col) {

                Component c = super.getTableCellRendererComponent(
                        table, value, isSelected, hasFocus, row, col);

                setHorizontalAlignment(CENTER);
                return c;
            }
        };
    }

    /**
     * 
     * 
     * @param scroll 
     */
    public static void configurarScroll(JScrollPane scroll) {
        scroll.setBorder(UIManager.getBorder("ScrollPane.border"));
        scroll.getViewport().setBackground(UIManager.getColor("Panel.background"));
    }

    /**
     * 
     * 
     * @param comp 
     */
    public static void estilizarTextoPainel(JComponent comp) {
        comp.setFont(FONTE_CONSOLE);
        comp.setForeground(new Color(255, 255, 255));
    }

    /**
     * 
     * 
     * @param lista
     * @return 
     */
    public static JsonArray toJsonArray(List<String> lista) {
        JsonArray arr = new JsonArray();
        for (String s : lista) {
            arr.add(s);
        }
        return arr;
    }
}

