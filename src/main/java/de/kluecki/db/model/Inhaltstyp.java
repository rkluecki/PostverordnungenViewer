package de.kluecki.db.model;

public class Inhaltstyp {

    private final int inhaltstypID;
    private final String bezeichnung;

    public Inhaltstyp(int inhaltstypID, String bezeichnung) {
        this.inhaltstypID = inhaltstypID;
        this.bezeichnung = bezeichnung;
    }

    public int getInhaltstypID() {
        return inhaltstypID;
    }

    public String getBezeichnung() {
        return bezeichnung;
    }

    @Override
    public String toString() {
        return bezeichnung;
    }
}