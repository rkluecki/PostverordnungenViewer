package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;

public class OcrPruefentscheidungRepository {

    public void speichernOderAktualisieren(
            int bandID,
            int bildIndex,
            String entscheidungsart,
            String bemerkung,
            String gepruefteQuelle,
            boolean istErledigt
    ) {

        String sql = """
                MERGE dbo.OcrPruefentscheidung AS ziel
                USING (
                    SELECT
                        ? AS BandID,
                        ? AS BildIndex
                ) AS quelle
                    ON ziel.BandID = quelle.BandID
                   AND ziel.BildIndex = quelle.BildIndex

                WHEN MATCHED THEN
                    UPDATE SET
                        Entscheidungsart = ?,
                        Bemerkung = ?,
                        GepruefteQuelle = ?,
                        IstErledigt = ?,
                        GeprueftAm = SYSDATETIME()

                WHEN NOT MATCHED THEN
                    INSERT
                    (
                        BandID,
                        BildIndex,
                        Entscheidungsart,
                        Bemerkung,
                        GepruefteQuelle,
                        IstErledigt
                    )
                    VALUES
                    (
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        ?
                    );
                """;

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, bandID);
            statement.setInt(2, bildIndex);

            statement.setString(3, entscheidungsart);
            statement.setString(4, leerZuNull(bemerkung));
            statement.setString(5, leerZuNull(gepruefteQuelle));
            statement.setBoolean(6, istErledigt);

            statement.setInt(7, bandID);
            statement.setInt(8, bildIndex);
            statement.setString(9, entscheidungsart);
            statement.setString(10, leerZuNull(bemerkung));
            statement.setString(11, leerZuNull(gepruefteQuelle));
            statement.setBoolean(12, istErledigt);

            statement.executeUpdate();

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Die OCR-Prüfentscheidung für BandID "
                            + bandID
                            + ", Bildindex "
                            + bildIndex
                            + " konnte nicht gespeichert werden.",
                    ex
            );
        }
    }

    private String leerZuNull(String wert) {

        if (wert == null || wert.isBlank()) {
            return null;
        }

        return wert.trim();
    }
}