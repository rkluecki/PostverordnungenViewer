package de.kluecki.db;

public class InhaltTabellenEintrag {

    private String nr;
    private String titel;
    private String seite;
    private String typ;
    private String beschreibung;

    public InhaltTabellenEintrag(String nr, String titel, String seite) {
        this.nr = nr;
        this.titel = titel;
        this.seite = seite;
        this.typ = "";
        this.beschreibung = "";
    }

    public InhaltTabellenEintrag(String nr,
                                 String titel,
                                 String seite,
                                 String typ,
                                 String beschreibung) {

        this.nr = nr;
        this.titel = titel;
        this.seite = seite;
        this.typ = typ;
        this.beschreibung = beschreibung;
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

    public String getTyp() {
        return typ;
    }

    public void setTyp(String typ) {
        this.typ = typ;
    }

    public String getBeschreibung() {
        return beschreibung;
    }

    public void setBeschreibung(String beschreibung) {
        this.beschreibung = beschreibung;
    }
}