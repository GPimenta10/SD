package Cruzamento;

public class Cruzamento3 extends CruzamentoBase {

    // 🔧 Configurações fixas (podem ser centralizadas depois num Config.java)
    private static final int PORTA_ESCUTA = 6003;
    private static final String HOST_SAIDA = "127.0.0.1";
    private static final int PORTA_SAIDA = 7000;
    private static final String HOST_DASHBOARD = "127.0.0.1";
    private static final int PORTA_DASHBOARD = 9000;

    public Cruzamento3() {
        super("Cruzamento3", PORTA_ESCUTA, HOST_SAIDA, PORTA_SAIDA, HOST_DASHBOARD, PORTA_DASHBOARD);
    }

    @Override
    protected void inicializarSemaforos() {
        Object sincronizador = new Object();

        // Duas entradas: E3 e E2
        Semaforo semE3 = new Semaforo("Cr3-E3", 5000, 1500, sincronizador, true, this);
        Semaforo semE2 = new Semaforo("Cr3-E2", 5000, 1500, sincronizador, false, this);

        semE3.setOutroSemaforo(semE2);
        semE2.setOutroSemaforo(semE3);

        semE3.start();
        semE2.start();

        this.semaforos = new Semaforo[]{ semE3, semE2 };
        log("✓ Semáforos iniciados");
    }

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════╗");
        System.out.println("║      CRUZAMENTO 3 - INICIANDO      ║");
        System.out.println("╚════════════════════════════════════╝");
        System.out.println("Escuta: porta " + PORTA_ESCUTA);
        System.out.println("Saída: " + HOST_SAIDA + ":" + PORTA_SAIDA);
        System.out.println("Dashboard: " + HOST_DASHBOARD + ":" + PORTA_DASHBOARD);
        System.out.println();

        Cruzamento3 cruzamento = new Cruzamento3();
        cruzamento.iniciar();
    }
}
