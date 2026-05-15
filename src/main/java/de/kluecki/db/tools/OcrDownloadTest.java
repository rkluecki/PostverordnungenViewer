package de.kluecki.db.tools;

import de.kluecki.ocr.OcrDownloadService;
import de.kluecki.ocr.OcrDownloadService.OcrDownloadErgebnis;

public class OcrDownloadTest {

    private static final int BAND_ID = 38;
    private static final String OBJECT_ID = "bsb10335662";

    public static void main(String[] args) {

        OcrDownloadService service = new OcrDownloadService();

        System.out.println("Starte OCR-Download-Test");
        System.out.println("BandID: " + BAND_ID);
        System.out.println("Object-ID: " + OBJECT_ID);
        System.out.println();

        OcrDownloadErgebnis ergebnis = service.downloadUndImportiereBsbOcrFuerBand(
                BAND_ID,
                OBJECT_ID
        );

        System.out.println();
        System.out.println(ergebnis.meldung());
        System.out.println("Gestartet: " + ergebnis.gestartet());
        System.out.println("Mapping-Seiten: " + ergebnis.mappingSeiten());
        System.out.println("Erfolgreich gespeichert: " + ergebnis.erfolgreich());
        System.out.println("Ohne OCR/Text: " + ergebnis.ohneOcr());
        System.out.println("Fehler: " + ergebnis.fehler());
    }
}