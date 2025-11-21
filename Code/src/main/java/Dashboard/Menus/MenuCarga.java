package Dashboard.Menus;

import Dashboard.Logs.TipoLog;
import Dashboard.Utils.DashboardUIUtils;
import Logging.LogClienteDashboard;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import javax.swing.*;

/**
 * Utilitário para exibir o menu de seleção de carga com o estilo visual do Dashboard.
 * Gere toda a interação inicial com o utilizador usando botões.
 */
public class MenuCarga {

    // Cores do teu Dashboard
    private static final Color BG_COLOR = new Color(40, 42, 54);
    private static final Color FG_COLOR = new Color(220, 220, 220);

    /**
     * Exibe o menu com botões, valida a escolha e devolve a carga selecionada.
     * Se o utilizador fechar a janela, encerra o programa imediatamente.
     */
    public static String obterCargaOuSair() {

        // 1. Criar Painel de Conteúdo (Título e Descrição)
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_COLOR);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Título
        JLabel lblTitulo = new JLabel("Selecione a Intensidade do Tráfego");
        DashboardUIUtils.estilizarTextoPainel(lblTitulo);
        lblTitulo.setFont(DashboardUIUtils.FONTE_TITULO);
        lblTitulo.setForeground(new Color(97, 175, 239)); // Azul suave
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Descrição
        JTextArea txtDescricao = new JTextArea(
                "\nEscolha a carga para a simulação:\n\n" +
                        " • BAIXA : 30 veículos  (~2.0s intervalo)\n" +
                        " • MEDIA : 100 veículos (~1.5s intervalo)\n" +
                        " • ALTA  : 250 veículos (~0.5s intervalo)\n"
        );
        DashboardUIUtils.estilizarTextoPainel(txtDescricao);
        txtDescricao.setForeground(FG_COLOR);
        txtDescricao.setBackground(BG_COLOR);
        txtDescricao.setEditable(false);
        txtDescricao.setMargin(new Insets(10, 0, 20, 0));

        panel.add(lblTitulo);
        panel.add(txtDescricao);

        // 2. Opções de carga
        String[] opcoes = {"BAIXA", "MEDIA", "ALTA"};

        // 3. Mostrar o Dialog
        int escolha = JOptionPane.showOptionDialog(
                null,
                panel,
                "Configuração Inicial",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                opcoes,
                opcoes[0]
        );

        // Se fechar no X
        if (escolha == -1) {
            System.out.println("Cancelado pelo utilizador.");
            System.exit(0);
        }

        String cargaEscolhida = opcoes[escolha];

        LogClienteDashboard.enviar(
                TipoLog.SISTEMA,
                "A iniciar nova execução do sistema (Carga: " + cargaEscolhida + ")..."
        );
        return cargaEscolhida;
    }
}
