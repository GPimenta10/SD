package Dashboard.Paineis;

import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Painel que exibe informações sobre os servidores conectados
 * com estilo moderno (FlatLaf OneDark).
 */
public class PainelServidores extends JPanel {

    private final Map<String, JLabel> labelsPorServidor;
    private final JPanel painelConteudo;

    public PainelServidores() {

        setLayout(new BorderLayout());

        // Fundo moderno do tema
        setBackground(UIManager.getColor("Panel.background"));

        // Título moderno
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Servidores Conectados",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        labelsPorServidor = new LinkedHashMap<>();

        // Painel interno (vertical)
        painelConteudo = new JPanel();
        painelConteudo.setLayout(new BoxLayout(painelConteudo, BoxLayout.Y_AXIS));
        painelConteudo.setBackground(UIManager.getColor("Panel.background"));
        painelConteudo.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Scroll moderno
        JScrollPane scroll = new JScrollPane(painelConteudo);
        scroll.setBackground(UIManager.getColor("Panel.background"));
        scroll.getViewport().setBackground(UIManager.getColor("Panel.background"));
        scroll.setBorder(null);

        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        // Modernizar scrollbar
        JScrollBar sb = scroll.getVerticalScrollBar();
        sb.setUnitIncrement(12);
        sb.setBackground(UIManager.getColor("Panel.background"));

        add(scroll, BorderLayout.CENTER);

        adicionarMensagemInicial();
    }

    /**
     * Mensagem inicial quando não há servidores conectados
     */
    private void adicionarMensagemInicial() {
        JLabel labelInicial = new JLabel("Aguardando conexões");
        DashboardUIUtils.estilizarTextoPainel(labelInicial);
        labelInicial.setForeground(UIManager.getColor("Label.disabledForeground"));
        labelInicial.setAlignmentX(Component.LEFT_ALIGNMENT);
        painelConteudo.add(labelInicial);
    }

    /**
     * Adiciona ou atualiza um servidor
     */
    public void adicionarServidor(String nome, String ip, int porta) {
        SwingUtilities.invokeLater(() -> {
            if (labelsPorServidor.isEmpty()) {
                painelConteudo.removeAll();
            }

            String chave = nome;
            String info = String.format("%s:%d", ip, porta);

            JLabel label = labelsPorServidor.get(chave);

            if (label == null) {
                label = criarLabelServidor(nome, info);
                labelsPorServidor.put(chave, label);
                painelConteudo.add(label);
                painelConteudo.add(Box.createRigidArea(new Dimension(0, 6)));
            } else {
                atualizarLabelServidor(label, nome, info);
            }

            painelConteudo.revalidate();
            painelConteudo.repaint();
        });
    }

    /**
     * Cria um label moderno contendo informações do servidor
     */
    private JLabel criarLabelServidor(String nome, String info) {
        String icone = getIconePorNome(nome);
        String texto = icone + "  " + nome + " — " + info;

        JLabel label = new JLabel(texto);

        DashboardUIUtils.estilizarTextoPainel(label);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);

        return label;
    }

    /**
     * Atualiza texto do label
     */
    private void atualizarLabelServidor(JLabel label, String nome, String info) {
        String icone = getIconePorNome(nome);
        label.setText(icone + "  " + nome + " — " + info);
    }

    /**
     * Ícone dependendo do tipo de servidor
     */
    private String getIconePorNome(String nome) {
        String n = nome.toLowerCase();

        if (n.contains("cruzamento") || n.startsWith("cr"))   return "🚦";
        if (n.contains("entrada")    || n.startsWith("e"))    return "🚪";
        if (n.contains("saida")      || n.startsWith("s"))    return "🏁";
        if (n.contains("gerador"))                              return "⚡";
        return "🔌";
    }
}
