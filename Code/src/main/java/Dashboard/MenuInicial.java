package Dashboard;

import Dashboard.Utils.DashboardUIUtils;
import PontosEntrada.TipoCenario;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import javax.swing.*;
import java.awt.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * Menu inicial do sistema que permite selecionar a carga e o cenário
 * antes de iniciar a simulação.
 *
 */
public class MenuInicial {
    // Constantes de configuração
    private static final String FICHEIRO_CONFIG = "configCargas.json";
    private static final String CHAVE_CARGAS = "cargas";
    private static final String CHAVE_VEICULOS = "totalVeiculos";
    private static final String CHAVE_INTERVALO = "intervaloMs";

    // Constantes de UI
    private static final int LARGURA_PAINEL = 350;
    private static final int ALTURA_PAINEL = 340;
    private static final int LARGURA_COMBO = 250;
    private static final int ALTURA_COMBO = 30;
    private static final int ESPACAMENTO_PEQUENO = 5;
    private static final int ESPACAMENTO_MEDIO = 15;
    private static final int ESPACAMENTO_GRANDE = 20;
    private static final float TAMANHO_FONTE_TITULO = 18f;
    private static final float TAMANHO_FONTE_DESCRICAO = 11f;
    private static final float TAMANHO_FONTE_SECAO = 13f;

    // Constantes de texto
    private static final String TITULO_JANELA = "Configuração Inicial - Sistema de Tráfego";
    private static final String TITULO_PRINCIPAL = "Sistema de Tráfego Urbano";
    private static final String LABEL_CARGA = "Intensidade do Tráfego:";
    private static final String LABEL_CENARIO = "Cenário de Caminhos:";
    private static final String BTN_INICIAR = "Iniciar";
    private static final String BTN_CANCELAR = "Cancelar";

    private static final Gson gson = new Gson();
    private static JsonObject configCargas;

    /**
     * Exibe o menu inicial e retorna as configurações selecionadas.
     * Se o utilizador fechar a janela ou cancelar, encerra o programa.
     *
     * @return Array com [nomeCarga, nomeCenario]
     */
    public static String[] obterConfiguracoes() {
        carregarConfiguracoes();

        String[] cargas = obterNomesCargas();

        JComboBox<String> comboCarga = criarComboBox(cargas);
        JComboBox<TipoCenario> comboCenario = criarComboBox(TipoCenario.values());

        JPanel panel = montarPainel(cargas, comboCarga, comboCenario);

        int resultado = mostrarDialogo(panel);

        if (resultado != 0) {
            System.out.println("Operação cancelada pelo utilizador.");
            System.exit(0);
        }

        String carga = (String) comboCarga.getSelectedItem();
        TipoCenario cenario = (TipoCenario) comboCenario.getSelectedItem();

        imprimirConfiguracoes(carga, cenario);

        return new String[]{carga, cenario.name()};
    }

    // ========================================================================
    // Carregamento de Configurações
    // ========================================================================

    private static void carregarConfiguracoes() {
        try (InputStream is = MenuInicial.class.getClassLoader().getResourceAsStream(FICHEIRO_CONFIG)) {
            if (is == null) {
                throw new RuntimeException("Ficheiro " + FICHEIRO_CONFIG + " não encontrado");
            }
            configCargas = gson.fromJson(new InputStreamReader(is, StandardCharsets.UTF_8), JsonObject.class);
        } catch (Exception e) {
            System.err.println("Erro ao carregar configurações: " + e.getMessage());
            System.exit(1);
        }
    }

    private static String[] obterNomesCargas() {
        Set<String> keys = configCargas.getAsJsonObject(CHAVE_CARGAS).keySet();
        return keys.toArray(new String[0]);
    }

    private static JsonObject obterInfoCarga(String nome) {
        return configCargas.getAsJsonObject(CHAVE_CARGAS).getAsJsonObject(nome);
    }

    // ========================================================================
    // Construção do Painel
    // ========================================================================

