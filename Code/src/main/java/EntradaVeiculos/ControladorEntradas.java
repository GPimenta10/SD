package EntradaVeiculos;

import Veiculo.TipoVeiculo;
import Veiculo.Veiculo;
import java.util.Random;

public class ControladorEntradas {

    //private final E1 e1;
    private final E2 e2;
    private final E3 e3;
    private final int totalVeiculos;
    private final int intervaloMs;
    private final Random random = new Random();

    public ControladorEntradas(String host, int totalVeiculos, int intervaloMs) {
        this.totalVeiculos = totalVeiculos;
        this.intervaloMs = intervaloMs;

        //this.e1 = new E1(host);
        this.e2 = new E2(host);
        this.e3 = new E3(host);
    }

    public void iniciar() throws InterruptedException {
        System.out.println("╔════════════════════════════════════════════╗");
        System.out.println("║  CONTROLADOR DE ENTRADAS - INICIANDO       ║");
        System.out.println("╚════════════════════════════════════════════╝");
        System.out.println("Total de veículos: " + totalVeiculos);
        System.out.println("Intervalo: " + intervaloMs + "ms");
        System.out.println("──────────────────────────────────────────────");

        for (int i = 1; i <= totalVeiculos; i++) {
            EntradaBase entrada = escolherEntrada();
            Veiculo v = gerarVeiculo(i, entrada);
            entrada.enviarVeiculo(v);
            Thread.sleep(intervaloMs);
        }

        System.out.println("✓ Todos os veículos foram gerados e enviados.");
    }

    public static void main(String[] args) throws InterruptedException {
        String host = "127.0.0.1";
        int totalVeiculos = 50;
        int intervalo = 500;

        // permite passar argumentos pela linha de comando, como já fazes nos outros processos
        if (args.length >= 3) {
            host = args[0];
            totalVeiculos = Integer.parseInt(args[1]);
            intervalo = Integer.parseInt(args[2]);
        }

        ControladorEntradas controlador = new ControladorEntradas(host, totalVeiculos, intervalo);
        controlador.iniciar();
    }

    private EntradaBase escolherEntrada() {
        int escolha = random.nextInt(3);
        return switch (escolha) {
            case 0 -> e2;
            default -> e3;
        };
    }

    private Veiculo gerarVeiculo(int sequencia, EntradaBase entrada) {
        TipoVeiculo tipo = escolherTipo();
        String id = String.format("%s-%03d", entrada.nome, sequencia);
        return new Veiculo(id, tipo, entrada.nome, entrada.percurso);
    }

    private TipoVeiculo escolherTipo() {
        int rand = random.nextInt(100);
        if (rand < 30) return TipoVeiculo.MOTA;
        if (rand < 80) return TipoVeiculo.CARRO;
        return TipoVeiculo.CAMIAO;
    }
}
