package Dashboard;

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

    TipoLog(String icone) {
        this.icone = icone;
    }

    public String getIcone() {
        return icone;
    }
}
