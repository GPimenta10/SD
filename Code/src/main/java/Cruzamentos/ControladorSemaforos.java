package Cruzamentos;

import Dashboard.ComunicadorDashboard;
import java.util.*;

/**
 * Thread que alterna o verde entre semáforos de forma mutuamente exclusiva.
 *
 */
public class ControladorSemaforos extends Thread {

    private final String id;
    private final Map<String, Semaforo> semaforos;
    private final ComunicadorDashboard dashboard;
    private final long tempoVerde;
    private final long tempoAllRed;

    /**
     * Construtor do controlador de semáforos de um cruzamento.
     *
     * Cria uma thread responsável por gerir o ciclo dos semáforos
     * (abrir e fechar alternadamente, com intervalos de segurança).
     *
     * Cada controlador atua de forma independente para o cruzamento associado
     * e comunica com o Dashboard sempre que há mudança de estado.
     *
     * @param id           identificador do cruzamento (ex.: "Cr3")
     * @param semaforos    Mapa que associa o nome da direção ao objeto Semaforo correspondente
     * @param dashboard    Instância do comunicador responsável por enviar estados ao Dashboard
     * @param tempoVerde   Duração, em milissegundos, durante a qual o semáforo permanece verde
     * @param tempoAllRed  Tempo de segurança (todos vermelhos) entre trocas de estado
     */
    public ControladorSemaforos(String id, Map<String, Semaforo> semaforos, ComunicadorDashboard dashboard, long tempoVerde, long tempoAllRed) {
        super(id + "_Controlador");
        this.id = id;
        this.semaforos = semaforos;
        this.dashboard = dashboard;
        this.tempoVerde = tempoVerde;
        this.tempoAllRed = tempoAllRed;
        setDaemon(true);
    }

    /**
     * Metodo principal de execução da thread do controlador de semáforos.
     *
     * Este loop alterna ciclicamente os estados dos semáforos do cruzamento:
     * 1. Todos os semáforos são colocados a vermelho (fase de segurança).
     * 2. Um semáforo é aberto (verde) de cada vez, conforme a ordem definida.
     * 3. Após o tempo verde, todos voltam a vermelho antes de mudar para o próximo.
     *
     * O processo repete-se indefinidamente, garantindo que nunca há duas direções
     * ativas em simultâneo e mantendo um intervalo de segurança entre trocas.
     */
    @Override
    public void run() {
        List<String> ordens = new ArrayList<>(semaforos.keySet());
        if (ordens.isEmpty()) return;

        int i = 0;
        while (true) {
            setAllRed();
            dormir(tempoAllRed);

            String ativa = ordens.get(i % ordens.size());
            abrirSemaforo(ativa);
            enviarEstadoDashboard(ativa);

            dormir(tempoVerde);
            setAllRed();
            dormir(tempoAllRed);

            i++;
        }
    }

    /**
     * Coloca todos os semáforos deste cruzamento em vermelho.
     *
     * Este metodo é chamado durante o período de segurança (all-red),
     * garantindo que nenhuma direção permanece verde enquanto o sistema
     * faz a transição para outro semáforo.
     */
    private void setAllRed() {
        semaforos.values().forEach(Semaforo::fechar);
    }

    /**
     * Abre apenas o semáforo indicado (verde) e fecha todos os restantes.
     *
     * @param semaforoAAbrir Nome da direção cujo semáforo deve ficar verde
     */
    private void abrirSemaforo(String semaforoAAbrir ) {
        semaforos.forEach((nome, s) -> {
            if (nome.equals(semaforoAAbrir )) {
                s.abrir();
            } else {
                s.fechar();
            }
        });
    }

    /**
     * Envia para o Dashboard o estado atual dos semáforos deste cruzamento.
     * O semáforo indicado no parâmetro ficará marcado como "VERDE",
     * enquanto todos os outros serão enviados como "VERMELHO".
     *
     * Exemplo de mensagem enviada:
     * [Semaforo] Cr3 de_E3_para_S=VERDE de_Cr2_para_S=VERMELHO
     *
     * @param semaforoAAbrir nome do semáforo que está atualmente verde
     */
    private void enviarEstadoDashboard(String semaforoAAbrir) {
        // Cria o início da mensagem com o identificador do cruzamento
        StringBuilder mensagem = new StringBuilder();
        mensagem.append("[Semaforo] ")
                .append(id) // nome do cruzamento (ex.: Cr3)
                .append(" ");

        // Para cada semáforo deste cruzamento, acrescenta o estado correspondente
        semaforos.forEach((nome, semaforo) -> {
            mensagem.append(nome) // nome do semáforo (ex.: de_E3_para_S)
                    .append("=");

            if (nome.equals(semaforoAAbrir)) {
                mensagem.append("VERDE ");
            } else {
                mensagem.append("VERMELHO ");
            }
        });

        // Remove espaços em excesso e envia ao Dashboard
        String mensagemFinal = mensagem.toString().trim();
        dashboard.enviar(mensagemFinal);
    }


    /**
     * Faz o processo "adormecer" durante o tempo especificado.
     *
     * Este metodo é utilizado para introduzir pequenas pausas entre ações
     * (por exemplo, entre as trocas de estado dos semáforos),
     * sem necessidade de tratar manualmente as exceções de interrupção.
     *
     * @param ms tempo de espera em milissegundos
     */
    private void dormir(long ms) {
        try {
            Thread.sleep(ms); // pausa a thread atual pelo tempo indicado
        } catch (InterruptedException ignored) {
            // Ignora a interrupção para manter o fluxo simples
            Thread.currentThread().interrupt(); // boa prática: restaura o estado de interrupção
        }
    }

}

