package de.kluecki.db;

import de.kluecki.db.model.Inhaltstyp;

public class InhaltTabellenEintrag {

    private String nr;
    private String titel;
    private String seite;
    private Inhaltstyp typ;
    private String beschreibung;

    private String gebiet;
    private String bandJahr;
    private Integer seiteVon;
    private Integer seiteBis;

    public InhaltTabellenEintrag(String nr, String titel, String seite) {
        this.nr = nr;
        this.titel = titel;
        this.seite = seite;
        this.typ = null;
        this.beschreibung = "";
    }

    public InhaltTabellenEintrag(String nr,
                                 String titel,
                                 String seite,
                                 Inhaltstyp typ,
                                 String beschreibung) {

        this.nr = nr;
        this.titel = titel;
        this.seite = seite;
        this.typ = typ;
        this.beschreibung = beschreibung;
    }

    public InhaltTabellenEintrag(String nr,
                                 String titel,
                                 String seite,
                                 Inhaltstyp typ,
                                 String beschreibung,
                                 String gebiet,
                                 String bandJahr,
                                 Integer seiteVon,
                                 Integer seiteBis) {

        this.nr = nr;
        this.titel = titel;
        this.seite = seite;
        this.typ = typ;
        this.beschreibung = beschreibung;
        this.gebiet = gebiet;
        this.bandJahr = bandJahr;
        this.seiteVon = seiteVon;
        this.seiteBis = seiteBis;
    }

    public String getNr() {
        return nr;
    }

    public void setNr(String nr) {
        this.nr = nr;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getSeite() {
        return seite;
    }

    public void setSeite(String seite) {
        this.seite = seite;
    }

    public Inhaltstyp getTyp() {
        return typ;
    }

    public void setTyp(Inhaltstyp typ) {
        this.typ = typ;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }

    public String getGebiet() {
        return gebiet;
    }

    public void setGebiet(String gebiet) {
        this.gebiet = gebiet;
    }

    public String getBandJahr() {
        return bandJahr;
    }

    public void setBandJahr(String bandJahr) {
        this.bandJahr = bandJahr;
    }

    public Integer getSeiteVon() {
        return seiteVon;
    }

    public void setSeiteVon(Integer seiteVon) {
        this.seiteVon = seiteVon;
    }

    public Integer getSeiteBis() {
        return seiteBis;
    }

    public void setSeiteBis(Integer seiteBis) {
        this.seiteBis = seiteBis;
    }
}