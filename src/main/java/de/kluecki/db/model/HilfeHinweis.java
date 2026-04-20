package de.kluecki.db.model;

public class HilfeHinweis {

    private int hilfeHinweisID;
    private int hilfeSchrittID;
    private String typ;
    private String text;
    private boolean istAktiv;

    public HilfeHinweis() {}

    public HilfeHinweis(int hilfeHinweisID, int hilfeSchrittID, String typ, String text, boolean istAktiv) {
        this.hilfeHinweisID = hilfeHinweisID;
        this.hilfeSchrittID = hilfeSchrittID;
        this.typ = typ;
        this.text = text;
        this.istAktiv = istAktiv;
    }

    public int getHilfeHinweisID() {
        return hilfeHinweisID;
    }

    public void setHilfeHinweisID(int hilfeHinweisID) {
        this.hilfeHinweisID = hilfeHinweisID;
    }

    public int getHilfeSchrittID() {
        return hilfeSchrittID;
    }

    public void setHilfeSchrittID(int hilfeSchrittID) {
        this.hilfeSchrittID = hilfeSchrittID;
    }

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
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