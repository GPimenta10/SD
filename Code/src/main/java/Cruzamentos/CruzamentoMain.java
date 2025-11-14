package Cruzamentos;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Processo genérico para iniciar cruzamentos definidos num ficheiro de configuração JSON.
 *
 */
public class CruzamentoMain {

    public static void main(String[] args) {
        System.out.println("=".repeat(60));
        System.out.println("PROCESSO: Cruzamentos a partir de config.json");
        System.out.println("=".repeat(60));

        try (InputStream in = CruzamentoMain.class.getResourceAsStream("/config.json");
             InputStreamReader reader = new InputStreamReader(in)) {
            Gson gson = new Gson();
            Type tipoConfig = new TypeToken<Map<String, List<Map<String, Object>>>>() {
            }.getType();
            Map<String, List<Map<String, Object>>> config = gson.fromJson(reader, tipoConfig);

            List<Map<String, Object>> cruzamentos = config.get("cruzamentos");
            if (cruzamentos == null || cruzamentos.isEmpty()) {
                System.err.println("[CruzamentoMain] Nenhum cruzamento encontrado no ficheiro de configuração!");
                return;
            }

            for (Map<String, Object> c : cruzamentos) {
                String nome = (String) c.get("nome");
                int portaServidor = ((Double) c.get("portaServidor")).intValue();
                String ipDashboard = (String) c.get("ipDashboard");
                int portaDashboard = ((Double) c.get("portaDashboard")).intValue();

                System.out.printf("[CruzamentoMain] Iniciando cruzamento %s na porta %d...%n",
                        nome, portaServidor);

                Cruzamento cr = new Cruzamento(nome, portaServidor, ipDashboard, portaDashboard);

                // Ligar as ligações
                List<Map<String, Object>> ligacoes = (List<Map<String, Object>>) c.get("ligacoes");
                if (ligacoes != null) {
                    for (Map<String, Object> lig : ligacoes) {
                        String origem = (String) lig.get("origem");
                        String destino = (String) lig.get("destino");
                        String ip = (String) lig.get("ip");
                        int porta = ((Double) lig.get("porta")).intValue();
                        cr.adicionarLigacao(origem, destino, ip, porta);
                    }
                }

                cr.iniciar();

                // Adiciona um hook para encerrar corretamente
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("\n[CruzamentoMain] Encerrando cruzamento " + nome + "...");
                    cr.parar();
                }));

                System.out.printf("[CruzamentoMain] Cruzamento %s ativo!%n", nome);
            }

            // Mantém o processo vivo
            while (true) {
                Thread.sleep(1000);
            }

        } catch (Exception e) {
            System.err.println("[CruzamentoMain] Erro ao ler config.json: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
