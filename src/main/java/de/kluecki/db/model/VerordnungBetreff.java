package de.kluecki.db.model;

public class VerordnungBetreff {
    private int verordnungBetreffID;

    private String gebiet;
    private String bandJahr;

    private int seiteVon;
    private int seiteBis;

    private String titel;
    private String bemerkung;

    public int getVerordnungBetreffID() {
        return verordnungBetreffID;
    }

    public void setVerordnungBetreffID(int verordnungBetreffID) {
        this.verordnungBetreffID = verordnungBetreffID;
    }

    public String getGebiet() {
        return gebiet;
    }

    public void setGebiet(String gebiet) {
        this.gebiet = gebiet;
    }

    public String getBandJahr() {
        return bandJahr;
    }

    public void setBandJahr(String bandJahr) {
        this.bandJahr = bandJahr;
    }

    public int getSeiteVon() {
        return seiteVon;
    }

    public void setSeiteVon(int seiteVon) {
        this.seiteVon = seiteVon;
    }

    public int getSeiteBis() {
        return seiteBis;
    }

    public void setSeiteBis(int seiteBis) {
        this.seiteBis = seiteBis;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getBemerkung() {
        return bemerkung;
    }

    public void setBemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
    }

    @Override
    public String toString() {
        return "VerordnungBetreff{" +
                "verordnungBetreffID=" + verordnungBetreffID +
                ", gebiet='" + gebiet + '\'' +
                ", bandJahr='" + bandJahr + '\'' +
                ", seiteVon=" + seiteVon +
                ", seiteBis=" + seiteBis +
                ", titel='" + titel + '\'' +
                ", bemerkung='" + bemerkung + '\'' +
                '}';
    }
}
