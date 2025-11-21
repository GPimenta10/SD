package Utils;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;

/**
 * Utilitário para exibir o menu de seleção de carga com o estilo visual do Dashboard.
 * Gere toda a interação inicial com o utilizador usando botões.
 */
public class MenuCarga {

    // Cores e Fontes idênticas ao Dashboard (FlatLaf One Dark)
    private static final Color BG_COLOR = new Color(40, 42, 54);
    private static final Color FG_COLOR = new Color(220, 220, 220);
    private static final Font FONT_CONSOLAS = new Font("Consolas", Font.PLAIN, 14);
    private static final Font FONT_TITULO = new Font("Consolas", Font.BOLD, 16);

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
        lblTitulo.setFont(FONT_TITULO);
        lblTitulo.setForeground(new Color(97, 175, 239)); // Azul suave
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Descrição
        JTextArea txtDescricao = new JTextArea(
                "\nEscolha a carga para a simulação:\n\n" +
                        " • BAIXA : 30 veículos  (~2.0s intervalo)\n" +
                        " • MEDIA : 100 veículos (~1.5s intervalo)\n" +
                        " • ALTA  : 250 veículos (~0.5s intervalo)\n"
        );
        txtDescricao.setFont(FONT_CONSOLAS);
        txtDescricao.setForeground(FG_COLOR);
        txtDescricao.setBackground(BG_COLOR);
        txtDescricao.setEditable(false);
        txtDescricao.setMargin(new Insets(10, 0, 20, 0)); // Margem extra em baixo

        panel.add(lblTitulo);
        panel.add(txtDescricao);

        // 2. Definir os Botões (Opções)
        String[]opcoes = {"BAIXA", "MEDIA", "ALTA"};

        // 3. Mostrar o Dialog com Botões Personalizados
        // O showOptionDialog cria um botão para cada string no array 'opcoes'
        int escolha = JOptionPane.showOptionDialog(
                null,
                panel,
                "Configuração Inicial",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,     // Sem ícone
                opcoes,   // Os nossos botões
                opcoes[0] // Botão selecionado por defeito
        );

        // 4. Processar a escolha
        // Se escolha for -1, significa que o utilizador fechou a janela no "X"
        if (escolha == -1) {
            System.out.println("Operação cancelada pelo utilizador.");
            System.exit(0);
        }

        String cargaEscolhida = opcoes[escolha];

        // Imprime o cabeçalho na consola
        System.out.println("=".repeat(60));
        System.out.println("A iniciar nova execução do sistema (Carga: " + cargaEscolhida + ")...");
        System.out.println("=".repeat(60));

        return cargaEscolhida;
    }
}