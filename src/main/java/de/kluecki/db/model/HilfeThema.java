package de.kluecki.db.model;

public class HilfeThema {

    private int hilfeThemaID;
    private String titel;
    private int sortierung;
    private boolean istAktiv;

    public HilfeThema() {}

    public HilfeThema(int hilfeThemaID, String titel, int sortierung, boolean istAktiv) {
        this.hilfeThemaID = hilfeThemaID;
        this.titel = titel;
        this.sortierung = sortierung;
        this.istAktiv = istAktiv;
    }

    public int getHilfeThemaID() {
        return hilfeThemaID;
    }

    public void setHilfeThemaID(int hilfeThemaID) {
        this.hilfeThemaID = hilfeThemaID;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public int getSortierung() {
        return sortierung;
    }

    public void setSortierung(int sortierung) {
        this.sortierung = sortierung;
    }

    public boolean isIstAktiv() {
        return istAktiv;
    }

    public void setIstAktiv(boolean istAktiv) {
        this.istAktiv = istAktiv;
    }

    @Override
    public String toString() {
        return titel;
    }
}
