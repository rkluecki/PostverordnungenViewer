package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.Veroeffentlichung;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

public class VeroeffentlichungRepository {

    public List<Veroeffentlichung> findByBand(String band, String gebiet) {
        List<Veroeffentlichung> liste = new ArrayList<>();

        // Übergang:
        // Hier später echte SQL auf Quelle(EbeneTyp=VEROEFFENTLICHUNG)
        // Für jetzt kannst du zunächst vorhandene Datenquelle anbinden.

        return liste;
    }
}