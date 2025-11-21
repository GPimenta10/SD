/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Dashboard.Logs;

/**
 * Enum para identificar tipo de log e respetivo icone
 * 
 */
public enum TipoLog {
    SISTEMA("⚙️"),
    GERADOR("🚀"),
    VEICULO("🚗"),
    CRUZAMENTO("➕"),
    FILA("📊"),
    SEMAFORO("🚦"),
    ERRO("❌"),
    AVISO("⚠️"),
    SUCESSO("✅");

    private final String icone;
    
    /**
     * Construtor da classe
     * 
     * @param icone Icone associado ao texto (log)
     */
    TipoLog(String icone) {
        this.icone = icone;
    }
    
    /**
     * Método para obter o icone
     * 
     * @return Um icone
     */
    public String getIcone() {
        return icone;
    }
}