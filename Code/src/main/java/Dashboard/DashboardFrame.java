package Dashboard;

import Dashboard.Logs.DashLogger;
import Dashboard.Paineis.*;
import Dashboard.Utils.DashboardUIUtils;
import Dashboard.Utils.MinimalScrollBarUI;

// Novos imports devido à refatorização do pacote Estatisticas
import Dashboard.Estatisticas.GestorEstatisticas;
import Dashboard.Estatisticas.ReceiverEstatisticas;
import Dashboard.Estatisticas.EstatisticasGlobais;
import Dashboard.Estatisticas.EstatisticasFila;
import Dashboard.Estatisticas.EstatisticasSaida;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

public class DashboardFrame extends JFrame implements ReceiverEstatisticas {

    private final GestorEstatisticas gestor;

    private final PainelEstatsGlobais painelEstatsGlobais;
    private final PainelMapa painelMapa;
    private final PainelEstatsSaida painelEstatsSaida;
    private final PainelInfoSaidaVeiculos painelInfoSaidaVeiculos;
    private final PainelLogs painelLogs;
    private final PainelServidores painelServidores;
    private final PainelEstatsCruzamentos painelEstatsCruzamentos;
    private final PainelResumoCruzamento painelResumoCruzamento;

    private static final Color BG = new Color(40, 42, 54);

    public DashboardFrame(GestorEstatisticas gestor) {
        super("Dashboard - Sistema de Tráfego Urbano");
        this.gestor = gestor;

        // Registar este frame como listener do gestor
        gestor.adicionarOuvinte(this);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLayout(new BorderLayout(3, 3));
        getContentPane().setBackground(BG);

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

        // Coluna esquerda
        painelEstatsSaida.setPreferredSize(new Dimension(0, 200));
        painelResumoCruzamento.setPreferredSize(new Dimension(0, 200));

        JPanel colunaEsquerda = criarColuna(350);
        colunaEsquerda.add(painelResumoCruzamento, BorderLayout.NORTH);
        colunaEsquerda.add(painelEstatsSaida, BorderLayout.CENTER);

        // Coluna centro
        painelMapa.setPreferredSize(new Dimension(400, 500));
        painelEstatsGlobais.setPreferredSize(new Dimension(0, 125));

        JPanel colunaCentro = criarColuna(-1);
        colunaCentro.add(painelMapa, BorderLayout.CENTER);
        colunaCentro.add(painelEstatsGlobais, BorderLayout.SOUTH);

        // Coluna direita
        JScrollPane scrollCruzEstats = criarScroll(painelEstatsCruzamentos);
        painelInfoSaidaVeiculos.setPreferredSize(new Dimension(0, 300));

        JPanel colunaDireita = criarColuna(600);
        colunaDireita.add(painelInfoSaidaVeiculos, BorderLayout.NORTH);
        colunaDireita.add(scrollCruzEstats, BorderLayout.CENTER);

        // Painel inferior
        painelLogs.setPreferredSize(new Dimension(700, 180));
        painelServidores.setPreferredSize(new Dimension(500, 180));

        JPanel painelInferior = new JPanel();
        painelInferior.setLayout(new BoxLayout(painelInferior, BoxLayout.X_AXIS));
        painelInferior.setPreferredSize(new Dimension(0, 140));
        painelInferior.setBackground(BG);

        painelInferior.add(Box.createRigidArea(new Dimension(10, 0)));
        painelInferior.add(painelLogs);
        painelInferior.add(painelServidores);

        add(criarComMargem(colunaEsquerda, 10, 3), BorderLayout.WEST);
        add(criarComMargem(colunaCentro, 3, 3), BorderLayout.CENTER);
        add(criarComMargem(colunaDireita, 3, 10), BorderLayout.EAST);
        add(criarComMargem(painelInferior, 10, 10), BorderLayout.SOUTH);

        setLocationRelativeTo(null);
        setVisible(true);
    }

    // ==================================================
    // CALLBACKS DO GESTOR (Tipos Atualizados)
    // ==================================================

    @Override
    public void onEstatisticasGlobaisAtualizadas(EstatisticasGlobais globais) {
        SwingUtilities.invokeLater(() -> painelEstatsGlobais.atualizar(globais));
    }

    @Override
    public void onEstatisticasCruzamentoAtualizadas(String cruzamento,
                                                    Map<String, EstatisticasFila> filas) {
        SwingUtilities.invokeLater(() ->
                painelEstatsCruzamentos.atualizarCruzamento(cruzamento, filas)
        );
    }

    @Override
    public void onEstatisticasSaidaAtualizadas(Map<String, EstatisticasSaida> saidas) {
        SwingUtilities.invokeLater(() -> painelEstatsSaida.atualizar(saidas));
    }

    @Override
    public void onResumoCruzamentosAtualizado(Map<String, Map<String, Integer>> resumo) {
        SwingUtilities.invokeLater(() -> painelResumoCruzamento.atualizar(resumo));
    }

    // ==================================================
    // HELPERS DE LAYOUT
    // ==================================================

    private JScrollPane criarScroll(JComponent componente) {
        JScrollPane scroll = new JScrollPane(componente);

        DashboardUIUtils.configurarScroll(scroll);
        scroll.getVerticalScrollBar().setUI(new MinimalScrollBarUI());
        scroll.getHorizontalScrollBar().setUI(new MinimalScrollBarUI());

        scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        return scroll;
    }

    private JPanel criarColuna(int largura) {
        JPanel coluna = new JPanel(new BorderLayout(0, 10));
        coluna.setBackground(BG);

        if (largura > 0) {
            coluna.setPreferredSize(new Dimension(largura, 0));
        }
        return coluna;
    }

    private JComponent criarComMargem(JComponent comp, int margemEsq, int margemDir) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setBackground(BG);

        wrapper.setBorder(BorderFactory.createEmptyBorder(10, margemEsq, 10, margemDir));
        wrapper.add(comp, BorderLayout.CENTER);

        return wrapper;
    }

    // ==================================================
    // GETTERS
    // ==================================================
    public PainelEstatsGlobais getPainelEstatisticas() { return painelEstatsGlobais; }
    public PainelMapa getPainelMapa() { return painelMapa; }
    public PainelEstatsSaida getPainelEstatisticasTipo() { return painelEstatsSaida; }
    public PainelInfoSaidaVeiculos getPainelVeiculos() { return painelInfoSaidaVeiculos; }
    public PainelServidores getPainelServidores() { return painelServidores; }
    public PainelEstatsCruzamentos getPainelEstatisticasCruzamentos() { return painelEstatsCruzamentos; }
    public PainelResumoCruzamento getPainelResumoCruzamento() { return painelResumoCruzamento; }
}