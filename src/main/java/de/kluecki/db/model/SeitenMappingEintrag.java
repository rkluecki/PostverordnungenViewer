package de.kluecki.db.model;

public class SeitenMappingEintrag {

    private final int bildIndex;
    private final String dateiname;
    private final String logischeSeite;

    public SeitenMappingEintrag(int bildIndex, String dateiname, String logischeSeite) {
        this.bildIndex = bildIndex;
        this.dateiname = dateiname;
        this.logischeSeite = logischeSeite;
    }

    public int getBildIndex() {
        return bildIndex;
    }

    public String getDateiname() {
        return dateiname;
    }

    public String getLogischeSeite() {
        return logischeSeite;
    }
}