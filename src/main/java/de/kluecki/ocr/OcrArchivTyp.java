package de.kluecki.ocr;

/**
 * Legt fest, aus welcher OCR-Quelle ein Import stammt.
 *
 * BSB_MDZ:
 * Online-Import über die BSB/MDZ-OCR-API.
 *
 * BLB_KARLSRUHE:
 * Online-Import über IIIF-Manifest und ALTO-Dateien
 * der Badischen Landesbibliothek.
 *
 * LOKALE_ALTO_DATEIEN:
 * Import bereits heruntergeladener ALTO-XML-Dateien
 * aus einem lokalen Ordner.
 */
public enum OcrArchivTyp {

    /**
     * Bayerische Staatsbibliothek / Münchener DigitalisierungsZentrum.
     *
     * API:
     * https://api.digitale-sammlungen.de/ocr/{objectId}/{pageNum}
     */
    BSB_MDZ,

    /**
     * Badische Landesbibliothek Karlsruhe / Digitale Sammlungen.
     *
     * Ablauf:
     * IIIF-Manifest laden, Canvas-/Page-IDs ermitteln
     * und anschließend ALTO-Daten je Seite abrufen.
     */
    BLB_KARLSRUHE,

    /**
     * Bereits heruntergeladene ALTO-XML-Dateien
     * aus einem lokalen Verzeichnis.
     *
     * Die Zuordnung zu dbo.SeitenMapping erfolgt
     * anhand des Dateinamens ohne Dateiendung.
     */
    LOKALE_ALTO_DATEIEN
}