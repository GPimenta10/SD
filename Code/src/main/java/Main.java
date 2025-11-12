import Cruzamentos.Cruzamento;
import Dashboard.DashboardFrame;
import Dashboard.ThreadServidorDashboard;
import PontosEntrada.GeradorVeiculos;
import PontosEntrada.PontoEntrada;
import Saida.Saida;

/**
 * Orquestrador completo da Simulação de Tráfego Urbano + Dashboard.
 * Inicia o Dashboard, Saída, Cruzamentos e Geradores de veículos.
 * Neste teste: E3 -> Cr3 -> Saída
 */
public class Main {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("TESTE: E3 -> Cr3 -> S + Dashboard integrado");
        System.out.println("=".repeat(60));

        // === 0. Iniciar o DASHBOARD ===
        System.out.println("[SISTEMA] A iniciar Dashboard...");
        DashboardFrame frame = new DashboardFrame();
        ThreadServidorDashboard servidorDashboard = new ThreadServidorDashboard(6000, frame);
        servidorDashboard.start();

        // Mostra o painel gráfico
        javax.swing.SwingUtilities.invokeLater(() -> frame.setVisible(true));

        // === 1. Iniciar o processo de SAÍDA ===
        Saida saida = new Saida(5999, "localhost", 6000);
        saida.iniciar();

        // === 2. Criar o Cruzamento 3 ===
        Cruzamento cr3 = new Cruzamento("Cr3", 5003, "localhost", 6000);

        // === 3. Adicionar ligação E3 -> Cr3 -> S ===
        cr3.adicionarLigacao("E3", "S", "localhost", 5999);

        // === 4. Iniciar o Cruzamento ===
        cr3.iniciar();

        System.out.println("\n[SISTEMA] Cruzamento Cr3 configurado e iniciado!");
        System.out.println("[SISTEMA] Cr3 tem " + cr3.getListaSemaforos().size() + " semáforo(s)");
        System.out.println("[SISTEMA] Aguardando veículos de E3...\n");

        // === 5. Iniciar o Gerador de Veículos (E3) ===
        GeradorVeiculos geradorE3 = new GeradorVeiculos(PontoEntrada.E3, "localhost", 5003, 3000);
        geradorE3.start();

        System.out.println("[SISTEMA] Gerador de veículos E3 iniciado!");
        System.out.println("[SISTEMA] Simulação em execução...\n");

        // === 6. Manter o sistema ativo por 30 segundos ===
        try {
            Thread.sleep(30000); // duração da simulação
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // === 7. Encerrar o sistema ===
        System.out.println("\n[SISTEMA] Parando simulação...");
        geradorE3.parar();
        cr3.parar();
        saida.parar();
        servidorDashboard.parar();
        System.out.println("[SISTEMA] Simulação encerrada.");
    }
}
