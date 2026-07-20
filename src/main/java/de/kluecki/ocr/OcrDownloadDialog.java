package de.kluecki.ocr;

import de.kluecki.ocr.OcrDownloadService.OcrDownloadErgebnis;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.DirectoryChooser;
import java.io.File;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Dialog für den OCR-Download/-Import.
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

        OcrArchivTyp startArchivTyp = OcrArchivTyp.BSB_MDZ;

        String startArchivAnzeige = getArchivTypAnzeige(startArchivTyp);
        String startIdLabelText = getArchivTypIdLabel(startArchivTyp);
        String startIdPromptText = getArchivTypIdPrompt(startArchivTyp);

        dialog.setTitle(startArchivAnzeige + "-OCR herunterladen / importieren");

        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        dialog.setResizable(true);
        dialog.setMinWidth(760);
        dialog.setMinHeight(560);

        Label lblTitel = new Label(startArchivAnzeige + "-OCR für Band herunterladen");
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

        ComboBox<OcrArchivTyp> cmbArchivTyp = new ComboBox<>();
        cmbArchivTyp.getItems().addAll(
                OcrArchivTyp.BSB_MDZ,
                OcrArchivTyp.BLB_KARLSRUHE,
                OcrArchivTyp.LOKALE_ALTO_DATEIEN
        );
        cmbArchivTyp.setValue(startArchivTyp);
        cmbArchivTyp.setMaxWidth(Double.MAX_VALUE);

        cmbArchivTyp.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(OcrArchivTyp item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : getArchivTypAnzeige(item));
            }
        });

        cmbArchivTyp.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(OcrArchivTyp item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : getArchivTypAnzeige(item));
            }
        });

        Label lblObjectId = new Label(startIdLabelText);
        TextField txtObjectId = new TextField();
        txtObjectId.setPromptText(startIdPromptText);
        txtObjectId.setPrefColumnCount(22);

        Button btnOrdnerAuswaehlen = new Button("Ordner auswählen");

        btnOrdnerAuswaehlen.setVisible(false);
        btnOrdnerAuswaehlen.setManaged(false);

        btnOrdnerAuswaehlen.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Ordner mit ALTO-XML-Dateien auswählen");

            File ausgewaehlterOrdner = chooser.showDialog(dialog);

            if (ausgewaehlterOrdner != null) {
                txtObjectId.setText(ausgewaehlterOrdner.getAbsolutePath());
            }
        });

        CheckBox chkBlbBereich = new CheckBox("BLB Manifest-ID-Bereich importieren");

        CheckBox chkVorhandeneOcrUeberspringen = new CheckBox("Vorhandene OCR-Seiten überspringen (empfohlen)");
        chkVorhandeneOcrUeberspringen.setSelected(true);
        chkVorhandeneOcrUeberspringen.setTooltip(new Tooltip(
                "Wenn aktiv, werden Seiten mit bereits vorhandener OCR nicht erneut beim Archiv angefragt."
        ));

        Label lblManifestVon = new Label("Manifest-ID von:");
        TextField txtManifestVon = new TextField();
        txtManifestVon.setPromptText("z. B. 7010966");
        txtManifestVon.setPrefColumnCount(10);

        Label lblManifestBis = new Label("Manifest-ID bis:");
        TextField txtManifestBis = new TextField();

        Label lblAltoSeiteVon = new Label("Bildseite von:");
        TextField txtAltoSeiteVon = new TextField();
        txtAltoSeiteVon.setPromptText("z. B. 6");
        txtAltoSeiteVon.setPrefColumnCount(10);

        Label lblAltoSeiteBis = new Label("Bildseite bis:");
        TextField txtAltoSeiteBis = new TextField();
        txtAltoSeiteBis.setPromptText("z. B. 1026");
        txtAltoSeiteBis.setPrefColumnCount(10);

        lblAltoSeiteVon.setVisible(false);
        lblAltoSeiteVon.setManaged(false);
        txtAltoSeiteVon.setVisible(false);
        txtAltoSeiteVon.setManaged(false);

        lblAltoSeiteBis.setVisible(false);
        lblAltoSeiteBis.setManaged(false);
        txtAltoSeiteBis.setVisible(false);
        txtAltoSeiteBis.setManaged(false);

        txtManifestBis.setPromptText("z. B. 7010990");
        txtManifestBis.setPrefColumnCount(10);

        lblManifestVon.setVisible(false);
        lblManifestVon.setManaged(false);
        txtManifestVon.setVisible(false);
        txtManifestVon.setManaged(false);

        lblManifestBis.setVisible(false);
        lblManifestBis.setManaged(false);
        txtManifestBis.setVisible(false);
        txtManifestBis.setManaged(false);

        chkBlbBereich.setVisible(false);
        chkBlbBereich.setManaged(false);

        Label lblHinweis = new Label("""
                Hinweis:
                Der OCR-Import darf erst gestartet werden, wenn für diesen Band
                ein Grundmapping / SeitenMapping vorhanden ist.
                Die OCR-Zuordnung erfolgt ausschließlich über die vorhandenen
                Dateinamen aus dem SeitenMapping.
                """);
        lblHinweis.setWrapText(true);
        lblHinweis.setMaxWidth(700);
        lblHinweis.setStyle("""
                -fx-font-size: 12px;
                -fx-text-fill: #555555;
                """);

        TextArea txtStatus = new TextArea();
        txtStatus.setEditable(false);
        txtStatus.setWrapText(true);
        txtStatus.setPrefRowCount(14);
        txtStatus.setPrefHeight(260);
        txtStatus.setPromptText("Hier erscheint später der Fortschritt des OCR-Imports.");

        Button btnStart = new Button("Import starten");
        btnStart.setDisable(true);

        Runnable aktualisiereStartButton = () -> {

            OcrArchivTyp aktuellerTyp = cmbArchivTyp.getValue();

            if (aktuellerTyp == null) {
                btnStart.setDisable(true);
                return;
            }

            if (aktuellerTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {

                boolean ordnerLeer = txtObjectId.getText() == null
                        || txtObjectId.getText().trim().isBlank();

                boolean seiteVonLeer = txtAltoSeiteVon.getText() == null
                        || txtAltoSeiteVon.getText().trim().isBlank();

                boolean seiteBisLeer = txtAltoSeiteBis.getText() == null
                        || txtAltoSeiteBis.getText().trim().isBlank();

                btnStart.setDisable(
                        ordnerLeer
                                || seiteVonLeer
                                || seiteBisLeer
                );

                return;
            }

            if (aktuellerTyp == OcrArchivTyp.BLB_KARLSRUHE
                    && chkBlbBereich.isSelected()) {

                boolean vonLeer = txtManifestVon.getText() == null
                        || txtManifestVon.getText().trim().isBlank();

                boolean bisLeer = txtManifestBis.getText() == null
                        || txtManifestBis.getText().trim().isBlank();

                btnStart.setDisable(vonLeer || bisLeer);

                return;
            }

            boolean idLeer = txtObjectId.getText() == null
                    || txtObjectId.getText().trim().isBlank();

            btnStart.setDisable(idLeer);
        };

        chkBlbBereich.selectedProperty().addListener((obs, alt, neu) -> {

            boolean bereichAktiv = neu != null && neu;

            lblObjectId.setVisible(!bereichAktiv);
            lblObjectId.setManaged(!bereichAktiv);
            txtObjectId.setVisible(!bereichAktiv);
            txtObjectId.setManaged(!bereichAktiv);

            lblManifestVon.setVisible(bereichAktiv);
            lblManifestVon.setManaged(bereichAktiv);
            txtManifestVon.setVisible(bereichAktiv);
            txtManifestVon.setManaged(bereichAktiv);

            lblManifestBis.setVisible(bereichAktiv);
            lblManifestBis.setManaged(bereichAktiv);
            txtManifestBis.setVisible(bereichAktiv);
            txtManifestBis.setManaged(bereichAktiv);

            aktualisiereStartButton.run();
        });

        txtObjectId.textProperty().addListener(
                (obs, alt, neu) -> aktualisiereStartButton.run()
        );

        txtAltoSeiteVon.textProperty().addListener(
                (obs, alt, neu) -> aktualisiereStartButton.run()
        );

        txtAltoSeiteBis.textProperty().addListener(
                (obs, alt, neu) -> aktualisiereStartButton.run()
        );

        txtManifestVon.textProperty().addListener((obs, alt, neu) -> aktualisiereStartButton.run());
        txtManifestBis.textProperty().addListener((obs, alt, neu) -> aktualisiereStartButton.run());

        cmbArchivTyp.valueProperty().addListener((obs, alterTyp, neuerTyp) -> {

            if (neuerTyp == null) {
                return;
            }

            String neueArchivAnzeige = getArchivTypAnzeige(neuerTyp);

            dialog.setTitle(neueArchivAnzeige + "-OCR herunterladen / importieren");
            lblTitel.setText(neueArchivAnzeige + "-OCR für Band herunterladen");

            lblObjectId.setText(getArchivTypIdLabel(neuerTyp));
            txtObjectId.setPromptText(getArchivTypIdPrompt(neuerTyp));
            txtObjectId.clear();

            boolean istBlb = neuerTyp == OcrArchivTyp.BLB_KARLSRUHE;
            boolean istBsbMdz = neuerTyp == OcrArchivTyp.BSB_MDZ;

            boolean istLokalesAlto =
                    neuerTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN;

            lblAltoSeiteVon.setVisible(istLokalesAlto);
            lblAltoSeiteVon.setManaged(istLokalesAlto);
            txtAltoSeiteVon.setVisible(istLokalesAlto);
            txtAltoSeiteVon.setManaged(istLokalesAlto);

            lblAltoSeiteBis.setVisible(istLokalesAlto);
            lblAltoSeiteBis.setManaged(istLokalesAlto);
            txtAltoSeiteBis.setVisible(istLokalesAlto);
            txtAltoSeiteBis.setManaged(istLokalesAlto);

            btnOrdnerAuswaehlen.setVisible(istLokalesAlto);
            btnOrdnerAuswaehlen.setManaged(istLokalesAlto);

            txtObjectId.setEditable(!istLokalesAlto);

            chkBlbBereich.setVisible(istBlb);
            chkBlbBereich.setManaged(istBlb);

            chkVorhandeneOcrUeberspringen.setDisable(!istBsbMdz);

            if (istBsbMdz) {
                chkVorhandeneOcrUeberspringen.setSelected(true);
            } else {
                chkVorhandeneOcrUeberspringen.setSelected(false);
            }

            if (!istBlb) {
                chkBlbBereich.setSelected(false);
            }

            txtManifestVon.clear();
            txtManifestBis.clear();

            btnStart.setDisable(true);
        });

        btnStart.setOnAction(e -> {

            String objectId = txtObjectId.getText() != null
                    ? txtObjectId.getText().trim()
                    : "";

            String manifestVonText = txtManifestVon.getText() != null
                    ? txtManifestVon.getText().trim()
                    : "";

            String manifestBisText = txtManifestBis.getText() != null
                    ? txtManifestBis.getText().trim()
                    : "";

            String altoSeiteVonText = txtAltoSeiteVon.getText() != null
                    ? txtAltoSeiteVon.getText().trim()
                    : "";

            String altoSeiteBisText = txtAltoSeiteBis.getText() != null
                    ? txtAltoSeiteBis.getText().trim()
                    : "";

            OcrArchivTyp archivTyp = cmbArchivTyp.getValue();

            boolean blbBereichAktiv = archivTyp == OcrArchivTyp.BLB_KARLSRUHE
                    && chkBlbBereich.isSelected();

            if (archivTyp == null) {
                txtStatus.setText("Bitte zuerst eine OCR-Quelle auswählen.");
                return;
            }

            String archivAnzeige = getArchivTypAnzeige(archivTyp);
            String idLabelText = getArchivTypIdLabel(archivTyp);

            if (archivTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {

                if (objectId.isBlank()) {
                    txtStatus.setText("Bitte zuerst einen ALTO-Ordner auswählen.");
                    return;
                }

                if (altoSeiteVonText.isBlank() || altoSeiteBisText.isBlank()) {
                    txtStatus.setText(
                            "Bitte Bildseite von und Bildseite bis eingeben."
                    );
                    return;
                }

                int altoSeiteVon;
                int altoSeiteBis;

                try {
                    altoSeiteVon = Integer.parseInt(altoSeiteVonText);
                    altoSeiteBis = Integer.parseInt(altoSeiteBisText);
                } catch (NumberFormatException ex) {
                    txtStatus.setText(
                            "Bildseite von und Bildseite bis müssen ganze Zahlen sein."
                    );
                    return;
                }

                if (altoSeiteVon < 0 || altoSeiteBis < 0) {
                    txtStatus.setText(
                            "Die Bildseitennummern dürfen nicht negativ sein."
                    );
                    return;
                }

                if (altoSeiteVon > altoSeiteBis) {
                    txtStatus.setText(
                            "Bildseite von darf nicht größer als Bildseite bis sein."
                    );
                    return;
                }

                String bandJahr = ermittleJahr(bandAnzeige);
                String ordnerJahr = ermittleJahr(objectId);

                if (bandJahr != null
                        && ordnerJahr != null
                        && !bandJahr.equals(ordnerJahr)) {

                    Alert warnung = new Alert(
                            Alert.AlertType.CONFIRMATION
                    );

                    warnung.initOwner(dialog);
                    warnung.setTitle("Jahr stimmt nicht überein");
                    warnung.setHeaderText(
                            "Der ausgewählte Band und der ALTO-Ordner "
                                    + "haben unterschiedliche Jahresangaben."
                    );

                    warnung.setContentText(
                            "Ausgewählter Band: "
                                    + bandAnzeige
                                    + System.lineSeparator()
                                    + "Erkanntes Bandjahr: "
                                    + bandJahr
                                    + System.lineSeparator()
                                    + System.lineSeparator()
                                    + "ALTO-Ordner: "
                                    + objectId
                                    + System.lineSeparator()
                                    + "Erkanntes Ordnerjahr: "
                                    + ordnerJahr
                                    + System.lineSeparator()
                                    + System.lineSeparator()
                                    + "Soll der Import trotzdem gestartet werden?"
                    );

                    ButtonType btnTrotzdem =
                            new ButtonType("Trotzdem importieren");

                    ButtonType btnAbbrechen =
                            new ButtonType(
                                    "Abbrechen",
                                    ButtonBar.ButtonData.CANCEL_CLOSE
                            );

                    warnung.getButtonTypes().setAll(
                            btnTrotzdem,
                            btnAbbrechen
                    );

                    Optional<ButtonType> ergebnis =
                            warnung.showAndWait();

                    if (ergebnis.isEmpty()
                            || ergebnis.get() != btnTrotzdem) {
                        txtStatus.setText(
                                "Import wegen abweichender Jahresangaben abgebrochen."
                        );
                        return;
                    }
                }
            }

            if (blbBereichAktiv) {

                if (manifestVonText.isBlank() || manifestBisText.isBlank()) {
                    txtStatus.setText("Bitte Manifest-ID von und Manifest-ID bis eingeben.");
                    return;
                }

                try {
                    Integer.parseInt(manifestVonText);
                    Integer.parseInt(manifestBisText);
                } catch (NumberFormatException ex) {
                    txtStatus.setText("Manifest-ID von/bis müssen ganze Zahlen sein.");
                    return;
                }

            } else {

                if (objectId.isBlank()) {
                    txtStatus.setText("Bitte zuerst eine " + idLabelText + " eingeben.");
                    return;
                }
            }

            btnStart.setDisable(true);
            txtObjectId.setDisable(true);
            txtManifestVon.setDisable(true);
            txtManifestBis.setDisable(true);
            chkBlbBereich.setDisable(true);
            chkVorhandeneOcrUeberspringen.setDisable(true);
            cmbArchivTyp.setDisable(true);
            txtStatus.clear();

            txtStatus.appendText("OCR-Quelle: " + archivAnzeige + System.lineSeparator());

            if (archivTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {

                txtStatus.appendText(
                        "ALTO-Ordner: "
                                + objectId
                                + System.lineSeparator()
                );

                txtStatus.appendText(
                        "Bildseite von: "
                                + altoSeiteVonText
                                + System.lineSeparator()
                );

                txtStatus.appendText(
                        "Bildseite bis: "
                                + altoSeiteBisText
                                + System.lineSeparator()
                );

            } else if (blbBereichAktiv) {

                txtStatus.appendText(
                        "BLB Manifest-ID von: "
                                + manifestVonText
                                + System.lineSeparator()
                );

                txtStatus.appendText(
                        "BLB Manifest-ID bis: "
                                + manifestBisText
                                + System.lineSeparator()
                );

            } else {

                txtStatus.appendText(
                        idLabelText
                                + " "
                                + objectId
                                + System.lineSeparator()
                );
            }

            txtStatus.appendText("Import wird vorbereitet..." + System.lineSeparator());
            txtStatus.appendText(System.lineSeparator());
            OcrDownloadService service = new OcrDownloadService();

            Task<OcrDownloadErgebnis> task = new Task<>() {
                @Override
                protected OcrDownloadErgebnis call() {

                    if (archivTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {

                        int altoSeiteVon = Integer.parseInt(altoSeiteVonText);
                        int altoSeiteBis = Integer.parseInt(altoSeiteBisText);

                        return service.importiereLokaleAltoDateien(
                                bandId,
                                objectId,
                                altoSeiteVon,
                                altoSeiteBis,
                                meldung -> Platform.runLater(() -> {
                                    txtStatus.appendText(
                                            meldung + System.lineSeparator()
                                    );

                                    txtStatus.positionCaret(
                                            txtStatus.getText().length()
                                    );
                                })
                        );
                    }

                    if (blbBereichAktiv) {
                        int manifestVon = Integer.parseInt(manifestVonText);
                        int manifestBis = Integer.parseInt(manifestBisText);

                        return service.downloadUndImportiereBlbOcrFuerManifestBereich(
                                bandId,
                                manifestVon,
                                manifestBis,
                                meldung -> Platform.runLater(() -> {
                                    txtStatus.appendText(meldung + System.lineSeparator());
                                    txtStatus.positionCaret(txtStatus.getText().length());
                                })
                        );
                    }

                    return service.downloadUndImportiereOcrFuerBand(
                            archivTyp,
                            bandId,
                            objectId,
                            chkVorhandeneOcrUeberspringen.isSelected(),
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

                if (blbBereichAktiv) {
                    txtStatus.appendText("Geprüfte Manifest-IDs: " + ergebnis.mappingSeiten() + System.lineSeparator());
                } else {
                    txtStatus.appendText("Mapping-Seiten: " + ergebnis.mappingSeiten() + System.lineSeparator());
                }

                txtStatus.appendText("Erfolgreich gespeichert: " + ergebnis.erfolgreich() + System.lineSeparator());
                txtStatus.appendText("OCR nachholen (eigene OCR): " + ergebnis.ohneOcr() + System.lineSeparator());
                txtStatus.appendText("Fehler: " + ergebnis.fehler() + System.lineSeparator());

                txtObjectId.setDisable(false);
                txtManifestVon.setDisable(false);
                txtManifestBis.setDisable(false);
                chkBlbBereich.setDisable(false);
                chkVorhandeneOcrUeberspringen.setDisable(cmbArchivTyp.getValue() != OcrArchivTyp.BSB_MDZ);
                cmbArchivTyp.setDisable(false);
                btnStart.setDisable(false);
            });

            task.setOnFailed(event -> {
                Throwable ex = task.getException();

                txtStatus.appendText(System.lineSeparator());
                txtStatus.appendText("Fehler beim OCR-Import." + System.lineSeparator());

                if (ex != null && ex.getMessage() != null) {
                    txtStatus.appendText(ex.getMessage() + System.lineSeparator());
                }

                txtObjectId.setDisable(false);
                txtManifestVon.setDisable(false);
                txtManifestBis.setDisable(false);
                chkBlbBereich.setDisable(false);
                cmbArchivTyp.setDisable(false);
                btnStart.setDisable(false);
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        Button btnAbbrechen = new Button("Schließen");
        btnAbbrechen.setOnAction(e -> dialog.close());

        HBox objectIdBereich = new HBox(8, txtObjectId, btnOrdnerAuswaehlen);
        HBox.setHgrow(txtObjectId, Priority.ALWAYS);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(lblBand, 0, 0);
        grid.add(lblBandWert, 1, 0);

        grid.add(lblBandId, 0, 1);
        grid.add(lblBandIdWert, 1, 1);

        grid.add(lblArchivTyp, 0, 2);
        grid.add(cmbArchivTyp, 1, 2);

        grid.add(lblObjectId, 0, 3);
        grid.add(objectIdBereich, 1, 3);

        grid.add(chkVorhandeneOcrUeberspringen, 1, 4);

        grid.add(chkBlbBereich, 1, 5);

        grid.add(lblManifestVon, 0, 6);
        grid.add(txtManifestVon, 1, 6);

        grid.add(lblManifestBis, 0, 7);
        grid.add(txtManifestBis, 1, 7);

        grid.add(lblAltoSeiteVon, 0, 8);
        grid.add(txtAltoSeiteVon, 1, 8);

        grid.add(lblAltoSeiteBis, 0, 9);
        grid.add(txtAltoSeiteBis, 1, 9);

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

        VBox.setVgrow(txtStatus, Priority.ALWAYS);

        Scene scene = new Scene(root, 760, 560);
        dialog.setScene(scene);
        dialog.showAndWait();
    }

    private static String getArchivTypAnzeige(OcrArchivTyp archivTyp) {

        if (archivTyp == OcrArchivTyp.BSB_MDZ) {
            return "BSB/MDZ Digitale Sammlungen";
        }

        if (archivTyp == OcrArchivTyp.BLB_KARLSRUHE) {
            return "BLB Karlsruhe Digitale Sammlungen";
        }

        if (archivTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {
            return "Lokale ALTO-Dateien";
        }

        return "Unbekannte OCR-Quelle";
    }

    private static String getArchivTypIdLabel(OcrArchivTyp archivTyp) {

        if (archivTyp == OcrArchivTyp.BSB_MDZ) {
            return "BSB/MDZ Object-ID:";
        }

        if (archivTyp == OcrArchivTyp.BLB_KARLSRUHE) {
            return "BLB Manifest-ID:";
        }

        if (archivTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {
            return "ALTO-Ordner:";
        }

        return "OCR Objekt-/Manifest-ID:";
    }

    private static String getArchivTypIdPrompt(OcrArchivTyp archivTyp) {

        if (archivTyp == OcrArchivTyp.BSB_MDZ) {
            return "z. B. bsb10335662";
        }

        if (archivTyp == OcrArchivTyp.BLB_KARLSRUHE) {
            return "z. B. 7010966";
        }

        if (archivTyp == OcrArchivTyp.LOKALE_ALTO_DATEIEN) {
            return "Lokalen Ordner mit ALTO-XML-Dateien auswählen";
        }

        return "z. B. Objekt- oder Manifest-ID";
    }

    private static String ermittleJahr(String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        Pattern pattern = Pattern.compile(
                "(?<!\\d)(18\\d{2}|19\\d{2}|20\\d{2})(?!\\d)"
        );

        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return null;
    }
}