package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.OcrPruefungEintrag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OcrPruefungRepository {

    public List<OcrPruefungEintrag> pruefeBand(
            int bandID,
            String ansicht
    ) {

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
                END AS OCRStatus,

                pe.Entscheidungsart,
                pe.Bemerkung AS PruefBemerkung,
                pe.GepruefteQuelle,
                pe.IstErledigt

            FROM dbo.SeitenMapping sm

            LEFT JOIN dbo.SeitenOCR so
                ON so.BandID = sm.BandID
               AND so.BildIndex = sm.BildIndex

            LEFT JOIN dbo.OcrPruefentscheidung pe
                ON pe.BandID = sm.BandID
               AND pe.BildIndex = sm.BildIndex

            WHERE sm.BandID = ?

              AND (
                    so.SeitenOCRID IS NULL
                    OR so.OCRText IS NULL
                    OR LTRIM(RTRIM(so.OCRText)) = ''
                  )

              AND (
                    ? = 'ALLE'

                    OR (
                        ? = 'OFFEN'
                        AND (
                            pe.OcrPruefentscheidungID IS NULL
                            OR pe.IstErledigt = 0
                        )
                    )

                    OR (
                        ? = 'GEKLAERT'
                        AND pe.IstErledigt = 1
                    )
                  )

            ORDER BY sm.BildIndex;
            """;

        List<OcrPruefungEintrag> ergebnisse =
                new ArrayList<>();

        try (
                Connection connection =
                        DatabaseConnection.getConnection();

                PreparedStatement statement =
                        connection.prepareStatement(sql)
        ) {
            statement.setInt(1, bandID);
            statement.setString(2, ansicht);
            statement.setString(3, ansicht);
            statement.setString(4, ansicht);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

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

                    eintrag.setEntscheidungsart(
                            resultSet.getString(
                                    "Entscheidungsart"
                            )
                    );

                    eintrag.setPruefBemerkung(
                            resultSet.getString(
                                    "PruefBemerkung"
                            )
                    );

                    eintrag.setGepruefteQuelle(
                            resultSet.getString(
                                    "GepruefteQuelle"
                            )
                    );

                    boolean istErledigt =
                            resultSet.getBoolean(
                                    "IstErledigt"
                            );

                    if (resultSet.wasNull()) {
                        eintrag.setIstErledigt(null);
                    } else {
                        eintrag.setIstErledigt(
                                istErledigt
                        );
                    }

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