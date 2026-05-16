package de.kluecki.ocr;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.SeitenOCR;
import de.kluecki.db.repository.SeitenOCRRepository;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;

/**
 * Service zum Herunterladen und Importieren von OCR-Daten.
 *
 * Aktueller Schwerpunkt:
 * - BSB/MDZ-OCR-API
 * - hOCR herunterladen
 * - hOCR in einfachen Text umwandeln
 * - OCRText pro Band + Dateiname in dbo.SeitenOCR speichern
 *
 * Wichtig:
 * Es wird NICHT blind durchnummeriert.
 * Die vorhandenen Einträge aus dbo.SeitenMapping sind die sichere Steuerliste.
 */
public class OcrDownloadService {

    private final SeitenOCRRepository seitenOCRRepository = new SeitenOCRRepository();

    public OcrDownloadErgebnis downloadUndImportiereOcrFuerBand(
            OcrArchivTyp archivTyp,
            int bandId,
            String objectId,
            Consumer<String> progressCallback
    ) {

        if (archivTyp == OcrArchivTyp.BSB_MDZ) {
            return downloadUndImportiereBsbOcrFuerBand(
                    bandId,
                    objectId,
                    progressCallback
            );
        }

        String meldung = "OCR-Archivtyp wird noch nicht unterstützt: " + archivTyp;

        meldeFortschritt(progressCallback, meldung);

        return new OcrDownloadErgebnis(
                bandId,
                objectId,
                0,
                0,
                0,
                0,
                false,
                meldung
        );
    }

    public OcrDownloadErgebnis downloadUndImportiereBsbOcrFuerBand(int bandId, String objectId) {
        return downloadUndImportiereBsbOcrFuerBand(
                bandId,
                objectId,
                System.out::println
        );
    }

    public OcrDownloadErgebnis downloadUndImportiereBsbOcrFuerBand(
            int bandId,
            String objectId,
            Consumer<String> progressCallback
    ) {

        int erfolgreich = 0;
        int ohneOcr = 0;
        int fehler = 0;

        List<SeitenMappingInfo> seiten = ladeSeitenMappings(bandId);

        if (seiten.isEmpty()) {

            String meldung = "Kein SeitenMapping für BandID "
                    + bandId
                    + " vorhanden. Bitte zuerst das Grundmapping erstellen. OCR-Import wurde nicht gestartet.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    objectId,
                    0,
                    0,
                    0,
                    0,
                    false,
                    meldung
            );
        }

        for (int index = 0; index < seiten.size(); index++) {

            SeitenMappingInfo mappingInfo = seiten.get(index);
            int aktuelleNummer = index + 1;

            try {
                String pageNum = ermittlePageNumAusDateiname(mappingInfo.dateiname());

                if (pageNum == null || pageNum.isBlank()) {
                    fehler++;
                    continue;
                }

                String url = "https://api.digitale-sammlungen.de/ocr/"
                        + objectId
                        + "/"
                        + pageNum;

                String hocr = ladeHocr(url);

                if (hocr == null || hocr.isBlank()) {
                    ohneOcr++;

                    meldeFortschritt(progressCallback,
                            aktuelleNummer + " / " + seiten.size()
                                    + " ohne hOCR: " + mappingInfo.dateiname()
                    );

                    continue;
                }

                String text = extrahiereTextAusHocr(hocr);

                if (text == null || text.isBlank()) {
                    ohneOcr++;

                    meldeFortschritt(progressCallback,
                            aktuelleNummer + " / " + seiten.size()
                                    + " ohne Text: " + mappingInfo.dateiname()
                    );

                    continue;
                }

                SeitenOCR ocr = new SeitenOCR();
                ocr.setBandID(bandId);
                ocr.setBildIndex(mappingInfo.bildIndex());
                ocr.setDateiname(mappingInfo.dateiname());
                ocr.setLogischeSeite(mappingInfo.logischeSeite());
                ocr.setOcrText(text);
                ocr.setOcrQuelle("BSB/MDZ Digitale Sammlungen / " + objectId);
                ocr.setOcrFormat("hOCR");

                seitenOCRRepository.insertOrUpdate(ocr);

                erfolgreich++;

                meldeFortschritt(progressCallback,
                        aktuelleNummer + " / " + seiten.size()
                                + " gespeichert: " + mappingInfo.dateiname()
                                + " | Zeichen: " + text.length()
                );

                // Kleine Pause, damit die API nicht unnötig hart angefragt wird.
                Thread.sleep(250);

            } catch (Exception e) {
                fehler++;

                meldeFortschritt(progressCallback,
                        aktuelleNummer + " / " + seiten.size()
                                + " FEHLER: " + mappingInfo.dateiname()
                );

                e.printStackTrace();
            }
        }

