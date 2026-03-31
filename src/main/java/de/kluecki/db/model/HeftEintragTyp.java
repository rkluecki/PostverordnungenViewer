package de.kluecki.db.model;

public class HeftEintragTyp {

    private int heftEintragTypID;
    private String bezeichnung;

    public HeftEintragTyp() {
    }

    public HeftEintragTyp(int heftEintragTypID, String bezeichnung) {
        this.heftEintragTypID = heftEintragTypID;
        this.bezeichnung = bezeichnung;
    }

    public int getHeftEintragTypID() {
        return heftEintragTypID;
    }

    public void setHeftEintragTypID(int heftEintragTypID) {
        this.heftEintragTypID = heftEintragTypID;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    public void setBezeichnung(String bezeichnung) {
        this.bezeichnung = bezeichnung;
    }

    @Override
    public String toString() {
        return bezeichnung != null ? bezeichnung : "";
    }
}