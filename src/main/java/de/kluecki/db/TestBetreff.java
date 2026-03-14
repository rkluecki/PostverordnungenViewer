package de.kluecki.db;

import de.kluecki.db.model.VerordnungBetreff;
import de.kluecki.db.repository.VerordnungBetreffRepository;

import java.sql.Connection;

public class TestBetreff {

    public static void main(String[] args) {
        try (Connection connection = DatabaseConnection.getConnection()) {

            VerordnungBetreffRepository repo =
                    new VerordnungBetreffRepository(connection);

            VerordnungBetreff neu = new VerordnungBetreff();
            neu.setGebiet("Bayern");
            neu.setBandJahr("VA842");
            neu.setSeiteVon(20);
            neu.setSeiteBis(22);
            neu.setTitel("Test-Verordnung");
            neu.setBemerkung("Nur Testeintrag");

            repo.insert(neu);

            System.out.println("Testeintrag gespeichert.");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}