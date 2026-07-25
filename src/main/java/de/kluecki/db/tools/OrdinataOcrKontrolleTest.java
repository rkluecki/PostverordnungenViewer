package de.kluecki.db.tools;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.OrdinataOcrKontrolle;
import de.kluecki.db.repository.OrdinataOcrKontrolleRepository;

import java.sql.Connection;
import java.util.List;

public class OrdinataOcrKontrolleTest {

    public static void main(String[] args) {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            OrdinataOcrKontrolleRepository repository =
                    new OrdinataOcrKontrolleRepository(connection);

            OrdinataOcrKontrolle testEintrag =
                    new OrdinataOcrKontrolle();

            testEintrag.setQuelleID(null);
            testEintrag.setGebietBezeichnung("Reichspost");
            testEintrag.setJahrVon(1908);
            testEintrag.setJahrBis(1908);
            testEintrag.setBandJahrAnzeige("1908");

            testEintrag.setArchivName("Noch nicht ermittelt");
            testEintrag.setQuellenStatus("QUELLE_FEHLT");
            testEintrag.setErschliessbarkeit(
                    "DERZEIT_NICHT_ERSCHLIESSBAR"
            );
            testEintrag.setOcrStatus("OCR_FEHLT");
            testEintrag.setImportStatus("NICHT_ANGELEGT");
            testEintrag.setPruefStatus("NICHT_GEPRUEFT");
            testEintrag.setPrioritaet("SEHR_HOCH");

            testEintrag.setNaechsterSchritt("Quelle suchen");
            testEintrag.setBemerkung(
                    "Automatisch erzeugter Repository-Test"
            );
            testEintrag.setIstErledigt(false);

            repository.insert(testEintrag);

            System.out.println(
                    "Testeintrag wurde innerhalb der Transaktion gespeichert."
            );

            List<OrdinataOcrKontrolle> eintraege =
                    repository.findAll();

            System.out.println(
                    "Gefundene Kontrolllisteneinträge: "
                            + eintraege.size()
            );

            for (OrdinataOcrKontrolle eintrag : eintraege) {

                if ("Automatisch erzeugter Repository-Test"
                        .equals(eintrag.getBemerkung())) {

                    System.out.println();
                    System.out.println("Testeintrag gefunden:");
                    System.out.println(
                            "ID: "
                                    + eintrag
                                    .getOrdinataOcrKontrolleID()
                    );
                    System.out.println(
                            "Gebiet: "
                                    + eintrag.getGebietBezeichnung()
                    );
                    System.out.println(
                            "Band/Jahr: "
                                    + eintrag.getBandJahrAnzeige()
                    );
                    System.out.println(
                            "Priorität: "
                                    + eintrag.getPrioritaet()
                    );
                    System.out.println(
                            "OCR-Status: "
                                    + eintrag.getOcrStatus()
                    );
                    System.out.println(
                            "Erstellt am: "
                                    + eintrag.getErstelltAm()
                    );
                }
            }

            connection.rollback();

            System.out.println();
            System.out.println(
                    "Rollback ausgeführt. "
                            + "Der Testeintrag wurde nicht dauerhaft gespeichert."
            );

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}