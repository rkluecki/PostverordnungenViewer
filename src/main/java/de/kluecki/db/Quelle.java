package de.kluecki.db;

public class Quelle {

    private int quelleID;
    private Integer parentQuelleID;
    private int quelleTypID;
    private String ebeneTyp;
    private String titel;
    private Integer seiteVon;
    private Integer seiteBis;

    public Quelle(int quelleID,
                  Integer parentQuelleID,
                  int quelleTypID,
                  String ebeneTyp,
                  String titel,
                  Integer seiteVon,
                  Integer seiteBis) {
        this.quelleID = quelleID;
        this.parentQuelleID = parentQuelleID;
        this.quelleTypID = quelleTypID;
        this.ebeneTyp = ebeneTyp;
        this.titel = titel;
        this.seiteVon = seiteVon;
        this.seiteBis = seiteBis;
    }

    public int getQuelleID() {
        return quelleID;
    }

    public Integer getParentQuelleID() {
        return parentQuelleID;
    }

    public int getQuelleTypID() {
        return quelleTypID;
    }

    public String getEbeneTyp() {
        return ebeneTyp;
    }

    public String getTitel() {
        return titel;
    }

    public Integer getSeiteVon() {
        return seiteVon;
    }

    public Integer getSeiteBis() {
        return seiteBis;
    }

    @Override
    public String toString() {
        return quelleID + " | " + ebeneTyp + " | " + titel
                + " | Seiten: " + seiteVon + "-" + seiteBis
                + " | TypID: " + quelleTypID
                + " | Parent: " + parentQuelleID;
    }
}