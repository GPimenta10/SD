package PontosEntrada;

import Dashboard.ComunicadorDashboard;
import Veiculo.Veiculo;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * Processo principal que gere a criação, envio e registo de veículos.
 *
 * ALTERAÇÃO: Usa ObjectOutputStream para enviar veículos serializados.
 */
public class GeradorVeiculos {

    public enum ModoGeracao { FIXO, POISSON }

    private final PontoEntrada pontoEntrada;
    private final String hostDestino;
    private final int portaDestino;
    private final ModoGeracao modo;
    private final int numeroVeiculos;
    private volatile boolean executando = true;

    // Flags de controlo
    private static final boolean ATIVAR_E1 = false;
    private static final boolean ATIVAR_E2 = false;
    private static final boolean ATIVAR_E3 = true;

    public GeradorVeiculos(PontoEntrada pontoEntrada, String hostDestino, int portaDestino,
                           ModoGeracao modo, int numeroVeiculos) {
        this.pontoEntrada = pontoEntrada;
        this.hostDestino = hostDestino;
        this.portaDestino = portaDestino;
        this.modo = modo;
        this.numeroVeiculos = numeroVeiculos;
    }

    public void iniciar() {
        if (!entradaAtiva(pontoEntrada)) {
            System.out.printf("[Gerador-%s] Ignorado (não ativo nesta simulação)%n", pontoEntrada);
            return;
        }

        System.out.printf("[Gerador-%s] Iniciando geração (%s, %d veículos)%n",
                pontoEntrada, modo, numeroVeiculos);

        CriarVeiculos fabrica = new CriarVeiculos(pontoEntrada);
        Temporizador temporizador = new Temporizador(modo);
        DistribuidorVeiculos distribuidor = new DistribuidorVeiculos(hostDestino, portaDestino);
        ComunicadorDashboard dashboard = ComunicadorDashboard.getInstance();

        try (Socket socket = new Socket(hostDestino, portaDestino);
             ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream())) {

            System.out.printf("[Gerador-%s] Conectado a %s:%d [MODO SERIALIZADO]%n",
                    pontoEntrada, hostDestino, portaDestino);

            for (int i = 0; i < numeroVeiculos && executando; i++) {
                Veiculo veiculo = fabrica.criarVeiculo();

                // Envia objeto serializado
                distribuidor.enviar(oos, veiculo);

                // Envia ao dashboard com ID do veículo
                dashboard.enviar(String.format("[Entrada] %s tipo=%s id=%s",
                        pontoEntrada, veiculo.getTipo(), veiculo.getId()));

                Thread.sleep(temporizador.proximoIntervalo());
            }

            System.out.printf("[Gerador-%s] Geração concluída.%n", pontoEntrada);

        } catch (Exception e) {
            System.err.printf("[Gerador-%s] Erro: %s%n", pontoEntrada, e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean entradaAtiva(PontoEntrada entrada) {
        return (entrada == PontoEntrada.E1 && ATIVAR_E1)
                || (entrada == PontoEntrada.E2 && ATIVAR_E2)
                || (entrada == PontoEntrada.E3 && ATIVAR_E3);
    }

    public void parar() { executando = false; }

    public static void main(String[] args) {
        if (args.length < 5) {
            System.err.println("Uso: java PontosEntrada.GeradorVeiculos <pontoEntrada> <porta> <modo> <parametro> <numVeiculos>");
            System.exit(1);
        }

        PontoEntrada entrada = PontoEntrada.valueOf(args[0]);
        int porta = Integer.parseInt(args[1]);
        ModoGeracao modo = ModoGeracao.valueOf(args[2]);
        int parametro = (modo == ModoGeracao.FIXO)
                ? Integer.parseInt(args[3])
                : (int) Double.parseDouble(args[3]);
        int numVeiculos = Integer.parseInt(args[4]);

        GeradorVeiculos gerador = new GeradorVeiculos(entrada, "localhost", porta, modo, numVeiculos);
        Runtime.getRuntime().addShutdownHook(new Thread(gerador::parar));
        gerador.iniciar();
    }
}