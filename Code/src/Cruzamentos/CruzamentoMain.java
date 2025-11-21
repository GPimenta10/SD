/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Cruzamentos;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import Dashboard.Logs.TipoLog;
import Logging.LogClienteDashboard;
import Utils.ConfigLoader;

/**
 * Processo principal para iniciar um cruzamento específico do sistema de tráfego.
 *
 * Este processo é responsável por:
 *  Receber o nome do cruzamento como argumento (ex: "Cr1", "Cr2")
 *  Carregar a configuração do cruzamento do ficheiro configMapa.json
 *  Inicializar o cruzamento com suas ligações para outros nós
 *  Manter o processo ativo até receber sinal de encerramento
 *
 */
public class CruzamentoMain {

    private static final int SLEEP_INTERVAL_MS = 1000;

    /**
     * Ponto de entrada do processo Cruzamento.
     *
     * @param args Argumentos da linha de comando. args[0] deve conter o nome do cruzamento
     */
    public static void main(String[] args) {

        // Valida argumentos
        if (args.length < 1) {
            System.err.println("ERRO: Nome do cruzamento não especificado!");
            System.err.println("USO: java Cruzamentos.CruzamentoMain <nomeCruzamento>");
            System.err.println("Exemplo: java Cruzamentos.CruzamentoMain Cr3");
            System.exit(1);
        }

        String nomeCruzamento = args[0];
        LogClienteDashboard.definirNomeProcesso(nomeCruzamento);
        LogClienteDashboard.enviar(TipoLog.SISTEMA, "Processo Cruzamento " + nomeCruzamento + " iniciado");

        try {
            JsonObject configCruzamento = carregarConfiguracao(nomeCruzamento);

            String nome = configCruzamento.get("nome").getAsString();
            
            // IMPORTANTE: Ler o IP da configuração
            String ipServidor = configCruzamento.has("ipServidor") 
                    ? configCruzamento.get("ipServidor").getAsString() 
                    : "localhost";
                    
            int portaServidor = configCruzamento.get("portaServidor").getAsInt();
            String ipDashboard = configCruzamento.get("ipDashboard").getAsString();
            int portaDashboard = configCruzamento.get("portaDashboard").getAsInt();

            LogClienteDashboard.enviar(
                    TipoLog.SISTEMA,
                    String.format("Configuração carregada para %s: servidor=%s:%d, dashboard=%s:%d",
                            nome, ipServidor, portaServidor, ipDashboard, portaDashboard)
            );

            // Passar IP para o construtor
            Cruzamento cruzamento = new Cruzamento(nome, ipServidor, portaServidor, ipDashboard, portaDashboard);
            carregarLigacoes(cruzamento, configCruzamento, nome);

            cruzamento.iniciar();
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Cruzamento " + nome + " ativo e operacional");

            registarShutdownHook(cruzamento, nome);
            manterProcessoAtivo();

        } catch (Exception e) {
            System.err.println("[CruzamentoMain] ERRO FATAL: " + e.getMessage());
            LogClienteDashboard.enviar(TipoLog.ERRO, "Erro fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Carrega a configuração de um cruzamento específico do ficheiro configMapa.json.
     *
     * Procura no array "cruzamentos" o objeto cujo campo "nome" corresponde
     * ao nome fornecido. Se não encontrado, termina o processo com erro.
     *
     * @param nomeCruzamento Nome do cruzamento a procurar
     * @return JsonObject com a configuração completa do cruzamento
     */
    private static JsonObject carregarConfiguracao(String nomeCruzamento) {
        JsonObject config = ConfigLoader.carregarCruzamento(nomeCruzamento);

        if (config == null) {
            System.err.printf("[CruzamentoMain] Cruzamento '%s' não encontrado!%n", nomeCruzamento);
            LogClienteDashboard.enviar(TipoLog.ERRO, "Cruzamento '" + nomeCruzamento + "' não existe no configMapa.json");
            System.exit(1);
        }

        return config;
    }

    /**
     * Carrega e adiciona todas as ligações de um cruzamento.
     *
     * Lê o array "ligacoes" da configuração e adiciona cada uma ao cruzamento.
     * Cada ligação define uma rota possível: origem → destino (IP:porta).
     *
     * @param cruzamento Instância do cruzamento onde adicionar as ligações
     * @param config Configuração JSON do cruzamento
     * @param nome Nome do cruzamento (para logs)
     */
    private static void carregarLigacoes(Cruzamento cruzamento, JsonObject config, String nome) {
        if (!config.has("ligacoes")) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Cruzamento " + nome + " não tem ligações definidas");
            return;
        }

        JsonArray ligacoes = config.getAsJsonArray("ligacoes");

        if (ligacoes.size() == 0) {
            LogClienteDashboard.enviar(TipoLog.AVISO, "Cruzamento " + nome + " não tem ligações definidas");
            return;
        }

        LogClienteDashboard.enviar(
                TipoLog.SISTEMA,
                String.format("Cruzamento %s — %d ligações carregadas", nome, ligacoes.size())
        );

        for (JsonElement elem : ligacoes) {
            JsonObject lig = elem.getAsJsonObject();

            String origem = lig.get("origem").getAsString();
            String destino = lig.get("destino").getAsString();
            String ip = lig.get("ip").getAsString();
            int porta = lig.get("porta").getAsInt();

            cruzamento.adicionarLigacao(origem, destino, ip, porta);
        }
    }

    /**
     * Regista um shutdown hook para encerramento gracioso do cruzamento.
     *
     * Garante que o cruzamento é parado corretamente quando o processo
     * recebe sinais de terminação (SIGINT, SIGTERM) ou quando a JVM encerra.
     *
     * @param cruzamento Instância do cruzamento a encerrar
     * @param nome Nome do cruzamento (para logs)
     */
    private static void registarShutdownHook(Cruzamento cruzamento, String nome) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "A encerrar cruzamento " + nome);
            cruzamento.parar();
            LogClienteDashboard.enviar(TipoLog.SISTEMA, "Cruzamento " + nome + " encerrado");
        }));
    }

    /**
     * Mantém o processo ativo através de um loop infinito.
     *
     * Necessário para que as threads daemon (servidor, cliente) continuem
     * a executar. Em caso de interrupção, o shutdown hook trata do encerramento.
     */
    private static void manterProcessoAtivo() {
        try {
            while (true) {
                Thread.sleep(SLEEP_INTERVAL_MS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
