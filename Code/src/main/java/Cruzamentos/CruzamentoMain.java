package Cruzamentos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Processo genérico para iniciar UM cruzamento específico.
 *
 * USO: java Cruzamentos.CruzamentoMain <nomeCruzamento>
 * Exemplo: java Cruzamentos.CruzamentoMain Cr3
 *
 * Este processo:
 * 1. Recebe o nome do cruzamento como argumento
 * 2. Lê o configCruzamentos.json
 * 3. Filtra apenas a configuração do cruzamento especificado
 * 4. Cria e inicia APENAS esse cruzamento
 * 5. Mantém o processo ativo
*/
public class CruzamentoMain {

    public static void main(String[] args) {

        // === VALIDAÇÃO DE ARGUMENTOS ===
        if (args.length < 1) {
            System.err.println("ERRO: Nome do cruzamento não especificado!");
            System.err.println("USO: java Cruzamentos.CruzamentoMain <nomeCruzamento>");
            System.err.println("Exemplo: java Cruzamentos.CruzamentoMain Cr3");
            System.exit(1);
        }

        String nomeCruzamento = args[0];

        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Cruzamento " + nomeCruzamento);
        System.out.println("=".repeat(60));

        try {
            // === LER CONFIGURAÇÃO DO JSON ===
            InputStream in = CruzamentoMain.class.getResourceAsStream("/configCruzamentos.json");

            if (in == null) {
                System.err.println("[CruzamentoMain] ERRO: Ficheiro configCruzamentos.json não encontrado!");
                System.exit(1);
            }

            InputStreamReader reader = new InputStreamReader(in);
            Gson gson = new Gson();
            Type tipoConfig = new TypeToken<Map<String, List<Map<String, Object>>>>() {}.getType();
            Map<String, List<Map<String, Object>>> config = gson.fromJson(reader, tipoConfig);

            List<Map<String, Object>> cruzamentos = config.get("cruzamentos");

            if (cruzamentos == null || cruzamentos.isEmpty()) {
                System.err.println("[CruzamentoMain] ERRO: Nenhum cruzamento encontrado no configCruzamentos.json!");
                System.exit(1);
            }

            // === PROCURAR A CONFIGURAÇÃO DO CRUZAMENTO ESPECIFICADO ===
            Map<String, Object> configCruzamento = null;

            for (Map<String, Object> c : cruzamentos) {
                String nome = (String) c.get("nome");
                if (nome.equals(nomeCruzamento)) {
                    configCruzamento = c;
                    break;
                }
            }

            if (configCruzamento == null) {
                System.err.printf("[CruzamentoMain] ERRO: Cruzamento '%s' não encontrado no configCruzamentos.json!%n",
                        nomeCruzamento);
                System.err.println("[CruzamentoMain] Cruzamentos disponíveis:");
                for (Map<String, Object> c : cruzamentos) {
                    System.err.println("  - " + c.get("nome"));
                }
                System.exit(1);
            }

            // === EXTRAIR PARÂMETROS DA CONFIGURAÇÃO ===
            String nome = (String) configCruzamento.get("nome");
            int portaServidor = ((Number) configCruzamento.get("portaServidor")).intValue();
            String ipDashboard = (String) configCruzamento.get("ipDashboard");
            int portaDashboard = ((Number) configCruzamento.get("portaDashboard")).intValue();

            System.out.printf("[CruzamentoMain] Configuração carregada:%n");
            System.out.printf("  - Nome: %s%n", nome);
            System.out.printf("  - Porta Servidor: %d%n", portaServidor);
            System.out.printf("  - Dashboard: %s:%d%n", ipDashboard, portaDashboard);

            // === CRIAR O CRUZAMENTO ===
            Cruzamento cruzamento = new Cruzamento(nome, portaServidor, ipDashboard, portaDashboard);

            // === ADICIONAR LIGAÇÕES ===
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ligacoes = (List<Map<String, Object>>) configCruzamento.get("ligacoes");

            if (ligacoes != null && !ligacoes.isEmpty()) {
                System.out.printf("[CruzamentoMain] Adicionando %d ligações...%n", ligacoes.size());

                for (Map<String, Object> lig : ligacoes) {
                    String origem = (String) lig.get("origem");
                    String destino = (String) lig.get("destino");
                    String ip = (String) lig.get("ip");
                    int porta = ((Number) lig.get("porta")).intValue();

                    cruzamento.adicionarLigacao(origem, destino, ip, porta);
                    System.out.printf("  ✓ %s -> %s (%s:%d)%n", origem, destino, ip, porta);
                }
            } else {
                System.out.println("[CruzamentoMain] AVISO: Nenhuma ligação configurada!");
            }

            // === INICIAR O CRUZAMENTO ===
            System.out.println();
            cruzamento.iniciar();
            System.out.printf("[CruzamentoMain] Cruzamento %s ativo e operacional!%n", nome);
            System.out.println("=".repeat(60));

            // === SHUTDOWN HOOK ===
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println();
                System.out.printf("[CruzamentoMain] Encerrando cruzamento %s...%n", nome);
                cruzamento.parar();
                System.out.printf("[CruzamentoMain] Cruzamento %s encerrado com sucesso.%n", nome);
            }));

            // === MANTER PROCESSO ATIVO ===
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("[CruzamentoMain] ERRO FATAL: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}