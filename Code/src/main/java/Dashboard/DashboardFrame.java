package Dashboard;

import Dashboard.Logs.DashLogger;
import Dashboard.Paineis.*;
import Dashboard.Utils.DashboardUIUtils;
import Dashboard.Utils.MinimalScrollBarUI;

import javax.swing.*;
import java.awt.*;

/**
 * Frame principal do Dashboard de Tráfego.
 * Modernizado para consistência estética com o tema OneDark/Dracula.
 */
public class DashboardFrame extends JFrame {

    // Painéis principais
    private final PainelEstatsGlobais painelEstatsGlobais;
    private final PainelMapa painelMapa;
    private final PainelEstatsSaida painelEstatsSaida;
    private final PainelInfoSaidaVeiculos painelInfoSaidaVeiculos;
    private final PainelLogs painelLogs;
    private final PainelServidores painelServidores;
    private final PainelEstatsCruzamentos painelEstatsCruzamentos;
    private final PainelResumoCruzamento painelResumoCruzamento;

    // Cor base do tema
    private static final Color BG = new Color(40, 42, 54);

    public DashboardFrame() {
        super("Dashboard - Sistema de Tráfego Urbano");

        // ================================
        //           CONFIG FRAME
        // ================================
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(3, 3)); // Reduzido de 10 para 3
        getContentPane().setBackground(BG);

        // ================================
        //        INSTANCIAR PAINÉIS
        // ================================
        painelEstatsGlobais = new PainelEstatsGlobais();
        painelMapa = new PainelMapa();
        painelEstatsSaida = new PainelEstatsSaida();
        painelInfoSaidaVeiculos = new PainelInfoSaidaVeiculos();
        painelLogs = new PainelLogs();
        painelServidores = new PainelServidores();
        painelEstatsCruzamentos = new PainelEstatsCruzamentos();
        painelResumoCruzamento = new PainelResumoCruzamento();

        painelMapa.setDashboard(this);
        DashLogger.inicializar(painelLogs);

        // ================================
        //        COLUNA ESQUERDA
        // ================================
        painelEstatsSaida.setPreferredSize(new Dimension(0, 200));
        painelResumoCruzamento.setPreferredSize(new Dimension(0, 200));

        JPanel colunaEsquerda = criarColuna(350);
        colunaEsquerda.add(painelResumoCruzamento, BorderLayout.NORTH);
        colunaEsquerda.add(painelEstatsSaida, BorderLayout.CENTER);

        // ================================
        //        COLUNA CENTRAL
        // ================================
        painelMapa.setPreferredSize(new Dimension(400, 500));
        painelEstatsGlobais.setPreferredSize(new Dimension(0, 200));

        JPanel colunaCentro = criarColuna(-1);
        colunaCentro.add(painelMapa, BorderLayout.CENTER);
        colunaCentro.add(painelEstatsGlobais, BorderLayout.SOUTH);

        // ================================
        //        COLUNA DIREITA
        // ================================
        JScrollPane scrollCruzEstats = criarScroll(painelEstatsCruzamentos);
        painelInfoSaidaVeiculos.setPreferredSize(new Dimension(0, 300));

        JPanel colunaDireita = criarColuna(600);
        colunaDireita.add(painelInfoSaidaVeiculos, BorderLayout.NORTH);
        colunaDireita.add(scrollCruzEstats, BorderLayout.CENTER);

        // ================================
        //       PAINEL INFERIOR
        // ================================
        painelLogs.setPreferredSize(new Dimension(700, 180));
        painelServidores.setPreferredSize(new Dimension(500, 180));

        JPanel painelInferior = new JPanel();
        painelInferior.setLayout(new BoxLayout(painelInferior, BoxLayout.X_AXIS));
        painelInferior.setPreferredSize(new Dimension(0, 140));
        painelInferior.setBackground(BG);

        painelInferior.add(Box.createRigidArea(new Dimension(10, 0)));
        painelInferior.add(painelLogs);
        painelInferior.add(painelServidores);

        // ================================
        //     ADICIONAR AO FRAME
        // ================================
        add(criarComMargem(colunaEsquerda, 10, 3), BorderLayout.WEST);      // Borda esq=10, dir=3
        add(criarComMargem(colunaCentro, 3, 3), BorderLayout.CENTER);       // Ambos lados=3
        add(criarComMargem(colunaDireita, 3, 10), BorderLayout.EAST);       // Borda esq=3, dir=10
        add(criarComMargem(painelInferior, 10, 10), BorderLayout.SOUTH);    // Bordas normais

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ==================================================
    //               MÉTODOS AUXILIARES
    // ==================================================

    /**
     * Cria um JScrollPane personalizado com barras minimalistas.
     */
    private JScrollPane criarScroll(JComponent componente) {
        JScrollPane scroll = new JScrollPane(componente);

        DashboardUIUtils.configurarScroll(scroll);

        scroll.getVerticalScrollBar().setUI(new MinimalScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new MinimalScrollBarUI());

        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scroll;
    }

    /**
     * Cria uma coluna estilizada do dashboard.
     */
    private JPanel criarColuna(int largura) {
        JPanel coluna = new JPanel(new BorderLayout(0, 10));
        coluna.setBackground(BG);

        if (largura > 0) {
            coluna.setPreferredSize(new Dimension(largura, 0));
        }

        return coluna;
    }

    /**
     * Cria wrapper com margens personalizadas (topo, esquerda/direita variável, baixo).
     *
     * @param comp Componente a envolver
     * @param margemEsq Margem esquerda
     * @param margemDir Margem direita
     * @return JPanel com margens aplicadas
     */
    private JComponent criarComMargem(JComponent comp, int margemEsq, int margemDir) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);

        // Margens: topo=10, esquerda=variável, baixo=10, direita=variável
        wrapper.setBorder(BorderFactory.createEmptyBorder(10, margemEsq, 10, margemDir));
        wrapper.add(comp, BorderLayout.CENTER);

        return wrapper;
    }

    // ==================================================
    //                     GETTERS
    // ==================================================
    public PainelEstatsGlobais getPainelEstatisticas() { return painelEstatsGlobais; }
    public PainelMapa getPainelMapa() { return painelMapa; }
    public PainelEstatsSaida getPainelEstatisticasTipo() { return painelEstatsSaida; }
    public PainelInfoSaidaVeiculos getPainelVeiculos() { return painelInfoSaidaVeiculos; }
    public PainelServidores getPainelServidores() { return painelServidores; }
    public PainelEstatsCruzamentos getPainelEstatisticasCruzamentos() { return painelEstatsCruzamentos; }
    public PainelResumoCruzamento getPainelResumoCruzamento() { return painelResumoCruzamento; }
}