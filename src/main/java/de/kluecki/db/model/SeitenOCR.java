package de.kluecki.db.model;

import java.time.LocalDateTime;

public class SeitenOCR {

    private int seitenOCRID;
    private int bandID;
    private int bildIndex;
    private String dateiname;
    private String logischeSeite;
    private String ocrText;
    private String ocrTextKorrigiert;
    private String ocrKorrekturStatus;
    private LocalDateTime korrigiertAm;
    private LocalDateTime geprueftAm;
    private String ocrQuelle;
    private String ocrFormat;
    private LocalDateTime erstelltAm;
    private LocalDateTime geaendertAm;

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

    public String getOcrText() {
        return ocrText;
    }

    public void setOcrText(String ocrText) {
        this.ocrText = ocrText;
    }

    public String getOcrTextKorrigiert() {
        return ocrTextKorrigiert;
    }

    public void setOcrTextKorrigiert(String ocrTextKorrigiert) {
        this.ocrTextKorrigiert = ocrTextKorrigiert;
    }

    public String getOcrKorrekturStatus() {
        return ocrKorrekturStatus;
    }

    public void setOcrKorrekturStatus(String ocrKorrekturStatus) {
        this.ocrKorrekturStatus = ocrKorrekturStatus;
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

    public LocalDateTime getErstelltAm() {
        return erstelltAm;
    }

    public void setErstelltAm(LocalDateTime erstelltAm) {
        this.erstelltAm = erstelltAm;
    }

    public LocalDateTime getGeaendertAm() {
        return geaendertAm;
    }

    public void setGeaendertAm(LocalDateTime geaendertAm) {
        this.geaendertAm = geaendertAm;
    }

    public LocalDateTime getKorrigiertAm() {
        return korrigiertAm;
    }

    public void setKorrigiertAm(LocalDateTime korrigiertAm) {
        this.korrigiertAm = korrigiertAm;
    }

    public LocalDateTime getGeprueftAm() {
        return geprueftAm;
    }

    public void setGeprueftAm(LocalDateTime geprueftAm) {
        this.geprueftAm = geprueftAm;
    }
}