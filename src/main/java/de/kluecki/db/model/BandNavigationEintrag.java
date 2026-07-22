package de.kluecki.db.model;

public class BandNavigationEintrag {

    private final int quelleId;
    private final Integer parentQuelleId;
    private final int jahr;
    private final String titel;
    private final String bandOrdner;

    public BandNavigationEintrag(
            int quelleId,
            Integer parentQuelleId,
            int jahr,
            String titel,
            String bandOrdner) {

        this.quelleId = quelleId;
        this.parentQuelleId = parentQuelleId;
        this.jahr = jahr;
        this.titel = titel;
        this.bandOrdner = bandOrdner;
    }

    public int getQuelleId() {
        return quelleId;
    }

    public Integer getParentQuelleId() {
        return parentQuelleId;
    }

    public int getJahr() {
        return jahr;
    }

    public String getTitel() {
        return titel;
    }

    public String getBandOrdner() {
        return bandOrdner;
    }

    public boolean istUnterband() {
        return parentQuelleId != null;
    }

    public String getAnzeigeText() {
        if (istUnterband()) {
            return titel;
        }

        return String.valueOf(jahr);
    }

    @Override
    public String toString() {
        return getAnzeigeText();
    }
}