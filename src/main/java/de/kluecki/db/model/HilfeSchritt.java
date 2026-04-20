package de.kluecki.db.model;

public class HilfeSchritt {

    private int hilfeSchrittID;
    private int hilfeThemaID;
    private int reihenfolge;
    private String text;
    private boolean istAktiv;

    public HilfeSchritt() {}

    public HilfeSchritt(int hilfeSchrittID, int hilfeThemaID, int reihenfolge, String text, boolean istAktiv) {
        this.hilfeSchrittID = hilfeSchrittID;
        this.hilfeThemaID = hilfeThemaID;
        this.reihenfolge = reihenfolge;
        this.text = text;
        this.istAktiv = istAktiv;
    }

    public int getHilfeSchrittID() {
        return hilfeSchrittID;
    }

    public void setHilfeSchrittID(int hilfeSchrittID) {
        this.hilfeSchrittID = hilfeSchrittID;
    }

    public int getHilfeThemaID() {
        return hilfeThemaID;
    }

    public void setHilfeThemaID(int hilfeThemaID) {
        this.hilfeThemaID = hilfeThemaID;
    }

    public int getReihenfolge() {
        return reihenfolge;
    }

    public void setReihenfolge(int reihenfolge) {
        this.reihenfolge = reihenfolge;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public boolean isIstAktiv() {
        return istAktiv;
    }

    public void setIstAktiv(boolean istAktiv) {
        this.istAktiv = istAktiv;
    }
}
