package de.kluecki.db.model;

public class Veroeffentlichung {

    private int quelleId;
    private String nummer;
    private String titel;
    private String datum;
    private int seiteVon;
    private int seiteBis;
    private String status;
    private int ebeneSortierung;

    public int getQuelleId() {
        return quelleId;
    }

    public void setQuelleId(int quelleId) {
        this.quelleId = quelleId;
    }

    public String getNummer() {
        return nummer;
    }

    public void setNummer(String nummer) {
        this.nummer = nummer;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getDatum() {
        return datum;
    }

    public void setDatum(String datum) {
        this.datum = datum;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getEbeneSortierung() {
        return ebeneSortierung;
    }

    public void setEbeneSortierung(int ebeneSortierung) {
        this.ebeneSortierung = ebeneSortierung;
    }

    public String getSeitenAnzeige() {
        if (seiteVon == seiteBis) {
            return String.valueOf(seiteVon);
        }
        return seiteVon + "-" + seiteBis;
    }

    @Override
    public String toString() {
        return (nummer != null ? nummer + " | " : "") + titel;
    }
}