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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Service zum Herunterladen und Importieren von OCR-Daten.
 *
 * Aktueller Schwerpunkt:
 * - BSB/MDZ-OCR-API
 * - BLB-Karlsruhe-OCR
 * - hOCR / ALTO herunterladen
 * - OCR-Text pro Band + Dateiname in dbo.SeitenOCR speichern
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
        return downloadUndImportiereOcrFuerBand(
                archivTyp,
                bandId,
                objectId,
                false,
                progressCallback
        );
    }

    public OcrDownloadErgebnis downloadUndImportiereOcrFuerBand(
            OcrArchivTyp archivTyp,
            int bandId,
            String objectId,
            boolean vorhandeneOcrUeberspringen,
            Consumer<String> progressCallback
    ) {

        if (archivTyp == OcrArchivTyp.BSB_MDZ) {
            return downloadUndImportiereBsbOcrFuerBand(
                    bandId,
                    objectId,
                    vorhandeneOcrUeberspringen,
                    progressCallback
            );
        }

        if (archivTyp == OcrArchivTyp.BLB_KARLSRUHE) {
            return downloadUndImportiereBlbOcrFuerBand(
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

    public OcrDownloadErgebnis downloadUndImportiereBlbOcrFuerManifestBereich(
            int bandId,
            int manifestIdVon,
            int manifestIdBis,
            Consumer<String> progressCallback
    ) {

        int startId = Math.min(manifestIdVon, manifestIdBis);
        int endId = Math.max(manifestIdVon, manifestIdBis);

        int gepruefteManifeste = 0;
        int passendeManifeste = 0;
        int uebersprungeneManifeste = 0;
        int erfolgreichGesamt = 0;
        int ohneOcrGesamt = 0;
        int fehlerGesamt = 0;

        meldeFortschritt(progressCallback, "BLB-Manifestbereich-Import gestartet.");
        meldeFortschritt(progressCallback, "Manifest-ID von: " + startId);
        meldeFortschritt(progressCallback, "Manifest-ID bis: " + endId);
        meldeFortschritt(progressCallback, "");

        for (int manifestId = startId; manifestId <= endId; manifestId++) {

            gepruefteManifeste++;

            meldeFortschritt(progressCallback, "==================================================");
            meldeFortschritt(progressCallback, "Prüfe BLB Manifest-ID: " + manifestId);
            meldeFortschritt(progressCallback, "==================================================");

            OcrDownloadErgebnis einzelErgebnis = downloadUndImportiereBlbOcrFuerBand(
                    bandId,
                    String.valueOf(manifestId),
                    progressCallback
            );

            if (einzelErgebnis.gestartet()) {
                passendeManifeste++;
                erfolgreichGesamt += einzelErgebnis.erfolgreich();
                ohneOcrGesamt += einzelErgebnis.ohneOcr();
                fehlerGesamt += einzelErgebnis.fehler();

                meldeFortschritt(progressCallback,
                        "Manifest-ID " + manifestId + " verarbeitet."
                );
            } else {
                uebersprungeneManifeste++;

                meldeFortschritt(progressCallback,
                        "Manifest-ID " + manifestId + " übersprungen: "
                                + einzelErgebnis.meldung()
                );
            }

            meldeFortschritt(progressCallback, "");

            try {
                Thread.sleep(800);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();

                String meldung = "BLB-Manifestbereich-Import wurde unterbrochen.";

                meldeFortschritt(progressCallback, meldung);

                return new OcrDownloadErgebnis(
                        bandId,
                        startId + "-" + endId,
                        gepruefteManifeste,
                        erfolgreichGesamt,
                        ohneOcrGesamt,
                        fehlerGesamt + 1,
                        false,
                        meldung
                );
            }
        }

        String meldung = "BLB-Manifestbereich-Import abgeschlossen. "
                + "Geprüfte Manifest-IDs: " + gepruefteManifeste
                + ", passende Manifeste: " + passendeManifeste
                + ", übersprungen: " + uebersprungeneManifeste
                + ", gespeicherte OCR-Seiten: " + erfolgreichGesamt
                + ", OCR nachholen (eigene OCR): " + ohneOcrGesamt
                + ", Fehler: " + fehlerGesamt;

        meldeFortschritt(progressCallback, meldung);

        return new OcrDownloadErgebnis(
                bandId,
                startId + "-" + endId,
                gepruefteManifeste,
                erfolgreichGesamt,
                ohneOcrGesamt,
                fehlerGesamt,
                true,
                meldung
        );
    }

    private OcrDownloadErgebnis downloadUndImportiereBlbOcrFuerBand(
            int bandId,
            String manifestId,
            Consumer<String> progressCallback
    ) {
        String bereinigteManifestId = manifestId != null
                ? manifestId.trim()
                : "";

        if (bereinigteManifestId.isBlank()) {
            String meldung = "BLB Manifest-ID fehlt.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    manifestId,
                    0,
                    0,
                    0,
                    0,
                    false,
                    meldung
            );
        }

        String manifestUrl = "https://digital.blb-karlsruhe.de/i3f/v20/"
                + bereinigteManifestId
                + "/manifest";

        meldeFortschritt(progressCallback, "BLB-OCR-Import gestartet.");
        meldeFortschritt(progressCallback, "Manifest-ID: " + bereinigteManifestId);
        meldeFortschritt(progressCallback, "Manifest-URL:");
        meldeFortschritt(progressCallback, manifestUrl);
        meldeFortschritt(progressCallback, "Manifest wird geladen...");

        int erfolgreich = 0;
        int ohneOcr = 0;
        int fehler = 0;

        try {
            String manifestText = ladeTextVonUrl(manifestUrl);

            if (manifestText == null || manifestText.isBlank()) {
                String meldung = "BLB-Manifest konnte nicht geladen werden oder ist leer.";

                meldeFortschritt(progressCallback, meldung);

                return new OcrDownloadErgebnis(
                        bandId,
                        bereinigteManifestId,
                        0,
                        0,
                        0,
                        0,
                        false,
                        meldung
                );
            }

            List<String> canvasIds = extrahiereBlbCanvasIds(manifestText);
            List<SeitenMappingInfo> mappingSeiten = ladeSeitenMappings(bandId);

            meldeFortschritt(progressCallback, "Gefundene Canvas-/Page-IDs: " + canvasIds.size());
            meldeFortschritt(progressCallback, "SeitenMapping-Einträge im aktuellen Band: " + mappingSeiten.size());

            if (canvasIds.isEmpty()) {
                String meldung = "Keine BLB Canvas-/Page-IDs im Manifest gefunden. Import abgebrochen.";

                meldeFortschritt(progressCallback, meldung);

                return new OcrDownloadErgebnis(
                        bandId,
                        bereinigteManifestId,
                        0,
                        0,
                        0,
                        0,
                        false,
                        meldung
                );
            }

            if (mappingSeiten.isEmpty()) {
                String meldung = "Kein SeitenMapping für BandID "
                        + bandId
                        + " vorhanden. Import abgebrochen.";

                meldeFortschritt(progressCallback, meldung);

                return new OcrDownloadErgebnis(
                        bandId,
                        bereinigteManifestId,
                        canvasIds.size(),
                        0,
                        0,
                        0,
                        false,
                        meldung
                );
            }

            int fehlendeMappings = 0;

            for (String canvasId : canvasIds) {
                SeitenMappingInfo mappingInfo = findeSeitenMappingFuerBlbCanvasId(
                        canvasId,
                        mappingSeiten
                );

                if (mappingInfo == null) {
                    fehlendeMappings++;

                    meldeFortschritt(progressCallback,
                            "FEHLT im SeitenMapping: " + canvasId + ".jpg"
                    );
                }
            }

            if (fehlendeMappings > 0) {
                String meldung = "BLB-Import abgebrochen: "
                        + fehlendeMappings
                        + " Canvas/Page-ID(s) konnten nicht eindeutig dem SeitenMapping zugeordnet werden.";

                meldeFortschritt(progressCallback, meldung);

                return new OcrDownloadErgebnis(
                        bandId,
                        bereinigteManifestId,
                        canvasIds.size(),
                        0,
                        0,
                        fehlendeMappings,
                        false,
                        meldung
                );
            }

            meldeFortschritt(progressCallback, "Alle BLB Canvas/Page-IDs wurden im SeitenMapping gefunden.");
            meldeFortschritt(progressCallback, "OCR-Texte werden jetzt gespeichert...");

            for (int i = 0; i < canvasIds.size(); i++) {
                String canvasId = canvasIds.get(i);
                int aktuelleNummer = i + 1;

                SeitenMappingInfo mappingInfo = findeSeitenMappingFuerBlbCanvasId(
                        canvasId,
                        mappingSeiten
                );

                try {
                    String altoUrl = "https://digital.blb-karlsruhe.de/download/fulltext/alto3/" + canvasId;
                    String altoXml = ladeTextVonUrl(altoUrl);

                    if (altoXml == null || altoXml.isBlank()) {
                        ohneOcr++;

                        meldeFortschritt(progressCallback,
                                aktuelleNummer + " / " + canvasIds.size()
                                        + " OCR nachholen (eigene OCR): " + mappingInfo.dateiname()
                                        + " | Grund: BLB liefert keine ALTO-Datei"
                        );

                        continue;
                    }

                    String text = extrahiereTextAusAlto(altoXml);

                    if (text == null || text.isBlank()) {
                        ohneOcr++;

                        meldeFortschritt(progressCallback,
                                aktuelleNummer + " / " + canvasIds.size()
                                        + " OCR nachholen (eigene OCR): " + mappingInfo.dateiname()
                                        + " | Grund: ALTO vorhanden, aber ohne erkennbaren Text"
                        );

                        continue;
                    }

                    SeitenOCR ocr = new SeitenOCR();
                    ocr.setBandID(bandId);
                    ocr.setBildIndex(mappingInfo.bildIndex());
                    ocr.setDateiname(mappingInfo.dateiname());
                    ocr.setLogischeSeite(mappingInfo.logischeSeite());
                    ocr.setOcrText(text);
                    ocr.setOcrQuelle("BLB Karlsruhe Digitale Sammlungen / " + bereinigteManifestId);
                    ocr.setOcrFormat("ALTO");

                    seitenOCRRepository.insertOrUpdate(ocr);

                    erfolgreich++;

                    meldeFortschritt(progressCallback,
                            aktuelleNummer + " / " + canvasIds.size()
                                    + " gespeichert: " + mappingInfo.dateiname()
                                    + " | BLB Page-ID: " + canvasId
                                    + " | Zeichen: " + text.length()
                    );

                    Thread.sleep(150);

                } catch (Exception ex) {
                    fehler++;

                    meldeFortschritt(progressCallback,
                            aktuelleNummer + " / " + canvasIds.size()
                                    + " FEHLER: " + canvasId
                                    + " / " + mappingInfo.dateiname()
                                    + " | " + ex.getMessage()
                    );
                }
            }

            String meldung = "BLB-OCR-Import abgeschlossen.";

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigteManifestId,
                    canvasIds.size(),
                    erfolgreich,
                    ohneOcr,
                    fehler,
                    true,
                    meldung
            );

        } catch (Exception ex) {
            String meldung = "Fehler beim BLB-OCR-Import: " + ex.getMessage();

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigteManifestId,
                    0,
                    erfolgreich,
                    ohneOcr,
                    fehler + 1,
                    false,
                    meldung
            );
        }
    }

    private SeitenMappingInfo findeSeitenMappingFuerBlbCanvasId(
            String canvasId,
            List<SeitenMappingInfo> mappingSeiten
    ) {
        if (canvasId == null || canvasId.isBlank()
                || mappingSeiten == null || mappingSeiten.isEmpty()) {
            return null;
        }

        String erwarteterDateiname = canvasId + ".jpg";

        for (SeitenMappingInfo info : mappingSeiten) {
            if (info.dateiname() != null
                    && info.dateiname().equalsIgnoreCase(erwarteterDateiname)) {
                return info;
            }
        }

        return null;
    }

    private List<String> extrahiereBlbCanvasIds(String manifestText) {

        List<String> result = new ArrayList<>();

        if (manifestText == null || manifestText.isBlank()) {
            return result;
        }

        Pattern pattern = Pattern.compile("/canvas/(\\d+)");
        Matcher matcher = pattern.matcher(manifestText);

        while (matcher.find()) {
            String canvasId = matcher.group(1);

            if (!result.contains(canvasId)) {
                result.add(canvasId);
            }
        }

        return result;
    }

    private String ladeTextVonUrl(String url) throws Exception {

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

    private String extrahiereTextAusAlto(String altoXml) {

        if (altoXml == null || altoXml.isBlank()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        Pattern textLinePattern = Pattern.compile(
                "<TextLine\\b[^>]*>(.*?)</TextLine>",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher lineMatcher = textLinePattern.matcher(altoXml);

        while (lineMatcher.find()) {
            String lineXml = lineMatcher.group(1);
            String lineText = extrahiereAltoWoerterAusZeile(lineXml);

            if (!lineText.isBlank()) {
                result.append(lineText).append(System.lineSeparator());
            }
        }

        return result.toString().trim();
    }

    private String extrahiereAltoWoerterAusZeile(String lineXml) {

        if (lineXml == null || lineXml.isBlank()) {
            return "";
        }

        StringBuilder line = new StringBuilder();

        Pattern stringPattern = Pattern.compile(
                "<String\\b[^>]*\\bCONTENT=\"([^\"]*)\"[^>]*/?>",
                Pattern.CASE_INSENSITIVE
        );

        Matcher stringMatcher = stringPattern.matcher(lineXml);

        while (stringMatcher.find()) {
            String wort = dekodiereHtmlEntities(stringMatcher.group(1));

            if (!wort.isBlank()) {
                if (!line.isEmpty()) {
                    line.append(" ");
                }

                line.append(wort);
            }
        }

        return line.toString().trim();
    }

    public OcrDownloadErgebnis downloadUndImportiereBsbOcrFuerBand(int bandId, String objectId) {
        return downloadUndImportiereBsbOcrFuerBand(
                bandId,
                objectId,
                false,
                System.out::println
        );
    }

    public OcrDownloadErgebnis downloadUndImportiereBsbOcrFuerBand(
            int bandId,
            String objectId,
            Consumer<String> progressCallback
    ) {
        return downloadUndImportiereBsbOcrFuerBand(
                bandId,
                objectId,
                false,
                progressCallback
        );
    }

    public OcrDownloadErgebnis downloadUndImportiereBsbOcrFuerBand(
            int bandId,
            String objectId,
            boolean vorhandeneOcrUeberspringen,
            Consumer<String> progressCallback
    ) {

        int erfolgreich = 0;
        int ohneOcr = 0;
        int fehler = 0;
        int uebersprungen = 0;

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

        if (vorhandeneOcrUeberspringen) {
            meldeFortschritt(progressCallback,
                    "Vorhandene OCR-Seiten werden übersprungen. Es werden nur fehlende OCR-Seiten nachgeladen."
            );
        }

        for (int index = 0; index < seiten.size(); index++) {

            SeitenMappingInfo mappingInfo = seiten.get(index);
            int aktuelleNummer = index + 1;

            try {
                if (vorhandeneOcrUeberspringen
                        && seitenOCRRepository.existsByBandIdAndDateiname(
                        bandId,
                        mappingInfo.dateiname()
                )) {
                    uebersprungen++;

                    meldeFortschritt(progressCallback,
                            aktuelleNummer + " / " + seiten.size()
                                    + " übersprungen, OCR bereits vorhanden: "
                                    + mappingInfo.dateiname()
                    );

                    continue;
                }

                String pageNum = ermittlePageNumAusDateiname(mappingInfo.dateiname());

                if (pageNum == null || pageNum.isBlank()) {
                    fehler++;

                    meldeFortschritt(progressCallback,
                            aktuelleNummer + " / " + seiten.size()
                                    + " FEHLER: Seitenzahl aus Dateiname nicht ermittelbar: "
                                    + mappingInfo.dateiname()
                    );

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

                Thread.sleep(250);

            } catch (Exception e) {
                fehler++;

                meldeFortschritt(progressCallback,
                        aktuelleNummer + " / " + seiten.size()
                                + " FEHLER: " + mappingInfo.dateiname()
                                + " | " + e.getMessage()
                );

                e.printStackTrace();
            }
        }

        String meldung = "OCR-Download abgeschlossen.";

        if (vorhandeneOcrUeberspringen) {
            meldung = meldung + " Übersprungen wegen vorhandener OCR: " + uebersprungen + ".";
        }

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

    public OcrDownloadErgebnis pruefeLokalenAltoOrdner(
            int bandId,
            String ordnerPfad,
            int bildseiteVon,
            int bildseiteBis,
            Consumer<String> progressCallback
    ) {

        String bereinigterPfad = ordnerPfad != null
                ? ordnerPfad.trim()
                : "";

        if (bereinigterPfad.isBlank()) {
            String meldung = "Kein ALTO-Ordner ausgewählt.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    0,
                    false,
                    meldung
            );
        }

        if (bildseiteVon < 0 || bildseiteBis < 0) {
            String meldung = "Die Bildseitennummern dürfen nicht negativ sein.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        if (bildseiteVon > bildseiteBis) {
            String meldung =
                    "Bildseite von darf nicht größer als Bildseite bis sein.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        Path altoOrdner = Paths.get(bereinigterPfad);

        if (!Files.isDirectory(altoOrdner)) {
            String meldung =
                    "Der ausgewählte ALTO-Ordner existiert nicht oder ist kein Ordner.";

            meldeFortschritt(progressCallback, meldung);
            meldeFortschritt(progressCallback, bereinigterPfad);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        List<SeitenMappingInfo> mappingSeiten =
                ladeSeitenMappings(bandId);

        if (mappingSeiten.isEmpty()) {
            String meldung =
                    "Kein SeitenMapping für BandID "
                            + bandId
                            + " vorhanden.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        int anzahlBildseiten =
                bildseiteBis - bildseiteVon + 1;

        int passendeZuordnungen = 0;
        int fehlendeMappings = 0;
        int fehlendeAltoDateien = 0;

        meldeFortschritt(
                progressCallback,
                "Lokaler ALTO-Bereich wird geprüft."
        );

        meldeFortschritt(
                progressCallback,
                "ALTO-Ordner: " + bereinigterPfad
        );

        meldeFortschritt(
                progressCallback,
                "Bildseitenbereich: "
                        + bildseiteVon
                        + " bis "
                        + bildseiteBis
        );

        meldeFortschritt(
                progressCallback,
                "Erwartete Seiten im Bereich: "
                        + anzahlBildseiten
        );

        meldeFortschritt(progressCallback, "");

        for (int bildseite = bildseiteVon;
             bildseite <= bildseiteBis;
             bildseite++) {

            String erwarteterBildDateiname =
                    String.format("seite-%03d.jpg", bildseite);

            int altoNummer = bildseite - 1;

            String erwarteterAltoDateiname =
                    String.format("%08d.xml", altoNummer);

            SeitenMappingInfo mappingInfo = null;

            for (SeitenMappingInfo info : mappingSeiten) {
                if (info.dateiname() != null
                        && info.dateiname().equalsIgnoreCase(
                        erwarteterBildDateiname
                )) {
                    mappingInfo = info;
                    break;
                }
            }

            if (mappingInfo == null) {
                fehlendeMappings++;

                meldeFortschritt(
                        progressCallback,
                        "KEIN MAPPING: "
                                + erwarteterBildDateiname
                );

                continue;
            }

            Path altoDatei =
                    altoOrdner.resolve(erwarteterAltoDateiname);

            if (!Files.isRegularFile(altoDatei)) {
                fehlendeAltoDateien++;

                meldeFortschritt(
                        progressCallback,
                        "ALTO FEHLT: "
                                + erwarteterAltoDateiname
                                + " für "
                                + erwarteterBildDateiname
                );

                continue;
            }

            passendeZuordnungen++;
        }

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(
                progressCallback,
                "Passende Zuordnungen: "
                        + passendeZuordnungen
        );

        meldeFortschritt(
                progressCallback,
                "Fehlende SeitenMapping-Einträge: "
                        + fehlendeMappings
        );

        meldeFortschritt(
                progressCallback,
                "Fehlende ALTO-Dateien: "
                        + fehlendeAltoDateien
        );

        boolean allesPassend =
                passendeZuordnungen == anzahlBildseiten
                        && fehlendeMappings == 0
                        && fehlendeAltoDateien == 0;

        String meldung;

        if (allesPassend) {
            meldung =
                    "ALTO-Bereich vollständig und eindeutig zugeordnet.";
        } else {
            meldung =
                    "ALTO-Bereich ist noch nicht vollständig zuordenbar.";
        }

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(progressCallback, meldung);

        return new OcrDownloadErgebnis(
                bandId,
                bereinigterPfad,
                anzahlBildseiten,
                0,
                fehlendeAltoDateien,
                fehlendeMappings,
                allesPassend,
                meldung
        );
    }

    public OcrDownloadErgebnis importiereLokaleAltoDateien(
            int bandId,
            String ordnerPfad,
            int bildseiteVon,
            int bildseiteBis,
            Consumer<String> progressCallback
    ) {

        String bereinigterPfad = ordnerPfad != null
                ? ordnerPfad.trim()
                : "";

        if (bereinigterPfad.isBlank()) {
            String meldung = "Kein ALTO-Ordner ausgewählt.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    0,
                    false,
                    meldung
            );
        }

        if (bildseiteVon < 0 || bildseiteBis < 0) {
            String meldung =
                    "Die Bildseitennummern dürfen nicht negativ sein.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        if (bildseiteVon > bildseiteBis) {
            String meldung =
                    "Bildseite von darf nicht größer als Bildseite bis sein.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        Path altoOrdner = Paths.get(bereinigterPfad);

        if (!Files.isDirectory(altoOrdner)) {
            String meldung =
                    "Der ausgewählte ALTO-Ordner existiert nicht oder ist kein Ordner.";

            meldeFortschritt(progressCallback, meldung);
            meldeFortschritt(progressCallback, bereinigterPfad);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        List<SeitenMappingInfo> mappingSeiten =
                ladeSeitenMappings(bandId);

        if (mappingSeiten.isEmpty()) {
            String meldung =
                    "Kein SeitenMapping für BandID "
                            + bandId
                            + " vorhanden.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        int anzahlBildseiten =
                bildseiteBis - bildseiteVon + 1;

        int erfolgreich = 0;
        int ohneOcr = 0;
        int fehler = 0;

        int fehlendeMappings = 0;
        int fehlendeAltoDateien = 0;

        for (int bildseite = bildseiteVon;
             bildseite <= bildseiteBis;
             bildseite++) {

            String erwarteterBildDateiname =
                    String.format("seite-%03d.jpg", bildseite);

            int altoNummer = bildseite - 1;

            String erwarteterAltoDateiname =
                    String.format("%08d.xml", altoNummer);

            SeitenMappingInfo mappingInfo = null;

            for (SeitenMappingInfo info : mappingSeiten) {
                if (info.dateiname() != null
                        && info.dateiname().equalsIgnoreCase(
                        erwarteterBildDateiname
                )) {
                    mappingInfo = info;
                    break;
                }
            }

            if (mappingInfo == null) {
                fehlendeMappings++;

                meldeFortschritt(
                        progressCallback,
                        "KEIN MAPPING: " + erwarteterBildDateiname
                );

                continue;
            }

            Path altoDatei =
                    altoOrdner.resolve(erwarteterAltoDateiname);

            if (!Files.isRegularFile(altoDatei)) {
                fehlendeAltoDateien++;

                meldeFortschritt(
                        progressCallback,
                        "ALTO FEHLT: "
                                + erwarteterAltoDateiname
                                + " für "
                                + erwarteterBildDateiname
                );
            }
        }

        if (fehlendeMappings > 0 || fehlendeAltoDateien > 0) {
            String meldung =
                    "Lokaler ALTO-Import wurde nicht gestartet. "
                            + "Fehlende SeitenMapping-Einträge: "
                            + fehlendeMappings
                            + ", fehlende ALTO-Dateien: "
                            + fehlendeAltoDateien
                            + ".";

            meldeFortschritt(progressCallback, "");
            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    anzahlBildseiten,
                    0,
                    fehlendeAltoDateien,
                    fehlendeMappings,
                    false,
                    meldung
            );
        }

        meldeFortschritt(
                progressCallback,
                "Lokaler ALTO-Import wird vorbereitet."
        );

        meldeFortschritt(
                progressCallback,
                "ALTO-Ordner: " + bereinigterPfad
        );

        meldeFortschritt(
                progressCallback,
                "Bildseitenbereich: "
                        + bildseiteVon
                        + " bis "
                        + bildseiteBis
        );

        meldeFortschritt(
                progressCallback,
                "Zu importierende Seiten: " + anzahlBildseiten
        );

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(
                progressCallback,
                "Alle Zuordnungen wurden geprüft. "
                        + "Die ALTO-Dateien werden jetzt importiert."
        );
        meldeFortschritt(progressCallback, "");

        for (int bildseite = bildseiteVon;
             bildseite <= bildseiteBis;
             bildseite++) {

            int aktuelleNummer =
                    bildseite - bildseiteVon + 1;

            String erwarteterBildDateiname =
                    String.format("seite-%03d.jpg", bildseite);

            int altoNummer = bildseite - 1;

            String erwarteterAltoDateiname =
                    String.format("%08d.xml", altoNummer);

            SeitenMappingInfo mappingInfo = null;

            for (SeitenMappingInfo info : mappingSeiten) {
                if (info.dateiname() != null
                        && info.dateiname().equalsIgnoreCase(
                        erwarteterBildDateiname
                )) {
                    mappingInfo = info;
                    break;
                }
            }

            /*
             * Diese Fälle wurden bereits vor dem Import vollständig geprüft.
             * Die zusätzliche Kontrolle schützt trotzdem vor unerwarteten
             * Änderungen während des laufenden Imports.
             */
            if (mappingInfo == null) {
                fehler++;

                meldeFortschritt(
                        progressCallback,
                        aktuelleNummer + " / " + anzahlBildseiten
                                + " FEHLER: SeitenMapping fehlt für "
                                + erwarteterBildDateiname
                );

                continue;
            }

            Path altoDatei =
                    altoOrdner.resolve(erwarteterAltoDateiname);

            if (!Files.isRegularFile(altoDatei)) {
                fehler++;

                meldeFortschritt(
                        progressCallback,
                        aktuelleNummer + " / " + anzahlBildseiten
                                + " FEHLER: ALTO-Datei fehlt: "
                                + erwarteterAltoDateiname
                );

                continue;
            }

            try {
                String altoXml = Files.readString(altoDatei);

                if (altoXml.isBlank()) {
                    ohneOcr++;

                    meldeFortschritt(
                            progressCallback,
                            aktuelleNummer + " / " + anzahlBildseiten
                                    + " OCR nachholen (eigene OCR): "
                                    + mappingInfo.dateiname()
                                    + " | Grund: ALTO-Datei ist leer"
                    );

                    continue;
                }

                String text =
                        extrahiereTextAusAlto(altoXml);

                if (text == null || text.isBlank()) {
                    ohneOcr++;

                    meldeFortschritt(
                            progressCallback,
                            aktuelleNummer + " / " + anzahlBildseiten
                                    + " OCR nachholen (eigene OCR): "
                                    + mappingInfo.dateiname()
                                    + " | Grund: ALTO enthält keinen erkennbaren Text"
                    );

                    continue;
                }

                SeitenOCR ocr = new SeitenOCR();
                ocr.setBandID(bandId);
                ocr.setBildIndex(mappingInfo.bildIndex());
                ocr.setDateiname(mappingInfo.dateiname());
                ocr.setLogischeSeite(mappingInfo.logischeSeite());
                ocr.setOcrText(text);
                ocr.setOcrQuelle(
                        "Lokale ALTO-Dateien / Württemberg"
                );
                ocr.setOcrFormat("ALTO 2.0");

                seitenOCRRepository.insertOrUpdate(ocr);

                erfolgreich++;

                meldeFortschritt(
                        progressCallback,
                        aktuelleNummer + " / " + anzahlBildseiten
                                + " gespeichert: "
                                + mappingInfo.dateiname()
                                + " | ALTO: "
                                + erwarteterAltoDateiname
                                + " | Zeichen: "
                                + text.length()
                );

            } catch (Exception ex) {
                fehler++;

                meldeFortschritt(
                        progressCallback,
                        aktuelleNummer + " / " + anzahlBildseiten
                                + " FEHLER: "
                                + erwarteterAltoDateiname
                                + " / "
                                + mappingInfo.dateiname()
                                + " | "
                                + ex.getMessage()
                );

                ex.printStackTrace();
            }
        }

        String meldung =
                "Lokaler ALTO-Import abgeschlossen. "
                        + "Erfolgreich gespeichert: "
                        + erfolgreich
                        + ", OCR nachholen (eigene OCR): "
                        + ohneOcr
                        + ", Fehler: "
                        + fehler
                        + ".";

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(progressCallback, meldung);

        return new OcrDownloadErgebnis(
                bandId,
                bereinigterPfad,
                anzahlBildseiten,
                erfolgreich,
                ohneOcr,
                fehler,
                true,
                meldung
        );
    }

    public OcrDownloadErgebnis pruefeLokalenAltoOrdner(
            int bandId,
            String ordnerPfad,
            Consumer<String> progressCallback
    ) {

        String bereinigterPfad = ordnerPfad != null
                ? ordnerPfad.trim()
                : "";

        if (bereinigterPfad.isBlank()) {
            String meldung = "Kein ALTO-Ordner ausgewählt.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    0,
                    false,
                    meldung
            );
        }

        Path ordner = Paths.get(bereinigterPfad);

        if (!Files.exists(ordner)) {
            String meldung = "Der ausgewählte ALTO-Ordner existiert nicht.";

            meldeFortschritt(progressCallback, meldung);
            meldeFortschritt(progressCallback, bereinigterPfad);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        if (!Files.isDirectory(ordner)) {
            String meldung = "Der ausgewählte Pfad ist kein Ordner.";

            meldeFortschritt(progressCallback, meldung);
            meldeFortschritt(progressCallback, bereinigterPfad);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        meldeFortschritt(progressCallback, "Lokaler ALTO-Ordner wird geprüft.");
        meldeFortschritt(progressCallback, "Ordner: " + bereinigterPfad);
        meldeFortschritt(progressCallback, "");

        List<Path> altoDateien;

        try (Stream<Path> stream = Files.list(ordner)) {

            altoDateien = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName()
                            .toString()
                            .toLowerCase()
                            .endsWith(".xml"))
                    .sorted(Comparator.comparing(
                            path -> path.getFileName().toString()
                    ))
                    .toList();

        } catch (Exception ex) {
            String meldung = "Fehler beim Lesen des ALTO-Ordners: "
                    + ex.getMessage();

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    0,
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        List<SeitenMappingInfo> mappingSeiten = ladeSeitenMappings(bandId);

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(progressCallback, "Beispiele aus dem SeitenMapping:");

        for (int i = 0; i < Math.min(10, mappingSeiten.size()); i++) {
            SeitenMappingInfo info = mappingSeiten.get(i);

            meldeFortschritt(
                    progressCallback,
                    "Mapping " + (i + 1)
                            + ": BildIndex=" + info.bildIndex()
                            + " | Dateiname=" + info.dateiname()
                            + " | Stamm=" + ermittleDateistamm(info.dateiname())
            );
        }

        meldeFortschritt(progressCallback, "Letzte Einträge aus dem SeitenMapping:");

        int mappingStart = Math.max(0, mappingSeiten.size() - 10);

        for (int i = mappingStart; i < mappingSeiten.size(); i++) {
            SeitenMappingInfo info = mappingSeiten.get(i);

            meldeFortschritt(
                    progressCallback,
                    "Mapping " + (i + 1)
                            + ": BildIndex=" + info.bildIndex()
                            + " | Dateiname=" + info.dateiname()
                            + " | Stamm=" + ermittleDateistamm(info.dateiname())
            );
        }

        meldeFortschritt(progressCallback, "");

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(progressCallback, "Beispiele aus dem ALTO-Ordner:");

        for (int i = 0; i < Math.min(10, altoDateien.size()); i++) {
            Path datei = altoDateien.get(i);

            meldeFortschritt(
                    progressCallback,
                    "ALTO " + (i + 1)
                            + ": Dateiname=" + datei.getFileName()
                            + " | Stamm=" + ermittleDateistamm(
                            datei.getFileName().toString()
                    )
            );
        }

        meldeFortschritt(progressCallback, "Letzte Dateien aus dem ALTO-Ordner:");

        int altoStart = Math.max(0, altoDateien.size() - 10);

        for (int i = altoStart; i < altoDateien.size(); i++) {
            Path datei = altoDateien.get(i);

            meldeFortschritt(
                    progressCallback,
                    "ALTO " + (i + 1)
                            + ": Dateiname=" + datei.getFileName()
                            + " | Stamm=" + ermittleDateistamm(
                            datei.getFileName().toString()
                    )
            );
        }

        meldeFortschritt(progressCallback, "");

        meldeFortschritt(progressCallback, "");

        meldeFortschritt(
                progressCallback,
                "SeitenMapping-Einträge: " + mappingSeiten.size()
        );

        if (mappingSeiten.isEmpty()) {
            String meldung = "Kein SeitenMapping für BandID "
                    + bandId
                    + " vorhanden.";

            meldeFortschritt(progressCallback, meldung);

            return new OcrDownloadErgebnis(
                    bandId,
                    bereinigterPfad,
                    altoDateien.size(),
                    0,
                    0,
                    1,
                    false,
                    meldung
            );
        }

        int passendeDateien = 0;
        int fehlendeZuordnungen = 0;

        for (Path altoDatei : altoDateien) {

            String altoStamm = ermittleDateistamm(
                    altoDatei.getFileName().toString()
            );

            SeitenMappingInfo gefundenesMapping = null;

            for (SeitenMappingInfo mappingInfo : mappingSeiten) {

                String mappingStamm = ermittleDateistamm(
                        mappingInfo.dateiname()
                );

                if (altoStamm.equals(mappingStamm)) {
                    gefundenesMapping = mappingInfo;
                    break;
                }
            }

            if (gefundenesMapping != null) {
                passendeDateien++;

                meldeFortschritt(
                        progressCallback,
                        "ZUORDNUNG OK: "
                                + altoDatei.getFileName()
                                + " -> "
                                + gefundenesMapping.dateiname()
                );
            } else {
                fehlendeZuordnungen++;

                meldeFortschritt(
                        progressCallback,
                        "KEIN MAPPING: " + altoDatei.getFileName()
                );
            }
        }

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(
                progressCallback,
                "Passende ALTO-Dateien: " + passendeDateien
        );
        meldeFortschritt(
                progressCallback,
                "ALTO-Dateien ohne Mapping: " + fehlendeZuordnungen
        );

        meldeFortschritt(
                progressCallback,
                "Gefundene XML-Dateien: " + altoDateien.size()
        );

        String meldung;

        if (altoDateien.isEmpty()) {
            meldung = "Im ausgewählten Ordner wurden keine XML-Dateien gefunden.";
        } else {
            meldung = "ALTO-Ordner erfolgreich geprüft.";
        }

        meldeFortschritt(progressCallback, "");
        meldeFortschritt(progressCallback, meldung);

        return new OcrDownloadErgebnis(
                bandId,
                bereinigterPfad,
                altoDateien.size(),
                0,
                0,
                0,
                !altoDateien.isEmpty(),
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

    private String ermittleDateistamm(String dateiname) {

        if (dateiname == null || dateiname.isBlank()) {
            return "";
        }

        String name = dateiname.trim();

        int punktIndex = name.indexOf('.');

        if (punktIndex > 0) {
            name = name.substring(0, punktIndex);
        }

        return name.toLowerCase();
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