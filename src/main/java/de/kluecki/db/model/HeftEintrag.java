/*
 * Neue Zielstruktur
 *
 * Zweck:
 * Repräsentiert einen Eintrag innerhalb eines Heftes
 * (zentrale fachliche Einheit).
 *
 * Rolle im Projekt:
 * Soll langfristig die alte Veröffentlichungsstruktur ersetzen.
 *
 * Fachliche Struktur:
 * Heft -> HeftEintrag -> Inhaltseinheit
 */

package de.kluecki.db.model;

import java.time.LocalDate;

public class HeftEintrag {

    private int heftEintragID;
    private int heftID;
    private int heftEintragTypID;
    private String nro;
    private String titel;
    private LocalDate datum;
    private int seiteVon;
    private Integer seiteBis;
    private int sortierung;
    private String bemerkung;
    private boolean istAktiv;
    private String bandJahrAnzeige;
    private String gebietAnzeige;
    private String heftNummerAnzeige;

    public HeftEintrag() {
    }

    public int getHeftEintragID() {
        return heftEintragID;
    }

    public void setHeftEintragID(int heftEintragID) {
        this.heftEintragID = heftEintragID;
    }

    public int getHeftID() {
        return heftID;
    }

    public void setHeftID(int heftID) {
        this.heftID = heftID;
    }

    public int getHeftEintragTypID() {
        return heftEintragTypID;
    }

    public void setHeftEintragTypID(int heftEintragTypID) {
        this.heftEintragTypID = heftEintragTypID;
    }

    public String getNro() {
        return nro;
    }

    public void setNro(String nro) {
        this.nro = nro;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public LocalDate getDatum() {
        return datum;
    }

    public void setDatum(LocalDate datum) {
        this.datum = datum;
    }

    public int getSeiteVon() {
        return seiteVon;
    }

    public void setSeiteVon(int seiteVon) {
        this.seiteVon = seiteVon;
    }

    public Integer getSeiteBis() {
        return seiteBis;
    }

    public void setSeiteBis(Integer seiteBis) {
        this.seiteBis = seiteBis;
    }

    public int getSortierung() {
        return sortierung;
    }

    public void setSortierung(int sortierung) {
        this.sortierung = sortierung;
    }

    public String getBemerkung() {
        return bemerkung;
    }

    public void setBemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
    }

    public boolean isIstAktiv() {
        return istAktiv;
    }

    public void setIstAktiv(boolean istAktiv) {
        this.istAktiv = istAktiv;
    }

    public String getBandJahrAnzeige() {
        return bandJahrAnzeige;
    }

    public void setBandJahrAnzeige(String bandJahrAnzeige) {
        this.bandJahrAnzeige = bandJahrAnzeige;
    }

    public String getGebietAnzeige() {
        return gebietAnzeige;
    }

    public void setGebietAnzeige(String gebietAnzeige) {
        this.gebietAnzeige = gebietAnzeige;
    }

    public String getHeftNummerAnzeige() {
        return heftNummerAnzeige;
    }

    public void setHeftNummerAnzeige(String heftNummerAnzeige) {
        this.heftNummerAnzeige = heftNummerAnzeige;
    }

    @Override
    public String toString() {
        return titel != null ? titel : ("HeftEintrag #" + heftEintragID);
    }
}