package de.kluecki.db.model;

import java.time.LocalDateTime;

public class OrdinataOcrKontrolle {

    private int ordinataOcrKontrolleID;
    private Integer quelleID;

    private String gebietBezeichnung;
    private Integer jahrVon;
    private Integer jahrBis;
    private String bandJahrAnzeige;
    private String unterbandTitel;

    private String quellenTitel;
    private String archivName;
    private String quellenUrl;
    private String manifestId;

    private String quellenStatus;
    private String erschliessbarkeit;
    private String ocrStatus;
    private String importStatus;
    private String pruefStatus;
    private String prioritaet;

    private Integer seitenGesamt;
    private Integer seitenMitOcr;
    private Integer seitenOhneOcr;

    private String naechsterSchritt;
    private String bemerkung;

    private boolean istErledigt;

    private LocalDateTime erstelltAm;
    private LocalDateTime geaendertAm;
    private LocalDateTime zuletztGeprueftAm;

    public int getOrdinataOcrKontrolleID() {
        return ordinataOcrKontrolleID;
    }

    public void setOrdinataOcrKontrolleID(int ordinataOcrKontrolleID) {
        this.ordinataOcrKontrolleID = ordinataOcrKontrolleID;
    }

    public Integer getQuelleID() {
        return quelleID;
    }

    public void setQuelleID(Integer quelleID) {
        this.quelleID = quelleID;
    }

    public String getGebietBezeichnung() {
        return gebietBezeichnung;
    }

    public void setGebietBezeichnung(String gebietBezeichnung) {
        this.gebietBezeichnung = gebietBezeichnung;
    }

    public Integer getJahrVon() {
        return jahrVon;
    }

    public void setJahrVon(Integer jahrVon) {
        this.jahrVon = jahrVon;
    }

    public Integer getJahrBis() {
        return jahrBis;
    }

    public void setJahrBis(Integer jahrBis) {
        this.jahrBis = jahrBis;
    }

    public String getBandJahrAnzeige() {
        return bandJahrAnzeige;
    }

    public void setBandJahrAnzeige(String bandJahrAnzeige) {
        this.bandJahrAnzeige = bandJahrAnzeige;
    }

    public String getUnterbandTitel() {
        return unterbandTitel;
    }

    public void setUnterbandTitel(String unterbandTitel) {
        this.unterbandTitel = unterbandTitel;
    }

    public String getQuellenTitel() {
        return quellenTitel;
    }

    public void setQuellenTitel(String quellenTitel) {
        this.quellenTitel = quellenTitel;
    }

    public String getArchivName() {
        return archivName;
    }

    public void setArchivName(String archivName) {
        this.archivName = archivName;
    }

    public String getQuellenUrl() {
        return quellenUrl;
    }

    public void setQuellenUrl(String quellenUrl) {
        this.quellenUrl = quellenUrl;
    }

    public String getManifestId() {
        return manifestId;
    }

    public void setManifestId(String manifestId) {
        this.manifestId = manifestId;
    }

    public String getQuellenStatus() {
        return quellenStatus;
    }

    public void setQuellenStatus(String quellenStatus) {
        this.quellenStatus = quellenStatus;
    }

    public String getErschliessbarkeit() {
        return erschliessbarkeit;
    }

    public void setErschliessbarkeit(String erschliessbarkeit) {
        this.erschliessbarkeit = erschliessbarkeit;
    }

    public String getOcrStatus() {
        return ocrStatus;
    }

    public void setOcrStatus(String ocrStatus) {
        this.ocrStatus = ocrStatus;
    }

    public String getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(String importStatus) {
        this.importStatus = importStatus;
    }

    public String getPruefStatus() {
        return pruefStatus;
    }

    public void setPruefStatus(String pruefStatus) {
        this.pruefStatus = pruefStatus;
    }

    public String getPrioritaet() {
        return prioritaet;
    }

    public void setPrioritaet(String prioritaet) {
        this.prioritaet = prioritaet;
    }

    public Integer getSeitenGesamt() {
        return seitenGesamt;
    }

    public void setSeitenGesamt(Integer seitenGesamt) {
        this.seitenGesamt = seitenGesamt;
    }

    public Integer getSeitenMitOcr() {
        return seitenMitOcr;
    }

    public void setSeitenMitOcr(Integer seitenMitOcr) {
        this.seitenMitOcr = seitenMitOcr;
    }

    public Integer getSeitenOhneOcr() {
        return seitenOhneOcr;
    }

    public void setSeitenOhneOcr(Integer seitenOhneOcr) {
        this.seitenOhneOcr = seitenOhneOcr;
    }

    public String getNaechsterSchritt() {
        return naechsterSchritt;
    }

    public void setNaechsterSchritt(String naechsterSchritt) {
        this.naechsterSchritt = naechsterSchritt;
    }

    public String getBemerkung() {
        return bemerkung;
    }

    public void setBemerkung(String bemerkung) {
        this.bemerkung = bemerkung;
    }

    public boolean isIstErledigt() {
        return istErledigt;
    }

    public void setIstErledigt(boolean istErledigt) {
        this.istErledigt = istErledigt;
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

    public LocalDateTime getZuletztGeprueftAm() {
        return zuletztGeprueftAm;
    }

    public void setZuletztGeprueftAm(LocalDateTime zuletztGeprueftAm) {
        this.zuletztGeprueftAm = zuletztGeprueftAm;
    }

    @Override
    public String toString() {
        if (bandJahrAnzeige != null && !bandJahrAnzeige.isBlank()) {
            return gebietBezeichnung + " – " + bandJahrAnzeige;
        }

        return gebietBezeichnung;
    }
}