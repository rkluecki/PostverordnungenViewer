package de.kluecki.db.model;

import java.time.LocalDate;

public class Heft {

    private int heftID;
    private int bandID;
    private String heftNummer;
    private String titel;
    private LocalDate ausgabeDatum;
    private int seiteVon;
    private int seiteBis;
    private String bemerkung;
    private int sortierung;
    private boolean istAktiv;

    public int getHeftID() {
        return heftID;
    }

    public void setHeftID(int heftID) {
        this.heftID = heftID;
    }

    public int getBandID() {
        return bandID;
    }

    public void setBandID(int bandID) {
        this.bandID = bandID;
    }

    public String getHeftNummer() {
        return heftNummer;
    }

    public void setHeftNummer(String heftNummer) {
        this.heftNummer = heftNummer;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public LocalDate getAusgabeDatum() {
        return ausgabeDatum;
    }

    public void setAusgabeDatum(LocalDate ausgabeDatum) {
        this.ausgabeDatum = ausgabeDatum;
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

    public String getBemerkung() {
        return bemerkung;
    }

    public void setBemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
    }

    public int getSortierung() {
        return sortierung;
    }

    public void setSortierung(int sortierung) {
        this.sortierung = sortierung;
    }

    public boolean isIstAktiv() {
        return istAktiv;
    }

    public void setIstAktiv(boolean istAktiv) {
        this.istAktiv = istAktiv;
    }

    @Override
    public String toString() {
        if (titel != null && !titel.isBlank()) {
            return titel;
        }
        if (heftNummer != null && !heftNummer.isBlank()) {
            return "Heft " + heftNummer;
        }
        return "Heft " + heftID;
    }
}