    private static JPanel montarPainel(String[] cargas, JComboBox<String> comboCarga,
                                       JComboBox<TipoCenario> comboCenario) {
        JPanel panel = criarPainelPrincipal();

        panel.add(criarTitulo());
        panel.add(criarEspacamento(ESPACAMENTO_MEDIO));
        panel.add(criarDescricao(cargas));
        panel.add(criarEspacamento(ESPACAMENTO_GRANDE));
        panel.add(criarLabelSecao(LABEL_CARGA));
        panel.add(criarEspacamento(ESPACAMENTO_PEQUENO));
        panel.add(comboCarga);
        panel.add(criarEspacamento(ESPACAMENTO_MEDIO));
        panel.add(criarLabelSecao(LABEL_CENARIO));
        panel.add(criarEspacamento(ESPACAMENTO_PEQUENO));
        panel.add(comboCenario);

        return panel;
    }

    private static JPanel criarPainelPrincipal() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(UIManager.getColor("Panel.background"));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 25, 15, 25));
        panel.setPreferredSize(new Dimension(LARGURA_PAINEL, ALTURA_PAINEL));
        return panel;
    }

    private static Component criarEspacamento(int altura) {
        return Box.createRigidArea(new Dimension(0, altura));
    }

    // ========================================================================
    // Componentes de UI
    // ========================================================================

    private static JLabel criarTitulo() {
        JLabel titulo = new JLabel(TITULO_PRINCIPAL);
        titulo.setFont(DashboardUIUtils.FONTE_TITULO.deriveFont(TAMANHO_FONTE_TITULO));
        titulo.setForeground(UIManager.getColor("Component.accentColor"));
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        return titulo;
    }

    private static JLabel criarLabelSecao(String texto) {
        JLabel label = new JLabel(texto);
        label.setFont(DashboardUIUtils.FONTE_CONSOLE.deriveFont(Font.BOLD, TAMANHO_FONTE_SECAO));
        label.setForeground(UIManager.getColor("Label.foreground"));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        return label;
    }

    private static JTextArea criarDescricao(String[] cargas) {
        String texto = construirTextoDescricao(cargas);

        JTextArea desc = new JTextArea(texto);
        desc.setFont(DashboardUIUtils.FONTE_CONSOLE.deriveFont(TAMANHO_FONTE_DESCRICAO));
        desc.setForeground(UIManager.getColor("Label.foreground"));
        desc.setBackground(UIManager.getColor("Panel.background"));
        desc.setEditable(false);
        desc.setAlignmentX(Component.CENTER_ALIGNMENT);
        return desc;
    }

    private static <T> JComboBox<T> criarComboBox(T[] opcoes) {
        JComboBox<T> combo = new JComboBox<>(opcoes);
        combo.setFont(DashboardUIUtils.FONTE_CONSOLE);
        combo.setMaximumSize(new Dimension(LARGURA_COMBO, ALTURA_COMBO));
        combo.setAlignmentX(Component.CENTER_ALIGNMENT);
        return combo;
    }

    // ========================================================================
    // Construção de Texto
    // ========================================================================

    private static String construirTextoDescricao(String[] cargas) {
        StringBuilder sb = new StringBuilder("Cargas disponíveis:\n");

        for (String carga : cargas) {
            sb.append(formatarInfoCarga(carga));
        }

        sb.append("\nCenários disponíveis:\n");
        for (TipoCenario cenario : TipoCenario.values()) {
            sb.append(" • ").append(cenario.getDescricao()).append("\n");
        }

        return sb.toString().trim();
    }

    private static String formatarInfoCarga(String nome) {
        JsonObject info = obterInfoCarga(nome);
        int veiculos = info.get(CHAVE_VEICULOS).getAsInt();
        double intervaloSeg = info.get(CHAVE_INTERVALO).getAsLong() / 1000.0;

        return String.format(" • %s: %d veículos (~%.1fs intervalo)\n", nome, veiculos, intervaloSeg);
    }

    // ========================================================================
    // Diálogo e Output
    // ========================================================================

    private static int mostrarDialogo(JPanel panel) {
        String[] opcoes = {BTN_INICIAR, BTN_CANCELAR};

        return JOptionPane.showOptionDialog(
                null,
                panel,
                TITULO_JANELA,
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                opcoes,
                opcoes[0]
        );
    }

    private static void imprimirConfiguracoes(String carga, TipoCenario cenario) {
        System.out.println("=".repeat(60));
        System.out.println("Configuração selecionada:");
        System.out.println("  Carga: " + carga);
        System.out.println("  Cenário: " + cenario.getDescricao());
        System.out.println("=".repeat(60));
    }
}