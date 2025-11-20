## Desenvolvimento
- [ ] Adicionar mais um modo de simulação  
- [ ] Criar menu para escolher carga  
- [ ] Criar menu para escolher modo  
- [x] ~~Retirar limite de fila / Aumentar limite~~
- [X] ~~Criar classe para calcular estatísticas e enviar as mesmas ao dashboard~~ 
- [X] ~~Mover o método da classe `Veiculo` (responsável por cálculos/estatísticas) para a nova classe de estatísticas~~ 
- [ ] Rever classes e simplificar partes das mesmas (ter em conta o FPOO)  
- [ ] Comentar código  

## Relatório
- [ ] Mostrar estrutura do projeto (não é preciso explicar classes e métodos)  
- [ ] Analisar e comparar resultados com diferentes cargas e cenários  

## Melhorias
- [ ] Rever import das configs. Deve-se importar o endereço IP e Porta do ficheiro configMapa.json (Pode ficar para último)
      
- [x] ~~Dashboard.Utils.EventoMovimento. Classe simples que poderia estar em Dashboard ou Dashboard.Paineis. É específica do mapa, não é utilitário genérico. Não está a ser usada, era para tratar daquele stress de estar mais lento o mapa que o código, a meu ver pode ser eliminada bem como toda a lógica associada à mesma (é preciso ver noutras classes se existem métodos relativos a isto).~~
      
- [x] ~~GestorEstatisticas em Utils. É específico do Dashboard, não é utilitário genérico. Sugestão: Mover para Dashboard.Gestao ou Dashboard (confirmar se é só mover, se for pode-se dar check).~~
      
- [x] ~~Cruzamento.java com problema: Método gerarEstatisticas() com muita responsabilidade:~~
      
      Simplificação sugerida: // Criar classe EstatisticaCruzamento com este código:
        public class EstatisticaCruzamento {
        private final String nome;
        private final List<EstatisticaSemaforo> semaforos;
    
        // Método toMap() para serialização

        // No Cruzamento:
        public EstatisticaCruzamento gerarEstatisticas() {
            return new EstatisticaCruzamento(
                nomeCruzamento,
                listaSemaforos.stream()
                    .map(Semaforo::getEstatistica)
                    .collect(Collectors.toList())
            );
        }
      
- [x] ~~Mensagem.java - Poderia usar record (Java 17+):~~
      
      public record Mensagem(
          String tipo,
          String origem,
          String destino,
          Map<String, Object> conteudo,
          long timestamp
      ) {
          public Mensagem(String tipo, String origem, String destino, Map<String, Object> conteudo) {
              this(tipo, origem, destino, conteudo, System.currentTimeMillis());
          }
          
          public String toJson() { return new Gson().toJson(this); }
          public static Mensagem fromJson(String json) { return new Gson().fromJson(json, Mensagem.class); }
      }
      
- [ ] GestorEstatisticas.java com problema: Classe muito grande (200+ linhas) com múltiplas responsabilidades. Simplificação:
        Separar DTOs (EstatisticasGlobais, EstatisticasFila, EstatisticasSaida) em ficheiro próprio
        Extrair lógica de notificação para classe NotificadorEstatisticas
        Usar Streams API para simplificar getTotalGerado():

- [x] ~~PainelMapa.java está muito grande (400+ linhas), extrair lógica de desenho para classes auxiliares: DesenharVias, DesenharNos, DesenharSemaforos, DesenharVeiculos. Extrair cálculos de posições para classe GestorPosicoes~~
      
- [x] ~~Utils EnviarLogs.java duplica funcionalidade de DashLogger.java~~
