package de.kluecki.db.model;

public class SeitenMapping {

    private int mappingID;
    private int bandID;
    private int bildIndex;
    private String logischeSeite;
    private String dateiname;

    public SeitenMapping() {
    }

    public SeitenMapping(int mappingID, int bandID, int bildIndex, String logischeSeite) {
        this.mappingID = mappingID;
        this.bandID = bandID;
        this.bildIndex = bildIndex;
        this.logischeSeite = logischeSeite;
    }

    public int getMappingID() {
        return mappingID;
    }

    public void setMappingID(int mappingID) {
        this.mappingID = mappingID;
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

    public String getLogischeSeite() {
        return logischeSeite;
    }

    public void setLogischeSeite(String logischeSeite) {
        this.logischeSeite = logischeSeite;
    }

    public String getDateiname() {
        return dateiname;
    }

    public void setDateiname(String dateiname) {
        this.dateiname = dateiname;
    }

    @Override
    public String toString() {
        return "SeitenMapping{" +
                "mappingID=" + mappingID +
                ", bandID=" + bandID +
                ", bildIndex=" + bildIndex +
                ", logischeSeite='" + logischeSeite + '\'' +
                '}';
    }
}