        String meldung = "OCR-Download abgeschlossen.";

        return new OcrDownloadErgebnis(
                bandId,
                objectId,
                seiten.size(),
                erfolgreich,
                ohneOcr,
                fehler,
                true,
                meldung
        );
    }

    private List<SeitenMappingInfo> ladeSeitenMappings(int bandId) {

        List<SeitenMappingInfo> result = new ArrayList<>();

        String sql = """
                SELECT
                    BildIndex,
                    Dateiname,
                    LogischeSeite
                FROM dbo.SeitenMapping
                WHERE BandID = ?
                ORDER BY BildIndex
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);

            try (ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    int bildIndex = rs.getInt("BildIndex");
                    String dateiname = rs.getString("Dateiname");
                    String logischeSeite = rs.getString("LogischeSeite");

                    result.add(new SeitenMappingInfo(
                            bildIndex,
                            dateiname,
                            logischeSeite
                    ));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    private String ermittlePageNumAusDateiname(String dateiname) {

        if (dateiname == null || dateiname.isBlank()) {
            return null;
        }

        int punktIndex = dateiname.lastIndexOf('.');

        if (punktIndex <= 0) {
            return dateiname;
        }

        return dateiname.substring(0, punktIndex);
    }

    private String ladeHocr(String url) throws Exception {

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() != 200) {
            return null;
        }

        return response.body();
    }

    private String extrahiereTextAusHocr(String hocr) {

        StringBuilder result = new StringBuilder();

        Pattern lineStartPattern = Pattern.compile(
                "<[^>]*class=[\"'][^\"']*ocr_line[^\"']*[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE
        );

        Matcher lineStartMatcher = lineStartPattern.matcher(hocr);

        List<Integer> lineStarts = new ArrayList<>();

        while (lineStartMatcher.find()) {
            lineStarts.add(lineStartMatcher.start());
        }

        for (int i = 0; i < lineStarts.size(); i++) {

            int start = lineStarts.get(i);
            int end;

            if (i + 1 < lineStarts.size()) {
                end = lineStarts.get(i + 1);
            } else {
                end = hocr.length();
            }

            String lineHtml = hocr.substring(start, end);
            String lineText = extrahiereWoerterAusHtml(lineHtml);

            if (!lineText.isBlank()) {
                result.append(lineText).append(System.lineSeparator());
            }
        }

        if (!result.isEmpty()) {
            return result.toString().trim();
        }

        return htmlZuText(hocr).trim();
    }

    private String extrahiereWoerterAusHtml(String html) {

        StringBuilder line = new StringBuilder();

        Pattern wordPattern = Pattern.compile(
                "<[^>]*class=[\"'][^\"']*ocrx_word[^\"']*[\"'][^>]*>(.*?)</[^>]+>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher wordMatcher = wordPattern.matcher(html);

        while (wordMatcher.find()) {
            String word = htmlZuText(wordMatcher.group(1));

            if (!word.isBlank()) {
                if (!line.isEmpty()) {
                    line.append(" ");
                }

                line.append(word);
            }
        }

        if (!line.isEmpty()) {
            return line.toString();
        }

        return htmlZuText(html);
    }

    private String htmlZuText(String html) {

        String text = html
                .replaceAll("(?is)<script.*?</script>", " ")
                .replaceAll("(?is)<style.*?</style>", " ")
                .replaceAll("(?is)<br\\s*/?>", "\n")
                .replaceAll("(?is)</p>", "\n")
                .replaceAll("(?is)</div>", "\n")
                .replaceAll("(?is)<[^>]+>", " ");

        text = dekodiereHtmlEntities(text);

        return text
                .replace("\r", "")
                .replaceAll("[ \\t\\x0B\\f]+", " ")
                .replaceAll(" *\\n *", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String dekodiereHtmlEntities(String text) {

        String result = text
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'");

        Pattern numericEntityPattern = Pattern.compile("&#(\\d+);");
        Matcher matcher = numericEntityPattern.matcher(result);

        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            int codePoint = Integer.parseInt(matcher.group(1));
            String replacement = Character.toString(codePoint);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);

        return buffer.toString();
    }

    private void meldeFortschritt(Consumer<String> progressCallback, String nachricht) {

        if (progressCallback != null) {
            progressCallback.accept(nachricht);
        }
    }

    private record SeitenMappingInfo(
            int bildIndex,
            String dateiname,
            String logischeSeite
    ) {
    }

    public record OcrDownloadErgebnis(
            int bandId,
            String objectId,
            int mappingSeiten,
            int erfolgreich,
            int ohneOcr,
            int fehler,
            boolean gestartet,
            String meldung
    ) {

    }
}