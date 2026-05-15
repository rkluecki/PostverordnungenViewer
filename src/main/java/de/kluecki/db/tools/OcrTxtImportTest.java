package de.kluecki.db.tools;

import de.kluecki.db.model.SeitenMapping;
import de.kluecki.db.model.SeitenOCR;
import de.kluecki.db.repository.QuelleRepository;
import de.kluecki.db.repository.SeitenMappingRepository;
import de.kluecki.db.repository.SeitenOCRRepository;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class OcrTxtImportTest {

    public static void main(String[] args) {

        String gebiet = "Bayern";
        String band = "1842";

        Path ocrTxtOrdner = Path.of(
                "D:\\Postgeschichte_PC\\Postverordnungen\\Bayern\\1842\\OCR-TXT"
        );

        QuelleRepository quelleRepository = new QuelleRepository();
        SeitenMappingRepository seitenMappingRepository = new SeitenMappingRepository();
        SeitenOCRRepository seitenOCRRepository = new SeitenOCRRepository();

        int bandId = quelleRepository.findBandId(gebiet, band);

        if (bandId <= 0) {
            System.out.println("BandID nicht gefunden für: " + gebiet + " / " + band);
            return;
        }

        System.out.println("BandID: " + bandId);

        try {
            if (!Files.exists(ocrTxtOrdner)) {
                System.out.println("OCR-TXT-Ordner existiert nicht:");
                System.out.println(ocrTxtOrdner);
                return;
            }

            List<SeitenMapping> mappings =
                    seitenMappingRepository.findByBandId(bandId);

            int importiert = 0;
            int uebersprungen = 0;

            for (SeitenMapping mapping : mappings) {

                String dateiname = mapping.getDateiname();

                if (dateiname == null || dateiname.isBlank()) {
                    uebersprungen++;
                    continue;
                }

                String dateinameOhneEndung = entferneDateiendung(dateiname);

                Path txtDatei = ocrTxtOrdner.resolve(dateinameOhneEndung + ".txt");

                if (!Files.exists(txtDatei)) {
                    System.out.println("Keine TXT gefunden für: " + dateiname);
                    uebersprungen++;
                    continue;
                }

                String ocrText = Files.readString(txtDatei);

                SeitenOCR ocr = new SeitenOCR();
                ocr.setBandID(bandId);
                ocr.setBildIndex(mapping.getBildIndex());
                ocr.setDateiname(dateiname);
                ocr.setLogischeSeite(mapping.getLogischeSeite());
                ocr.setOcrText(ocrText);
                ocr.setOcrQuelle("BSB / bsb10335662");
                ocr.setOcrFormat("hOCR->TXT");

                seitenOCRRepository.insertOrUpdate(ocr);

                importiert++;

                System.out.println("Importiert: " + dateiname);
            }

            System.out.println();
            System.out.println("Fertig.");
            System.out.println("Importiert: " + importiert);
            System.out.println("Übersprungen: " + uebersprungen);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String entferneDateiendung(String dateiname) {

        int punktIndex = dateiname.lastIndexOf('.');

        if (punktIndex <= 0) {
            return dateiname;
        }

        return dateiname.substring(0, punktIndex);
    }
}