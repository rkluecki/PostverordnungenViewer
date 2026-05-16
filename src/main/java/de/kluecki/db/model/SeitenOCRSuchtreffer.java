package de.kluecki.db.model;

/**
 * Suchtreffer für OCR-Text.
 *
 * Diese Klasse ist bewusst klein:
 * Sie enthält nur die Informationen, die wir später
 * in einer Trefferliste anzeigen und zum Springen zur Seite brauchen.
 */
public class SeitenOCRSuchtreffer {

    private int seitenOCRID;
    private int bandID;
    private int bildIndex;
    private String dateiname;
    private String logischeSeite;
    private String ocrQuelle;
    private String ocrFormat;
    private String textAusschnitt;

    public int getSeitenOCRID() {
        return seitenOCRID;
    }

    public void setSeitenOCRID(int seitenOCRID) {
        this.seitenOCRID = seitenOCRID;
    }

    public int getBandID() {
        return bandID;
    }

    public void setBandID(int bandID) {
        this.bandID = bandID;
    }

    public int getBildIndex() {
        return bildIndex;
    }

    public void setBildIndex(int bildIndex) {
        this.bildIndex = bildIndex;
    }

    public String getDateiname() {
        return dateiname;
    }

    public void setDateiname(String dateiname) {
        this.dateiname = dateiname;
    }

    public String getLogischeSeite() {
        return logischeSeite;
    }

    public void setLogischeSeite(String logischeSeite) {
        this.logischeSeite = logischeSeite;
    }

    public String getOcrQuelle() {
        return ocrQuelle;
    }

    public void setOcrQuelle(String ocrQuelle) {
        this.ocrQuelle = ocrQuelle;
    }

    public String getOcrFormat() {
        return ocrFormat;
    }

    public void setOcrFormat(String ocrFormat) {
        this.ocrFormat = ocrFormat;
    }

    public String getTextAusschnitt() {
        return textAusschnitt;
    }

    public void setTextAusschnitt(String textAusschnitt) {
        this.textAusschnitt = textAusschnitt;
    }
}