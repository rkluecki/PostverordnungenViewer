package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.SeitenOCR;
import de.kluecki.db.model.SeitenOCRSuchtreffer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SeitenOCRRepository {

    public SeitenOCR findByBandIdAndDateiname(int bandId, String dateiname) {

        String sql = """
                SELECT
                    SeitenOCRID,
                    BandID,
                    BildIndex,
                    Dateiname,
                    LogischeSeite,
                    OCRText,
                    OCRQuelle,
                    OCRFormat,
                    ErstelltAm,
                    GeaendertAm
                FROM dbo.SeitenOCR
                WHERE BandID = ?
                  AND Dateiname = ?
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);
            stmt.setString(2, dateiname);

            try (ResultSet rs = stmt.executeQuery()) {

                if (rs.next()) {
                    SeitenOCR ocr = new SeitenOCR();

                    ocr.setSeitenOCRID(rs.getInt("SeitenOCRID"));
                    ocr.setBandID(rs.getInt("BandID"));
                    ocr.setBildIndex(rs.getInt("BildIndex"));
                    ocr.setDateiname(rs.getString("Dateiname"));
                    ocr.setLogischeSeite(rs.getString("LogischeSeite"));
                    ocr.setOcrText(rs.getString("OCRText"));
                    ocr.setOcrQuelle(rs.getString("OCRQuelle"));
                    ocr.setOcrFormat(rs.getString("OCRFormat"));

                    if (rs.getTimestamp("ErstelltAm") != null) {
                        ocr.setErstelltAm(rs.getTimestamp("ErstelltAm").toLocalDateTime());
                    }

                    if (rs.getTimestamp("GeaendertAm") != null) {
                        ocr.setGeaendertAm(rs.getTimestamp("GeaendertAm").toLocalDateTime());
                    }

                    return ocr;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public List<SeitenOCRSuchtreffer> sucheOcrText(int bandId, String suchbegriff) {

        List<SeitenOCRSuchtreffer> treffer = new ArrayList<>();

        if (suchbegriff == null || suchbegriff.trim().isBlank()) {
            return treffer;
        }

        String sql = """
            SELECT TOP 200
                SeitenOCRID,
                BandID,
                BildIndex,
                Dateiname,
                LogischeSeite,
                OCRQuelle,
                OCRFormat,
                OCRText
            FROM dbo.SeitenOCR
            WHERE BandID = ?
              AND OCRText LIKE ?
            ORDER BY BildIndex
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String muster = "%" + suchbegriff.trim() + "%";

            stmt.setInt(1, bandId);
            stmt.setString(2, muster);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    SeitenOCRSuchtreffer suchtreffer = new SeitenOCRSuchtreffer();

                    String ocrText = rs.getString("OCRText");

                    suchtreffer.setSeitenOCRID(rs.getInt("SeitenOCRID"));
                    suchtreffer.setBandID(rs.getInt("BandID"));
                    suchtreffer.setBildIndex(rs.getInt("BildIndex"));
                    suchtreffer.setDateiname(rs.getString("Dateiname"));
                    suchtreffer.setLogischeSeite(rs.getString("LogischeSeite"));
                    suchtreffer.setOcrQuelle(rs.getString("OCRQuelle"));
                    suchtreffer.setOcrFormat(rs.getString("OCRFormat"));
                    suchtreffer.setTextAusschnitt(erstelleTextAusschnitt(ocrText, suchbegriff.trim()));

                    treffer.add(suchtreffer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return treffer;
    }

    private String erstelleTextAusschnitt(String text, String suchbegriff) {

        if (text == null || text.isBlank()) {
            return "";
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return text.length() > 160 ? text.substring(0, 160) + "..." : text;
        }

        String textKlein = text.toLowerCase();
        String suchbegriffKlein = suchbegriff.toLowerCase();

        int position = textKlein.indexOf(suchbegriffKlein);

        if (position < 0) {
            return text.length() > 160 ? text.substring(0, 160) + "..." : text;
        }

        int start = Math.max(0, position - 70);
        int ende = Math.min(text.length(), position + suchbegriff.length() + 90);

        String ausschnitt = text.substring(start, ende)
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();

        if (start > 0) {
            ausschnitt = "... " + ausschnitt;
        }

        if (ende < text.length()) {
            ausschnitt = ausschnitt + " ...";
        }

        return ausschnitt;
    }

    public void insertOrUpdate(SeitenOCR ocr) {

        String sql = """
                IF EXISTS (
                    SELECT 1
                    FROM dbo.SeitenOCR
                    WHERE BandID = ?
                      AND Dateiname = ?
                )
                BEGIN
                    UPDATE dbo.SeitenOCR
                    SET
                        BildIndex = ?,
                        LogischeSeite = ?,
                        OCRText = ?,
                        OCRQuelle = ?,
                        OCRFormat = ?,
                        GeaendertAm = SYSUTCDATETIME()
                    WHERE BandID = ?
                      AND Dateiname = ?
                END
                ELSE
                BEGIN
                    INSERT INTO dbo.SeitenOCR
                    (
                        BandID,
                        BildIndex,
                        Dateiname,
                        LogischeSeite,
                        OCRText,
                        OCRQuelle,
                        OCRFormat
                    )
                    VALUES
                    (
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        ?,
                        ?
                    )
                END
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            int i = 1;

            // EXISTS-Prüfung
            stmt.setInt(i++, ocr.getBandID());
            stmt.setString(i++, ocr.getDateiname());

            // UPDATE-Werte
            stmt.setInt(i++, ocr.getBildIndex());
            stmt.setString(i++, ocr.getLogischeSeite());
            stmt.setString(i++, ocr.getOcrText());
            stmt.setString(i++, ocr.getOcrQuelle());
            stmt.setString(i++, ocr.getOcrFormat());
            stmt.setInt(i++, ocr.getBandID());
            stmt.setString(i++, ocr.getDateiname());

            // INSERT-Werte
            stmt.setInt(i++, ocr.getBandID());
            stmt.setInt(i++, ocr.getBildIndex());
            stmt.setString(i++, ocr.getDateiname());
            stmt.setString(i++, ocr.getLogischeSeite());
            stmt.setString(i++, ocr.getOcrText());
            stmt.setString(i++, ocr.getOcrQuelle());
            stmt.setString(i++, ocr.getOcrFormat());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}