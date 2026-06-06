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

    private static final int STANDARD_SUCH_LIMIT = 1000;
    private static final int MAX_SUCH_LIMIT = 5000;

    private int bereinigeOffset(int offset) {
        return Math.max(0, offset);
    }

    private int bereinigeLimit(int limit) {
        if (limit <= 0) {
            return STANDARD_SUCH_LIMIT;
        }

        return Math.min(limit, MAX_SUCH_LIMIT);
    }

    public SeitenOCR findByBandIdAndDateiname(int bandId, String dateiname) {

        String sql = """
                SELECT
                    SeitenOCRID,
                    BandID,
                    BildIndex,
                    Dateiname,
                    LogischeSeite,
                    OCRText,
                    OCRTextKorrigiert,
                    OCRKorrekturStatus,
                    OCRQuelle,
                    OCRFormat,
                    ErstelltAm,
                    GeaendertAm,
                    KorrigiertAm,
                    GeprueftAm
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
                    ocr.setOcrTextKorrigiert(rs.getString("OCRTextKorrigiert"));
                    ocr.setOcrKorrekturStatus(rs.getString("OCRKorrekturStatus"));
                    ocr.setOcrQuelle(rs.getString("OCRQuelle"));
                    ocr.setOcrFormat(rs.getString("OCRFormat"));

                    if (rs.getTimestamp("ErstelltAm") != null) {
                        ocr.setErstelltAm(rs.getTimestamp("ErstelltAm").toLocalDateTime());
                    }

                    if (rs.getTimestamp("GeaendertAm") != null) {
                        ocr.setGeaendertAm(rs.getTimestamp("GeaendertAm").toLocalDateTime());
                    }

                    if (rs.getTimestamp("KorrigiertAm") != null) {
                        ocr.setKorrigiertAm(rs.getTimestamp("KorrigiertAm").toLocalDateTime());
                    }

                    if (rs.getTimestamp("GeprueftAm") != null) {
                        ocr.setGeprueftAm(rs.getTimestamp("GeprueftAm").toLocalDateTime());
                    }

                    return ocr;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public boolean existsByBandIdAndDateiname(int bandId, String dateiname) {

        String sql = """
            SELECT 1
            FROM dbo.SeitenOCR
            WHERE BandID = ?
              AND Dateiname = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);
            stmt.setString(2, dateiname);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<SeitenOCRSuchtreffer> sucheOcrText(int bandId, String suchbegriff, String suchart) {
        return sucheOcrText(bandId, suchbegriff, suchart, 0, STANDARD_SUCH_LIMIT);
    }

    public List<SeitenOCRSuchtreffer> sucheOcrText(
            int bandId,
            String suchbegriff,
            String suchart,
            int offset,
            int limit
    ) {

        List<SeitenOCRSuchtreffer> treffer = new ArrayList<>();

        if (suchbegriff == null || suchbegriff.trim().isBlank()) {
            return treffer;
        }

        int offsetBereinigt = bereinigeOffset(offset);
        int limitBereinigt = bereinigeLimit(limit);

        String sql = """
            SELECT
                SeitenOCRID,
                BandID,
                BildIndex,
                Dateiname,
                LogischeSeite,
                OCRQuelle,
                OCRFormat,
                OCRText,
                OCRTextKorrigiert
            FROM dbo.SeitenOCR
            WHERE BandID = ?
              AND (
                    OCRText LIKE ?
                    OR OCRTextKorrigiert LIKE ?
                  )
            ORDER BY BildIndex
            OFFSET ? ROWS
            FETCH NEXT ? ROWS ONLY
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String suchbegriffBereinigt = suchbegriff.trim();
            String muster = baueLikeMuster(suchbegriffBereinigt, suchart);

            stmt.setInt(1, bandId);
            stmt.setString(2, muster);
            stmt.setString(3, muster);
            stmt.setInt(4, offsetBereinigt);
            stmt.setInt(5, limitBereinigt);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    SeitenOCRSuchtreffer suchtreffer = new SeitenOCRSuchtreffer();

                    String ocrText = rs.getString("OCRText");
                    String ocrTextKorrigiert = rs.getString("OCRTextKorrigiert");

                    boolean trefferOriginal =
                            passtSuchbegriff(ocrText, suchbegriffBereinigt, suchart);

                    boolean trefferKorrigiert =
                            passtSuchbegriff(ocrTextKorrigiert, suchbegriffBereinigt, suchart);

                    if (!trefferOriginal && !trefferKorrigiert) {
                        continue;
                    }

                    String trefferArt;
                    String textFuerAusschnitt;

                    if (trefferOriginal && trefferKorrigiert) {
                        trefferArt = "Original + korrigiert";
                        textFuerAusschnitt = ocrTextKorrigiert;
                    } else if (trefferKorrigiert) {
                        trefferArt = "Korrigierte Fassung";
                        textFuerAusschnitt = ocrTextKorrigiert;
                    } else {
                        trefferArt = "Original-OCR";
                        textFuerAusschnitt = ocrText;
                    }

                    suchtreffer.setSeitenOCRID(rs.getInt("SeitenOCRID"));
                    suchtreffer.setBandID(rs.getInt("BandID"));
                    suchtreffer.setBildIndex(rs.getInt("BildIndex"));
                    suchtreffer.setDateiname(rs.getString("Dateiname"));
                    suchtreffer.setLogischeSeite(rs.getString("LogischeSeite"));
                    suchtreffer.setOcrQuelle(rs.getString("OCRQuelle"));
                    suchtreffer.setOcrFormat(rs.getString("OCRFormat"));

                    suchtreffer.setTrefferArt(trefferArt);
                    suchtreffer.setSuchbegriff(suchbegriffBereinigt);
                    suchtreffer.setSuchart(suchart);
                    suchtreffer.setTextAusschnitt(
                            erstelleTextAusschnitt(textFuerAusschnitt, suchbegriffBereinigt, suchart)
                    );

                    treffer.add(suchtreffer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return treffer;
    }

    public List<SeitenOCRSuchtreffer> sucheOcrTextImGebiet(String gebiet, String suchbegriff, String suchart) {
        return sucheOcrTextImGebiet(gebiet, suchbegriff, suchart, 0, STANDARD_SUCH_LIMIT);
    }

    public List<SeitenOCRSuchtreffer> sucheOcrTextImGebiet(
            String gebiet,
            String suchbegriff,
            String suchart,
            int offset,
            int limit
    ) {

        List<SeitenOCRSuchtreffer> treffer = new ArrayList<>();

        if (gebiet == null || gebiet.trim().isBlank()) {
            return treffer;
        }

        if (suchbegriff == null || suchbegriff.trim().isBlank()) {
            return treffer;
        }

        int offsetBereinigt = bereinigeOffset(offset);
        int limitBereinigt = bereinigeLimit(limit);

        String sql = """
        SELECT
            s.SeitenOCRID,
            s.BandID,
            s.BildIndex,
            s.Dateiname,
            s.LogischeSeite,
            s.OCRQuelle,
            s.OCRFormat,
            s.OCRText,
            s.OCRTextKorrigiert,
            q.Land,
            q.Jahr,
            q.JahrVon,
            q.JahrBis
        FROM dbo.SeitenOCR s
        INNER JOIN dbo.Quelle q
            ON q.QuelleID = s.BandID
        WHERE q.EbeneTyp = 'BAND'
          AND q.Land = ?
          AND (
                s.OCRText LIKE ?
                OR s.OCRTextKorrigiert LIKE ?
              )
        ORDER BY
            q.Jahr,
            q.JahrVon,
            s.BildIndex
        OFFSET ? ROWS
        FETCH NEXT ? ROWS ONLY
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String suchbegriffBereinigt = suchbegriff.trim();
            String muster = baueLikeMuster(suchbegriffBereinigt, suchart);

            stmt.setString(1, gebiet.trim());
            stmt.setString(2, muster);
            stmt.setString(3, muster);
            stmt.setInt(4, offsetBereinigt);
            stmt.setInt(5, limitBereinigt);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    SeitenOCRSuchtreffer suchtreffer = new SeitenOCRSuchtreffer();

                    String ocrText = rs.getString("OCRText");
                    String ocrTextKorrigiert = rs.getString("OCRTextKorrigiert");

                    boolean trefferOriginal =
                            passtSuchbegriff(ocrText, suchbegriffBereinigt, suchart);

                    boolean trefferKorrigiert =
                            passtSuchbegriff(ocrTextKorrigiert, suchbegriffBereinigt, suchart);

                    if (!trefferOriginal && !trefferKorrigiert) {
                        continue;
                    }

                    String trefferArt;
                    String textFuerAusschnitt;

                    if (trefferOriginal && trefferKorrigiert) {
                        trefferArt = "Original + korrigiert";
                        textFuerAusschnitt = ocrTextKorrigiert;
                    } else if (trefferKorrigiert) {
                        trefferArt = "Korrigierte Fassung";
                        textFuerAusschnitt = ocrTextKorrigiert;
                    } else {
                        trefferArt = "Original-OCR";
                        textFuerAusschnitt = ocrText;
                    }

                    Integer jahr = (Integer) rs.getObject("Jahr");
                    Integer jahrVon = (Integer) rs.getObject("JahrVon");
                    Integer jahrBis = (Integer) rs.getObject("JahrBis");

                    suchtreffer.setSeitenOCRID(rs.getInt("SeitenOCRID"));
                    suchtreffer.setBandID(rs.getInt("BandID"));
                    suchtreffer.setBildIndex(rs.getInt("BildIndex"));
                    suchtreffer.setDateiname(rs.getString("Dateiname"));
                    suchtreffer.setLogischeSeite(rs.getString("LogischeSeite"));
                    suchtreffer.setOcrQuelle(rs.getString("OCRQuelle"));
                    suchtreffer.setOcrFormat(rs.getString("OCRFormat"));

                    suchtreffer.setGebiet(rs.getString("Land"));
                    suchtreffer.setBandAnzeige(baueBandAnzeige(jahr, jahrVon, jahrBis));

                    suchtreffer.setTrefferArt(trefferArt);
                    suchtreffer.setSuchbegriff(suchbegriffBereinigt);
                    suchtreffer.setSuchart(suchart);
                    suchtreffer.setTextAusschnitt(
                            erstelleTextAusschnitt(textFuerAusschnitt, suchbegriffBereinigt, suchart)
                    );

                    treffer.add(suchtreffer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return treffer;
    }

    public List<SeitenOCRSuchtreffer> sucheOcrTextAlleGebiete(String suchbegriff, String suchart) {
        return sucheOcrTextAlleGebiete(suchbegriff, suchart, 0, STANDARD_SUCH_LIMIT);
    }

    public List<SeitenOCRSuchtreffer> sucheOcrTextAlleGebiete(
            String suchbegriff,
            String suchart,
            int offset,
            int limit
    ) {

        List<SeitenOCRSuchtreffer> treffer = new ArrayList<>();

        if (suchbegriff == null || suchbegriff.trim().isBlank()) {
            return treffer;
        }

        int offsetBereinigt = bereinigeOffset(offset);
        int limitBereinigt = bereinigeLimit(limit);

        String sql = """
        SELECT
            s.SeitenOCRID,
            s.BandID,
            s.BildIndex,
            s.Dateiname,
            s.LogischeSeite,
            s.OCRQuelle,
            s.OCRFormat,
            s.OCRText,
            s.OCRTextKorrigiert,
            q.Land,
            q.Jahr,
            q.JahrVon,
            q.JahrBis
        FROM dbo.SeitenOCR s
        INNER JOIN dbo.Quelle q
            ON q.QuelleID = s.BandID
        WHERE q.EbeneTyp = 'BAND'
          AND (
                s.OCRText LIKE ?
                OR s.OCRTextKorrigiert LIKE ?
              )
        ORDER BY
            q.Land,
            q.Jahr,
            q.JahrVon,
            s.BildIndex
        OFFSET ? ROWS
        FETCH NEXT ? ROWS ONLY
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String suchbegriffBereinigt = suchbegriff.trim();
            String muster = baueLikeMuster(suchbegriffBereinigt, suchart);

            stmt.setString(1, muster);
            stmt.setString(2, muster);
            stmt.setInt(3, offsetBereinigt);
            stmt.setInt(4, limitBereinigt);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    SeitenOCRSuchtreffer suchtreffer = new SeitenOCRSuchtreffer();

                    String ocrText = rs.getString("OCRText");
                    String ocrTextKorrigiert = rs.getString("OCRTextKorrigiert");

                    boolean trefferOriginal =
                            passtSuchbegriff(ocrText, suchbegriffBereinigt, suchart);

                    boolean trefferKorrigiert =
                            passtSuchbegriff(ocrTextKorrigiert, suchbegriffBereinigt, suchart);

                    if (!trefferOriginal && !trefferKorrigiert) {
                        continue;
                    }

                    String trefferArt;
                    String textFuerAusschnitt;

                    if (trefferOriginal && trefferKorrigiert) {
                        trefferArt = "Original + korrigiert";
                        textFuerAusschnitt = ocrTextKorrigiert;
                    } else if (trefferKorrigiert) {
                        trefferArt = "Korrigierte Fassung";
                        textFuerAusschnitt = ocrTextKorrigiert;
                    } else {
                        trefferArt = "Original-OCR";
                        textFuerAusschnitt = ocrText;
                    }

                    Integer jahr = (Integer) rs.getObject("Jahr");
                    Integer jahrVon = (Integer) rs.getObject("JahrVon");
                    Integer jahrBis = (Integer) rs.getObject("JahrBis");

                    suchtreffer.setSeitenOCRID(rs.getInt("SeitenOCRID"));
                    suchtreffer.setBandID(rs.getInt("BandID"));
                    suchtreffer.setBildIndex(rs.getInt("BildIndex"));
                    suchtreffer.setDateiname(rs.getString("Dateiname"));
                    suchtreffer.setLogischeSeite(rs.getString("LogischeSeite"));
                    suchtreffer.setOcrQuelle(rs.getString("OCRQuelle"));
                    suchtreffer.setOcrFormat(rs.getString("OCRFormat"));

                    suchtreffer.setGebiet(rs.getString("Land"));
                    suchtreffer.setBandAnzeige(baueBandAnzeige(jahr, jahrVon, jahrBis));

                    suchtreffer.setTrefferArt(trefferArt);
                    suchtreffer.setSuchbegriff(suchbegriffBereinigt);
                    suchtreffer.setSuchart(suchart);
                    suchtreffer.setTextAusschnitt(
                            erstelleTextAusschnitt(textFuerAusschnitt, suchbegriffBereinigt, suchart)
                    );

                    treffer.add(suchtreffer);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return treffer;
    }

    private String baueBandAnzeige(Integer jahr, Integer jahrVon, Integer jahrBis) {

        if (jahrVon != null && jahrBis != null && !jahrVon.equals(jahrBis)) {
            return jahrVon + "-" + jahrBis;
        }

        if (jahr != null) {
            return String.valueOf(jahr);
        }

        if (jahrVon != null && jahrBis != null) {
            return jahrVon + "-" + jahrBis;
        }

        if (jahrVon != null) {
            return String.valueOf(jahrVon);
        }

        return "";
    }

    private String baueLikeMuster(String suchbegriff, String suchart) {

        if (suchbegriff == null) {
            return "";
        }

        String wert = suchbegriff.trim();

        if (suchart == null || suchart.isBlank()) {
            return "%" + wert + "%";
        }

        return switch (suchart) {
            case "exakt" -> "%" + wert + "%";
            case "beginnt mit" -> "%" + wert + "%";
            case "endet mit" -> "%" + wert + "%";
            case "Wildcard" -> "%" + wert + "%";
            case "enthält" -> "%" + wert + "%";
            default -> "%" + wert + "%";
        };
    }

    private boolean passtSuchbegriff(String text, String suchbegriff, String suchart) {

        if (text == null || text.isBlank()) {
            return false;
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return false;
        }

        String textKlein = text.toLowerCase();
        String suchbegriffKlein = suchbegriff.trim().toLowerCase();

        if (suchart == null || suchart.isBlank()) {
            return textKlein.contains(suchbegriffKlein);
        }

        return switch (suchart) {
            case "exakt" -> passtExakterBegriff(textKlein, suchbegriffKlein);
            case "beginnt mit" -> passtWortBeginntMit(text, suchbegriff);
            case "endet mit" -> passtWortEndetMit(text, suchbegriff);
            case "Wildcard" -> passtWildcardMuster(textKlein, suchbegriffKlein);
            case "enthält" -> textKlein.contains(suchbegriffKlein);
            default -> textKlein.contains(suchbegriffKlein);
        };
    }

    private boolean passtWortBeginntMit(String text, String suchbegriff) {

        if (text == null || text.isBlank()) {
            return false;
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return false;
        }

        String regex = "(?<![\\p{L}\\p{N}])"
                + java.util.regex.Pattern.quote(suchbegriff);

        return java.util.regex.Pattern
                .compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(text)
                .find();
    }

    private boolean passtWortEndetMit(String text, String suchbegriff) {

        if (text == null || text.isBlank()) {
            return false;
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return false;
        }

        String regex = java.util.regex.Pattern.quote(suchbegriff)
                + "(?![\\p{L}\\p{N}])";

        return java.util.regex.Pattern
                .compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(text)
                .find();
    }

    private boolean passtExakterBegriff(String text, String suchbegriff) {

        if (text == null || text.isBlank()) {
            return false;
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return false;
        }

        String regex = "(?<![\\p{L}\\p{N}])"
                + java.util.regex.Pattern.quote(suchbegriff)
                + "(?![\\p{L}\\p{N}])";

        return java.util.regex.Pattern
                .compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(text)
                .find();
    }

    private boolean passtWildcardMuster(String text, String muster) {

        if (text == null || text.isBlank()) {
            return false;
        }

        if (muster == null || muster.isBlank()) {
            return false;
        }

        String regex = sqlLikeMusterZuRegex(muster);

        return java.util.regex.Pattern
                .compile(regex, java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE)
                .matcher(text)
                .find();
    }

    private String sqlLikeMusterZuRegex(String muster) {

        StringBuilder regex = new StringBuilder();

        for (int i = 0; i < muster.length(); i++) {
            char zeichen = muster.charAt(i);

            if (zeichen == '%') {
                regex.append(".*");
            } else if (zeichen == '_') {
                regex.append(".");
            } else {
                regex.append(java.util.regex.Pattern.quote(String.valueOf(zeichen)));
            }
        }

        return regex.toString();
    }

    private String erstelleTextAusschnitt(String text, String suchbegriff, String suchart) {

        if (text == null || text.isBlank()) {
            return "";
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return text.length() > 160 ? text.substring(0, 160) + "..." : text;
        }

        int position = findeAusschnittPosition(text, suchbegriff, suchart);

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

    private int findeAusschnittPosition(String text, String suchbegriff, String suchart) {

        if (text == null || text.isBlank()) {
            return -1;
        }

        if (suchbegriff == null || suchbegriff.isBlank()) {
            return -1;
        }

        if ("exakt".equals(suchart)) {
            return findeRegexPosition(
                    text,
                    "(?<![\\p{L}\\p{N}])"
                            + java.util.regex.Pattern.quote(suchbegriff)
                            + "(?![\\p{L}\\p{N}])"
            );
        }

        if ("beginnt mit".equals(suchart)) {
            return findeRegexPosition(
                    text,
                    "(?<![\\p{L}\\p{N}])"
                            + java.util.regex.Pattern.quote(suchbegriff)
            );
        }

        if ("endet mit".equals(suchart)) {
            return findeRegexPosition(
                    text,
                    java.util.regex.Pattern.quote(suchbegriff)
                            + "(?![\\p{L}\\p{N}])"
            );
        }

        if ("Wildcard".equals(suchart)) {
            return findeRegexPosition(
                    text,
                    sqlLikeMusterZuRegexFuerAusschnitt(suchbegriff)
            );
        }

        return text.toLowerCase().indexOf(suchbegriff.toLowerCase());
    }

    private int findeRegexPosition(String text, String regex) {

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                regex,
                java.util.regex.Pattern.CASE_INSENSITIVE | java.util.regex.Pattern.UNICODE_CASE
        );

        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.start();
        }

        return -1;
    }

    private String sqlLikeMusterZuRegexFuerAusschnitt(String muster) {

        StringBuilder regex = new StringBuilder();

        for (int i = 0; i < muster.length(); i++) {
            char zeichen = muster.charAt(i);

            if (zeichen == '%') {
                regex.append("[\\p{L}\\p{N}]*");
            } else if (zeichen == '_') {
                regex.append("[\\p{L}\\p{N}]");
            } else {
                regex.append(java.util.regex.Pattern.quote(String.valueOf(zeichen)));
            }
        }

        return regex.toString();
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
                       OCRText = CASE
                          WHEN OCRText IS NULL OR LTRIM(RTRIM(OCRText)) = '' THEN ?
                          ELSE OCRText
                       END,
                       OCRQuelle = CASE
                          WHEN OCRQuelle IS NULL OR LTRIM(RTRIM(OCRQuelle)) = '' THEN ?
                          ELSE OCRQuelle
                       END,
                       OCRFormat = CASE
                          WHEN OCRFormat IS NULL OR LTRIM(RTRIM(OCRFormat)) = '' THEN ?
                          ELSE OCRFormat
                       END,
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

    public void updateKorrigiertenText(int seitenOCRID, String korrigierterText) {

        String sql = """
            UPDATE dbo.SeitenOCR
            SET
                OCRTextKorrigiert = ?,
                OCRKorrekturStatus = ?,
                KorrigiertAm = SYSUTCDATETIME(),
                GeaendertAm = SYSUTCDATETIME()
            WHERE SeitenOCRID = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, korrigierterText);

            if (korrigierterText == null || korrigierterText.trim().isBlank()) {
                stmt.setString(2, "leer");
            } else {
                stmt.setString(2, "korrigiert");
            }

            stmt.setInt(3, seitenOCRID);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}