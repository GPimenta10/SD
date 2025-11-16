package Dashboard;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Painel que exibe logs importantes do sistema em tempo real.
 * Logs são categorizados por tipo e cor.
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
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 150));

        setBackground(new Color(40, 42, 54));

        setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(98, 114, 164), 2),
                "📋 Eventos Importantes do Sistema",
                0, 0,
                new Font("Arial", Font.BOLD, 14)
        ));
        ((javax.swing.border.TitledBorder) getBorder()).setTitleColor(new Color(248, 248, 242));

        // === JTextPane em vez de JTextArea ===
        areaLogs = new JTextPane();
        areaLogs.setEditable(false);
        areaLogs.setFont(new Font("Consolas", Font.PLAIN, 14));
        areaLogs.setBackground(new Color(40, 42, 54));
        areaLogs.setForeground(new Color(248, 248, 242));

        doc = areaLogs.getStyledDocument();

        // Inicializa estilos de cor
        inicializarEstilos();

        // Scroll
        scrollPane = new JScrollPane(areaLogs);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

        add(scrollPane, BorderLayout.CENTER);

        // Log inicial
        adicionarLog(TipoLog.SISTEMA, "Dashboard iniciado");
    }

    /**
     * Inicializa os estilos de cor para cada tipo de log
     */
    private void inicializarEstilos() {
        // Estilo SISTEMA - Branco
        estiloSistema = areaLogs.addStyle("Sistema", null);
        StyleConstants.setForeground(estiloSistema, new Color(248, 248, 242)); // White

        // Estilo GERADOR - Verde claro
        estiloGerador = areaLogs.addStyle("Gerador", null);
        StyleConstants.setForeground(estiloGerador, new Color(80, 250, 123)); // Green Dracula

        // Estilo VEICULO - Azul claro
        estiloVeiculo = areaLogs.addStyle("Veiculo", null);
        StyleConstants.setForeground(estiloVeiculo, new Color(139, 233, 253)); // Cyan Dracula

        // Estilo AVISO - Amarelo
        estiloAviso = areaLogs.addStyle("Aviso", null);
        StyleConstants.setForeground(estiloAviso, new Color(241, 250, 140)); // Yellow Dracula

        // Estilo CRUZAMENTO - Roxo
        estiloCruzamento = areaLogs.addStyle("Cruzamento", null);
        StyleConstants.setForeground(estiloCruzamento, new Color(189, 147, 249)); // Purple Dracula

        // Estilo FILA - Laranja
        estiloFila = areaLogs.addStyle("Fila", null);
        StyleConstants.setForeground(estiloFila, new Color(255, 184, 108)); // Orange Dracula

        // Estilo SEMAFORO - Rosa
        estiloSemaforo = areaLogs.addStyle("Semaforo", null);
        StyleConstants.setForeground(estiloSemaforo, new Color(255, 121, 198)); // Pink Dracula

        // Estilo ERRO - Vermelho
        estiloErro = areaLogs.addStyle("Erro", null);
        StyleConstants.setForeground(estiloErro, new Color(255, 85, 85)); // Red Dracula

        // Estilo SUCESSO - Verde brilhante
        estiloSucesso = areaLogs.addStyle("Sucesso", null);
        StyleConstants.setForeground(estiloSucesso, new Color(80, 250, 123)); // Green Dracula
    }

    /**
     * Adiciona um log com timestamp automático e cor baseada no tipo
     */
    public void adicionarLog(TipoLog tipo, String mensagem) {
        SwingUtilities.invokeLater(() -> {
            try {
                String timestamp = LocalTime.now().format(TIME_FORMAT);
                String logFormatado = String.format("[%s] %s %s%n", timestamp, tipo.getIcone(), mensagem);

                // Escolhe o estilo baseado no tipo
                Style estilo = switch (tipo) {
                    case SISTEMA -> estiloSistema;
                    case GERADOR -> estiloGerador;
                    case VEICULO -> estiloVeiculo;
                    case AVISO -> estiloAviso;
                    case CRUZAMENTO -> estiloCruzamento;
                    case FILA -> estiloFila;
                    case SEMAFORO -> estiloSemaforo;
                    case ERRO -> estiloErro;
                    case SUCESSO -> estiloSucesso;
                };

                // Adiciona o log com a cor correta
                doc.insertString(doc.getLength(), logFormatado, estilo);
                contadorLogs++;

                // Limita número de logs (opcional)
                if (contadorLogs > MAX_LOGS) {
                    int excesso = doc.getLength() - 10000; // Remove primeiros ~10KB
                    if (excesso > 0) {
                        doc.remove(0, excesso);
                    }
                }

                // Scroll automático para o final
                areaLogs.setCaretPosition(doc.getLength());

            } catch (BadLocationException e) {
                System.err.println("[PainelLogs] Erro ao adicionar log: " + e.getMessage());
            }
        });
    }
}