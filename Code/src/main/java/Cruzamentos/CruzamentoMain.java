package Cruzamentos;


import Dashboard.TipoLog;
import Utils.EnviarLogs;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Processo genérico para iniciar um cruzamento específico.
 *
 * Fluxo:
 * 1. Recebe o nome do cruzamento por argumento
 * 2. Lê configCruzamentos.json
 * 3. Carrega apenas esse cruzamento
 * 4. Inicia o cruzamento
 * 5. Mantém processo ativo
 */
public class CruzamentoMain {

    public static void main(String[] args) {

        // VALIDAÇÃO DE ARGUMENTOS
        if (args.length < 1) {
            /*System.err.println("ERRO: Nome do cruzamento não especificado!");
            System.err.println("USO: java Cruzamentos.CruzamentoMain <nomeCruzamento>");
            System.err.println("Exemplo: java Cruzamentos.CruzamentoMain Cr3");*/

            EnviarLogs.enviar(TipoLog.ERRO, "CruzamentoMain iniciado sem argumentos");
            System.exit(1);
        }

        String nomeCruzamento = args[0];
        EnviarLogs.definirNomeProcesso(nomeCruzamento);
        EnviarLogs.enviar(TipoLog.SISTEMA, "Processo Cruzamento " + nomeCruzamento + " iniciado");

        try {
            //LER CONFIGURAÇÃO DO JSON
            InputStream in = CruzamentoMain.class.getResourceAsStream("/configCruzamentos.json");

            if (in == null) {
                System.err.println("[CruzamentoMain] ERRO: configCruzamentos.json não encontrado!");
                EnviarLogs.enviar(TipoLog.ERRO, "Ficheiro configCruzamentos.json não encontrado!");
                System.exit(1);
            }

            InputStreamReader reader = new InputStreamReader(in);

            Gson gson = new Gson();
            Type tipoConfig = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();

            Map<String, List<Map<String, Object>>> config = gson.fromJson(reader, tipoConfig);
            List<Map<String, Object>> cruzamentos = config.get("cruzamentos");

            if (cruzamentos == null || cruzamentos.isEmpty()) {
                System.err.println("[CruzamentoMain] ERRO: Nenhum cruzamento encontrado no JSON!");
                EnviarLogs.enviar(TipoLog.ERRO, "Nenhum cruzamento encontrado no configCruzamentos.json");
                System.exit(1);
            }

            //PROCURAR A CONFIGURAÇÃO DO CRUZAMENTO
            Map<String, Object> configCruzamento = null;

            for (Map<String, Object> c : cruzamentos) {
                String nome = (String) c.get("nome");
                if (nome.equals(nomeCruzamento)) {
                    configCruzamento = c;
                    break;
                }
            }

            if (configCruzamento == null) {
                System.err.printf("[CruzamentoMain] Cruzamento '%s' não encontrado!%n", nomeCruzamento);
                EnviarLogs.enviar(TipoLog.ERRO, "Cruzamento '" + nomeCruzamento + "' não existe no JSON");

                // (debug futuro)
                // System.err.println("[CruzamentoMain] Lista disponível:");
                // for (Map<String, Object> c : cruzamentos)
                //     System.err.println("  - " + c.get("nome"));

                System.exit(1);
            }

            //EXTRAIR PARÂMETROS
            String nome = (String) configCruzamento.get("nome");
            int portaServidor = ((Number) configCruzamento.get("portaServidor")).intValue();
            String ipDashboard = (String) configCruzamento.get("ipDashboard");
            int portaDashboard = ((Number) configCruzamento.get("portaDashboard")).intValue();

            EnviarLogs.enviar(TipoLog.SISTEMA, "Configuração carregada para " + nome + ": servidor=" + portaServidor +
                            ", dashboard=" + ipDashboard + ":" + portaDashboard);

            //CRIAR O CRUZAMENTO
            Cruzamento cruzamento = new Cruzamento(nome, portaServidor, ipDashboard, portaDashboard);

            //ADICIONAR LIGAÇÕES
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ligacoes = (List<Map<String, Object>>) configCruzamento.get("ligacoes");

            if (ligacoes != null && !ligacoes.isEmpty()) {
                EnviarLogs.enviar(TipoLog.SISTEMA, "Cruzamento " + nome + " — " + ligacoes.size() + " ligações carregadas");

                for (Map<String, Object> lig : ligacoes) {
                    String origem = (String) lig.get("origem");
                    String destino = (String) lig.get("destino");
                    String ip = (String) lig.get("ip");
                    int porta = ((Number) lig.get("porta")).intValue();

                    cruzamento.adicionarLigacao(origem, destino, ip, porta);

                    // (debug futuro)
                    // System.out.printf("  ✓ %s -> %s (%s:%d)%n", origem, destino, ip, porta);
                }
            } else {
                EnviarLogs.enviar(TipoLog.AVISO, "Cruzamento " + nome + " não tem ligações definidas");
            }

            //INICIAR CRUZAMENTO
            cruzamento.iniciar();
            EnviarLogs.enviar(TipoLog.SISTEMA, "Cruzamento " + nome + " ativo e operacional");

            // SHUTDOWN HOOK
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                EnviarLogs.enviar(TipoLog.SISTEMA, "A encerrar cruzamento " + nome);
                cruzamento.parar();
                EnviarLogs.enviar(TipoLog.SISTEMA, "Cruzamento " + nome + " encerrado");
            }));

            // MANTER PROCESSO ATIVO
            while (true) {
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            System.err.println("[CruzamentoMain] ERRO FATAL: " + e.getMessage());
            EnviarLogs.enviar(TipoLog.ERRO, "Erro fatal: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
