package de.kluecki.db;

public class Quelle {

    private int quelleID;
    private Integer parentQuelleID;
    private int quelleTypID;
    private String titel;

    public Quelle(int quelleID, Integer parentQuelleID, int quelleTypID, String titel) {
        this.quelleID = quelleID;
        this.parentQuelleID = parentQuelleID;
        this.quelleTypID = quelleTypID;
        this.titel = titel;
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

    public String getTitel() {
        return titel;
    }

    @Override
    public String toString() {
        return quelleID + " | " + titel + " | TypID: " + quelleTypID + " | Parent: " + parentQuelleID;
    }
}