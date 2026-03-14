package de.kluecki.db.print;

import javafx.print.PrinterJob;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import java.io.File;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javafx.embed.swing.SwingFXUtils;

import java.awt.image.BufferedImage;
import java.io.IOException;
import de.kluecki.db.model.VerordnungBetreff;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.StageStyle;
import javafx.application.Platform;

public class PrintPdfService {

    private final String rootPath;
    private final PageImageProvider pageImageProvider;

    public PrintPdfService(String rootPath, PageImageProvider pageImageProvider) {
        this.rootPath = rootPath;
        this.pageImageProvider = pageImageProvider;
    }

    public void printSinglePage(Image image) {

        PrinterJob job = PrinterJob.createPrinterJob();

        if (job == null) {
            System.out.println("Kein Drucker gefunden");
            return;
        }

        boolean proceed = job.showPrintDialog(null);

        if (!proceed) {
            return;
        }

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);

        double printableWidth = job.getJobSettings().getPageLayout().getPrintableWidth();
        double printableHeight = job.getJobSettings().getPageLayout().getPrintableHeight();

        imageView.setFitWidth(printableWidth);
        imageView.setFitHeight(printableHeight);

        StackPane pane = new StackPane(imageView);
        pane.setPrefSize(printableWidth, printableHeight);

        boolean success = job.printPage(pane);

        if (success) {
            job.endJob();
        }
    }

    public void exportSinglePageToPdf(Image image, String titel) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seite als PDF speichern");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf")
        );
        fileChooser.setInitialFileName(createSafeFileName(titel) + ".pdf");

        File file = fileChooser.showSaveDialog(null);

        if (file == null) {
            return;
        }

        BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

        try (PDDocument document = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

            float pageWidth = page.getMediaBox().getWidth();
            float pageHeight = page.getMediaBox().getHeight();

            float imageWidth = pdImage.getWidth();
            float imageHeight = pdImage.getHeight();

            float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);

            float drawWidth = imageWidth * scale;
            float drawHeight = imageHeight * scale;

            float x = (pageWidth - drawWidth) / 2;
            float y = (pageHeight - drawHeight) / 2;

            try (var contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {
                contentStream.drawImage(pdImage, x, y, drawWidth, drawHeight);
            }

            document.save(file);

            System.out.println("PDF gespeichert: " + file.getAbsolutePath());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private String createSafeFileName(String text) {

        if (text == null || text.isBlank()) {
            return "dokument";
        }

        String safe = text;

        safe = safe.replace("/", "-");
        safe = safe.replace("\\", "-");
        safe = safe.replace(":", "-");
        safe = safe.replace("*", "");
        safe = safe.replace("?", "");
        safe = safe.replace("\"", "");
        safe = safe.replace("<", "");
        safe = safe.replace(">", "");
        safe = safe.replace("|", "-");

        safe = safe.trim();

        return safe;
    }

    public void exportRangeToPdf(String gebiet, String bandJahr, int seiteVon, int seiteBis, String titel) {

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Verordnung als PDF speichern");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PDF-Dateien", "*.pdf")
        );
        fileChooser.setInitialFileName(createSafeFileName(titel) + ".pdf");

        File file = fileChooser.showSaveDialog(null);

        if (file == null) {
            return;
        }

        try (PDDocument document = new PDDocument()) {

            for (int seite = seiteVon; seite <= seiteBis; seite++) {

                Image image = pageImageProvider.loadPageImage(gebiet, bandJahr, seite);

                if (image == null) {
                    System.out.println("Kein Bild für Seite " + seite);
                    continue;
                }

                PDPage page = new PDPage(PDRectangle.A4);
                document.addPage(page);

                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
                PDImageXObject pdImage = LosslessFactory.createFromImage(document, bufferedImage);

                float pageWidth = page.getMediaBox().getWidth();
                float pageHeight = page.getMediaBox().getHeight();

                float imageWidth = pdImage.getWidth();
                float imageHeight = pdImage.getHeight();

                float scale = Math.min(pageWidth / imageWidth, pageHeight / imageHeight);

                float drawWidth = imageWidth * scale;
                float drawHeight = imageHeight * scale;

                float x = (pageWidth - drawWidth) / 2;
                float y = (pageHeight - drawHeight) / 2;

                try (var contentStream =
                             new org.apache.pdfbox.pdmodel.PDPageContentStream(document, page)) {

                    contentStream.drawImage(pdImage, x, y, drawWidth, drawHeight);
                }

                System.out.println("Seite geladen: " + seite);
            }

            document.save(file);
            System.out.println("PDF gespeichert: " + file.getAbsolutePath());

        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public void printRange(VerordnungBetreff betreff) {

        if (betreff == null) {
            System.out.println("Kein Verordnungsbetreff übergeben");
            return;
        }

        String gebiet = betreff.getGebiet();
        String bandJahr = betreff.getBandJahr();
        int seiteVon = betreff.getSeiteVon();
        int seiteBis = betreff.getSeiteBis();

        PrinterJob job = PrinterJob.createPrinterJob();

        String jobName = betreff.getTitel();

        if (jobName == null || jobName.isBlank()) {
            jobName = "Verordnung";
        }

        job.getJobSettings().setJobName(jobName);

        if (job == null) {
            System.out.println("Kein Drucker gefunden");
            return;
        }

        boolean proceed = job.showPrintDialog(null);

        if (!proceed) {
            return;
        }

        Alert progressAlert = new Alert(Alert.AlertType.INFORMATION);
        progressAlert.initStyle(StageStyle.UTILITY);
        progressAlert.initModality(Modality.NONE);
        progressAlert.setTitle("Druck läuft");
        progressAlert.setHeaderText("Verordnung wird gedruckt");
        progressAlert.setContentText("Bereite Druck vor...");

        Label progressLabel = new Label("Bereite Druck vor...");
        progressAlert.getDialogPane().setContent(progressLabel);

        progressAlert.show();

        boolean success = true;

        for (int seite = seiteVon; seite <= seiteBis; seite++) {
            int gesamtSeiten = seiteBis - seiteVon + 1;
            int aktuellePosition = seite - seiteVon + 1;

            progressLabel.setText("Drucke Seite " + aktuellePosition + " von " + gesamtSeiten + " ...");

            Image image = pageImageProvider.loadPageImage(gebiet, bandJahr, seite);

            if (image == null) {
                System.out.println("Kein Bild für Seite " + seite);
                continue;
            }

            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);

            double printableWidth = job.getJobSettings().getPageLayout().getPrintableWidth();
            double printableHeight = job.getJobSettings().getPageLayout().getPrintableHeight();

            imageView.setFitWidth(printableWidth);
            imageView.setFitHeight(printableHeight);

            StackPane pane = new StackPane(imageView);
            pane.setPrefSize(printableWidth, printableHeight);

            boolean pageSuccess = job.printPage(pane);

            if (!pageSuccess) {

                int fehlerSeite = seite;

                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Druckfehler");
                    alert.setHeaderText("Druck konnte nicht abgeschlossen werden");
                    alert.setContentText("Fehler beim Drucken von Seite " + fehlerSeite);
                    alert.showAndWait();
                });

                success = false;
                break;
            }

            System.out.println("Seite gedruckt: " + seite);
        }

        if (success) {
            job.endJob();
        }

        progressAlert.close();

        if (success) {
            Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Druck fertig");
                alert.setHeaderText(null);
                alert.setContentText("Verordnung wurde erfolgreich gedruckt.");
                alert.show();
            });
        }
    }
}