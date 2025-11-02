package OutrasClasses;

import Veiculo.Veiculo;
import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * ComunicadorSocket - Classe utilitária para TODA a comunicação via sockets.
 *
 * OBJETIVO: Evitar duplicação de código entre componentes.
 *
 * Todos os cruzamentos, entradas e saída fazem o mesmo:
 * - Abrir socket
 * - Criar streams (ObjectOutputStream, PrintWriter)
 * - Enviar dados
 * - Fechar conexão
 * - Tratar erros
 *
 * Em vez de repetir isso dezenas de vezes, centralizamos aqui!
 */
public class ComunicadorSocket {

    // Formato de hora legível para logs
    private static final DateTimeFormatter HORA_FORMATO = DateTimeFormatter.ofPattern("HH:mm:ss");

    /**
     * Envia um VEÍCULO (objeto serializado) para um destino.
     *
     * Usado por: Cruzamentos → Saída
     *            Entradas → Cruzamentos
     *            Cruzamentos → Cruzamentos
     *
     * @param v Veículo a enviar
     * @param host Endereço de destino (ex: "127.0.0.1")
     * @param porta Porta de destino (ex: 7000)
     * @param origem Nome de quem envia (para logs, ex: "Cruzamento3")
     * @return true se enviou com sucesso, false se falhou
     */
    public static boolean enviarVeiculo(Veiculo v, String host, int porta, String origem) {
        try (Socket socket = new Socket(host, porta);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            out.writeObject(v);
            out.flush();

            System.out.printf("[%s] ✓ Enviado veículo %s para %s:%d%n",
                    origem, v.getId(), host, porta);
            return true;

        } catch (IOException e) {
            System.err.printf("[%s] ✗ Erro ao enviar veículo %s: %s%n",
                    origem, v.getId(), e.getMessage());
            return false;
        }
    }

    /**
     * Envia MENSAGEM DE TEXTO para o Dashboard.
     *
     * Usado por: Todos os componentes → Dashboard
     *
     * Dashboard só recebe texto (logs, eventos), não objetos.
     *
     * @param mensagem Texto a enviar (ex: "[Cruzamento3] Veículo E3-001 recebido")
     * @param hostDashboard Endereço do Dashboard
     * @param portaDashboard Porta do Dashboard
     */
    public static void enviarParaDashboard(String mensagem, String hostDashboard, int portaDashboard) {
        try (Socket socket = new Socket(hostDashboard, portaDashboard);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            out.println(mensagem);

        } catch (IOException e) {
            // Dashboard pode estar offline - evitamos spam no console
            long agora = System.currentTimeMillis();
            if (agora % 5000 < 50) { // imprime aviso a cada ~5s
                System.err.println("[ComunicadorSocket] ⚠️ Dashboard offline (" + e.getMessage() + ")");
            }
        }
    }

    /**
     * Envia EVENTO formatado para o Dashboard (com timestamp legível e epoch).
     *
     * Formato: [HH:mm:ss|epoch] COMPONENTE | EVENTO | DETALHES
     * Exemplo: [14:32:10|1730214531221] Cruzamento3 | RECEBIDO | veiculo=E3-001,tipo=CARRO
     *
     * Usado por: Cruzamentos, Saída, Entradas
     *
     * @param componente Nome do componente (ex: "Cruzamento3", "Saída")
     * @param evento Tipo de evento (ex: "RECEBIDO", "SAIU", "FILA_CHEIA")
     * @param detalhes Informação adicional (ex: "veiculo=E3-001,tipo=CARRO")
     * @param hostDashboard Endereço do Dashboard
     * @param portaDashboard Porta do Dashboard
     */
    public static void enviarEventoDashboard(String componente, String evento, String detalhes, String hostDashboard, int portaDashboard) {
        long timestamp = System.currentTimeMillis();
        String hora = LocalTime.now().format(HORA_FORMATO);

        String mensagem = String.format("[%s|%d] %s | %s | %s",
                hora, timestamp, componente, evento, detalhes);

        enviarParaDashboard(mensagem, hostDashboard, portaDashboard);
    }

    /**
     * Envia ESTATÍSTICAS estruturadas para o Dashboard.
     *
     * Formato: STATS|COMPONENTE|chave1=valor1,chave2=valor2,...
     * Exemplo: STATS|Cruzamento3|total=45,motos=12,carros=28,filaE3=3
     *
     * O Dashboard pode parsear e mostrar em formato tabela/gráfico.
     *
     * @param componente Nome do componente
     * @param stats String formatada "key=value,key=value,..."
     * @param hostDashboard Endereço do Dashboard
     * @param portaDashboard Porta do Dashboard
     */
    public static void enviarEstatisticas(String componente, String stats,
                                          String hostDashboard, int portaDashboard) {
        String mensagem = String.format("STATS|%s|%s", componente, stats);
        enviarParaDashboard(mensagem, hostDashboard, portaDashboard);
    }
}
