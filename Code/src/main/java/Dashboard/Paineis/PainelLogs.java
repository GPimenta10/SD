package Dashboard.Paineis;

import Dashboard.Logs.TipoLog;
import Dashboard.Utils.DashboardUIUtils;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Painel que exibe logs importantes do sistema, modernizado para FlatLaf OneDark.
 */
public class PainelLogs extends JPanel {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final int MAX_LOGS = 100;

    private final JTextPane areaLogs;
    private final JScrollPane scrollPane;
    private final StyledDocument doc;
    private int contadorLogs = 0;

    // Estilos para cada tipo de log
    private Style estiloSistema;
    private Style estiloGerador;
    private Style estiloVeiculo;
    private Style estiloAviso;
    private Style estiloCruzamento;
    private Style estiloFila;
    private Style estiloSemaforo;
    private Style estiloErro;
    private Style estiloSucesso;

    public PainelLogs() {
        // Layout moderno
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 150));

        // Fundo herdado do tema OneDark
        setBackground(UIManager.getColor("Panel.background"));

        // Título com border moderna do FlatLaf
        setBorder(BorderFactory.createTitledBorder(
                UIManager.getBorder("TitledBorder.border"),
                "Eventos Importantes do Sistema",
                0, 0,
                DashboardUIUtils.FONTE_TITULO,
                UIManager.getColor("Label.foreground")
        ));

        // === JTextPane moderno ===
        areaLogs = new JTextPane();
        areaLogs.setEditable(false);
        DashboardUIUtils.estilizarTextoPainel(areaLogs);

        // fundo e cor de texto integrados com o tema
        areaLogs.setBackground(UIManager.getColor("EditorPane.background"));
        areaLogs.setForeground(UIManager.getColor("Label.foreground"));

        doc = areaLogs.getStyledDocument();

        // Inicializar estilos
        inicializarEstilos();

        // Scroll moderno
        scrollPane = new JScrollPane(areaLogs);
        scrollPane.setBackground(UIManager.getColor("Panel.background"));
        scrollPane.setBorder(UIManager.getBorder("ScrollPane.border"));

        add(scrollPane, BorderLayout.CENTER);

        // Log inicial
        adicionarLog(TipoLog.SISTEMA, "Dashboard iniciado");
    }

    /**
     * Estilos de cor dos logs (cores Dracula / IntelliJ)
     */
    private void inicializarEstilos() {

        estiloSistema     = criarEstilo(new Color(248, 248, 242));  // Branco
        estiloGerador     = criarEstilo(new Color(80, 250, 123));   // Verde claro
        estiloVeiculo     = criarEstilo(new Color(139, 233, 253));  // Azul claro
        estiloAviso       = criarEstilo(new Color(241, 250, 140));  // Amarelo
        estiloCruzamento  = criarEstilo(new Color(189, 147, 249));  // Roxo
        estiloFila        = criarEstilo(new Color(255, 184, 108));  // Laranja
        estiloSemaforo    = criarEstilo(new Color(255, 121, 198));  // Rosa
        estiloErro        = criarEstilo(new Color(255, 85, 85));    // Vermelho
        estiloSucesso     = criarEstilo(new Color(80, 250, 123));   // Verde
    }

    /**
     * Cria um estilo de cor
     */
    private Style criarEstilo(Color cor) {
        Style s = areaLogs.addStyle(null, null);
        StyleConstants.setForeground(s, cor);
        return s;
    }

    /**
     * Adiciona um log com timestamp e cor baseada no tipo
     */
    public void adicionarLog(TipoLog tipo, String mensagem) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = LocalTime.now().format(TIME_FORMAT);
                String logFormatado = String.format("[%s] %s %s%n", timestamp, tipo.getIcone(), mensagem);

                Style estilo = switch (tipo) {
                    case SISTEMA     -> estiloSistema;
                    case GERADOR     -> estiloGerador;
                    case VEICULO     -> estiloVeiculo;
                    case AVISO       -> estiloAviso;
                    case CRUZAMENTO  -> estiloCruzamento;
                    case FILA        -> estiloFila;
                    case SEMAFORO    -> estiloSemaforo;
                    case ERRO        -> estiloErro;
                    case SUCESSO     -> estiloSucesso;
                };

                doc.insertString(doc.getLength(), logFormatado, estilo);

                contadorLogs++;
            } catch (BadLocationException e) {
                System.err.println("[PainelLogs] Erro ao adicionar log: " + e.getMessage());
            }
        });
    }
}