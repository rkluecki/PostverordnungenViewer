package de.kluecki.ocr;

/**
 * Legt fest, aus welcher OCR-Quelle ein Import stammt.
 *
 * Aktuell gibt es nur BSB/MDZ als funktionierenden Spezialfall.
 * Später können hier weitere Archive oder OCR-Formate ergänzt werden.
 */
public enum OcrArchivTyp {

    /**
     * Bayerische Staatsbibliothek / Münchener DigitalisierungsZentrum.
     *
     * Aktuelle API-Logik:
     * https://api.digitale-sammlungen.de/ocr/{objectId}/{pageNum}
     */
    BSB_MDZ
}