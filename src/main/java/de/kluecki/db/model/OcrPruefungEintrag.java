package de.kluecki.db.model;

public class OcrPruefungEintrag {

    private int bandID;
    private int bildIndex;
    private String dateiname;
    private String logischeSeite;
    private Integer seitenOCRID;
    private String ocrQuelle;
    private String ocrFormat;
    private String ocrStatus;
    private String entscheidungsart;
    private String pruefBemerkung;
    private String gepruefteQuelle;
    private Boolean istErledigt;

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

    public Integer getSeitenOCRID() {
        return seitenOCRID;
    }

    public void setSeitenOCRID(Integer seitenOCRID) {
        this.seitenOCRID = seitenOCRID;
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

    public String getEntscheidungsart() {
        return entscheidungsart;
    }

    public void setEntscheidungsart(String entscheidungsart) {
        this.entscheidungsart = entscheidungsart;
    }

    public String getPruefBemerkung() {
        return pruefBemerkung;
    }

    public void setPruefBemerkung(String pruefBemerkung) {
        this.pruefBemerkung = pruefBemerkung;
    }

    public String getGepruefteQuelle() {
        return gepruefteQuelle;
    }

    public void setGepruefteQuelle(String gepruefteQuelle) {
        this.gepruefteQuelle = gepruefteQuelle;
    }

    public Boolean getIstErledigt() {
        return istErledigt;
    }

    public void setIstErledigt(Boolean istErledigt) {
        this.istErledigt = istErledigt;
    }

    public void setOcrFormat(String ocrFormat) {
        this.ocrFormat = ocrFormat;
    }

    public String getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(String ocrStatus) {
        this.ocrStatus = ocrStatus;
    }
}