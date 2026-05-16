package de.kluecki.ocr;

import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import de.kluecki.ocr.OcrDownloadService.OcrDownloadErgebnis;
import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Dialog für den späteren OCR-Download/-Import.
 *
 * Aktueller Stand:
 * - zeigt BandID und Bandbezeichnung
 * - erlaubt Eingabe der BSB/MDZ-Object-ID
 * - startet den Import noch NICHT
 *
 * Wichtig:
 * OCR-Import setzt ein vorhandenes SeitenMapping / Grundmapping voraus.
 */
public class OcrDownloadDialog {

    public static void show(
            Stage owner,
            int bandId,
            String bandAnzeige
    ) {
        Stage dialog = new Stage();
        dialog.setTitle("BSB/MDZ-OCR herunterladen / importieren");
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        OcrArchivTyp archivTyp = OcrArchivTyp.BSB_MDZ;

        Label lblTitel = new Label("BSB/MDZ-OCR für Band herunterladen");
        lblTitel.setStyle("""
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                """);

        Label lblBand = new Label("Aktueller Band:");
        Label lblBandWert = new Label(bandAnzeige != null && !bandAnzeige.isBlank()
                ? bandAnzeige
                : "(kein Bandname verfügbar)");

        Label lblBandId = new Label("BandID:");
        Label lblBandIdWert = new Label(String.valueOf(bandId));

        Label lblArchivTyp = new Label("OCR-Quelle:");
        Label lblArchivTypWert = new Label(getArchivTypAnzeige(archivTyp));

        TextField txtObjectId = new TextField();
        txtObjectId.setPromptText("z. B. bsb10335662");
        txtObjectId.setPrefColumnCount(22);

        Label lblHinweis = new Label("""
                Hinweis:
                Der OCR-Import darf erst gestartet werden, wenn für diesen Band
                ein Grundmapping / SeitenMapping vorhanden ist.
                Die OCR-Zuordnung erfolgt ausschließlich über die vorhandenen
                Dateinamen aus dem SeitenMapping.
                """);
        lblHinweis.setWrapText(true);
        lblHinweis.setMaxWidth(520);
        lblHinweis.setStyle("""
                -fx-font-size: 12px;
                -fx-text-fill: #555555;
                """);

        TextArea txtStatus = new TextArea();
        txtStatus.setEditable(false);
        txtStatus.setWrapText(true);
        txtStatus.setPrefRowCount(6);
        txtStatus.setPromptText("Hier erscheint später der Fortschritt des OCR-Imports.");

        Button btnStart = new Button("Import starten");
        btnStart.setDisable(true);

        txtObjectId.textProperty().addListener((obs, alt, neu) -> {
            boolean leer = neu == null || neu.trim().isBlank();
            btnStart.setDisable(leer);
        });

        btnStart.setOnAction(e -> {

            String objectId = txtObjectId.getText() != null
                    ? txtObjectId.getText().trim()
                    : "";

            if (objectId.isBlank()) {
                txtStatus.setText("Bitte zuerst eine BSB/MDZ Object-ID eingeben.");
                return;
            }

            btnStart.setDisable(true);
            txtObjectId.setDisable(true);
            txtStatus.clear();

            txtStatus.appendText("OCR-Quelle: " + getArchivTypAnzeige(archivTyp) + System.lineSeparator());
            txtStatus.appendText("Object-ID: " + objectId + System.lineSeparator());
            txtStatus.appendText("Import wird vorbereitet..." + System.lineSeparator());
            txtStatus.appendText(System.lineSeparator());

            OcrDownloadService service = new OcrDownloadService();

            Task<OcrDownloadErgebnis> task = new Task<>() {
                @Override
                protected OcrDownloadErgebnis call() {
                    return service.downloadUndImportiereOcrFuerBand(
                            archivTyp,
                            bandId,
                            objectId,
                            meldung -> Platform.runLater(() -> {
                                txtStatus.appendText(meldung + System.lineSeparator());
                                txtStatus.positionCaret(txtStatus.getText().length());
                            })
                    );
                }
            };

            task.setOnSucceeded(event -> {
                OcrDownloadErgebnis ergebnis = task.getValue();

                txtStatus.appendText(System.lineSeparator());
                txtStatus.appendText(ergebnis.meldung() + System.lineSeparator());
                txtStatus.appendText("Gestartet: " + ergebnis.gestartet() + System.lineSeparator());
                txtStatus.appendText("Mapping-Seiten: " + ergebnis.mappingSeiten() + System.lineSeparator());
                txtStatus.appendText("Erfolgreich gespeichert: " + ergebnis.erfolgreich() + System.lineSeparator());
                txtStatus.appendText("Ohne OCR/Text: " + ergebnis.ohneOcr() + System.lineSeparator());
                txtStatus.appendText("Fehler: " + ergebnis.fehler() + System.lineSeparator());

                btnStart.setDisable(false);
                txtObjectId.setDisable(false);
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();

                txtStatus.appendText(System.lineSeparator());
                txtStatus.appendText("Fehler beim OCR-Import." + System.lineSeparator());

                if (ex != null && ex.getMessage() != null) {
                    txtStatus.appendText(ex.getMessage() + System.lineSeparator());
                }

                btnStart.setDisable(false);
                txtObjectId.setDisable(false);
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        Button btnAbbrechen = new Button("Schließen");
        btnAbbrechen.setOnAction(e -> dialog.close());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(lblBand, 0, 0);
        grid.add(lblBandWert, 1, 0);

        grid.add(lblBandId, 0, 1);
        grid.add(lblBandIdWert, 1, 1);

        grid.add(lblArchivTyp, 0, 2);
        grid.add(lblArchivTypWert, 1, 2);

        grid.add(new Label("BSB/MDZ Object-ID:"), 0, 3);
        grid.add(txtObjectId, 1, 3);

        ColumnConstraints col1 = new ColumnConstraints();
        col1.setMinWidth(130);

        ColumnConstraints col2 = new ColumnConstraints();
        col2.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(col1, col2);

        HBox buttons = new HBox(8, btnStart, btnAbbrechen);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        root.getChildren().addAll(
                lblTitel,
                grid,
                lblHinweis,
                txtStatus,
                buttons
        );

        Scene scene = new Scene(root, 560, 380);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static String getArchivTypAnzeige(OcrArchivTyp archivTyp) {

        if (archivTyp == OcrArchivTyp.BSB_MDZ) {
            return "BSB/MDZ Digitale Sammlungen";
        }

        return "Unbekannte OCR-Quelle";
    }
}