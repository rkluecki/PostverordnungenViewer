package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.OcrPruefungEintrag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OcrPruefungRepository {

    public List<OcrPruefungEintrag> pruefeBand(int bandID) {

        String sql = """
                SELECT
                    sm.BandID,
                    sm.BildIndex,
                    sm.Dateiname,
                    sm.LogischeSeite,
                    so.SeitenOCRID,
                    so.OCRQuelle,
                    so.OCRFormat,
                    CASE
                        WHEN so.SeitenOCRID IS NULL
                            THEN 'Kein OCR-Datensatz'
                        WHEN so.OCRText IS NULL
                             OR LTRIM(RTRIM(so.OCRText)) = ''
                            THEN 'OCR leer'
                        ELSE 'OCR vorhanden'
                    END AS OCRStatus
                FROM dbo.SeitenMapping sm
                LEFT JOIN dbo.SeitenOCR so
                    ON so.BandID = sm.BandID
                   AND so.BildIndex = sm.BildIndex
                WHERE sm.BandID = ?
                  AND (
                        so.SeitenOCRID IS NULL
                        OR so.OCRText IS NULL
                        OR LTRIM(RTRIM(so.OCRText)) = ''
                      )
                ORDER BY sm.BildIndex;
                """;

        List<OcrPruefungEintrag> ergebnisse = new ArrayList<>();

        try (
                Connection connection = DatabaseConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, bandID);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    OcrPruefungEintrag eintrag =
                            new OcrPruefungEintrag();

                    eintrag.setBandID(
                            resultSet.getInt("BandID")
                    );

                    eintrag.setBildIndex(
                            resultSet.getInt("BildIndex")
                    );

                    eintrag.setDateiname(
                            resultSet.getString("Dateiname")
                    );

                    eintrag.setLogischeSeite(
                            resultSet.getString("LogischeSeite")
                    );

                    int seitenOCRID =
                            resultSet.getInt("SeitenOCRID");

                    if (resultSet.wasNull()) {
                        eintrag.setSeitenOCRID(null);
                    } else {
                        eintrag.setSeitenOCRID(seitenOCRID);
                    }

                    eintrag.setOcrQuelle(
                            resultSet.getString("OCRQuelle")
                    );

                    eintrag.setOcrFormat(
                            resultSet.getString("OCRFormat")
                    );

                    eintrag.setOcrStatus(
                            resultSet.getString("OCRStatus")
                    );

                    ergebnisse.add(eintrag);
                }
            }

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Die OCR-Prüfung für BandID "
                            + bandID
                            + " ist fehlgeschlagen.",
                    ex
            );
        }

        return ergebnisse;
    }
}