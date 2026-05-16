package de.kluecki.ocr;

/**
 * Legt fest, aus welcher OCR-Quelle ein Import stammt.
 *
 * BSB_MDZ ist aktuell der funktionierende Online-Import über die BSB/MDZ-OCR-API.
 * BLB_KARLSRUHE ist als zweiter Archivtyp für die Digitalen Sammlungen
 * der Badischen Landesbibliothek vorbereitet.
 */
public enum OcrArchivTyp {

    /**
     * Bayerische Staatsbibliothek / Münchener DigitalisierungsZentrum.
     *
     * Aktuelle API-Logik:
     * https://api.digitale-sammlungen.de/ocr/{objectId}/{pageNum}
     */
    BSB_MDZ,

    /**
     * Badische Landesbibliothek Karlsruhe / Digitale Sammlungen.
     *
     * Geplanter Einstieg:
     * IIIF-Manifest laden, daraus Canvas-/Page-IDs ermitteln
     * und anschließend die OCR-Daten je Seite abrufen.
     *
     * Beispiel:
     * Manifest-ID: 7010966
     * Page-ID: 6998715
     */
    BLB_KARLSRUHE
}