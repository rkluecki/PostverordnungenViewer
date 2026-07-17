package de.kluecki.ocr;

import de.kluecki.db.model.OcrPruefungEintrag;
import de.kluecki.db.repository.OcrPruefungRepository;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.scene.input.MouseButton;
import java.util.function.Consumer;
import de.kluecki.db.repository.OcrPruefentscheidungRepository;
import javafx.application.Platform;
import java.util.Optional;
import java.util.List;

public final class OcrPruefungDialog {

    private OcrPruefungDialog() {
    }

    public static void show(
            Stage ownerStage,
            int bandID,
            String bandAnzeige,
            Consumer<OcrPruefungEintrag> onTrefferOeffnen
    ) {
        Stage stage = new Stage();

        stage.setTitle("OCR-Import prüfen");
        stage.initModality(Modality.WINDOW_MODAL);

        if (ownerStage != null) {
            stage.initOwner(ownerStage);
        }

        Label lblBand = new Label(
                bandAnzeige != null && !bandAnzeige.isBlank()
                        ? bandAnzeige
                        : "BandID " + bandID
        );

        lblBand.setStyle("""
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                -fx-text-fill: #263b2f;
                """);

        Label lblErgebnis = new Label("Prüfung wurde noch nicht gestartet.");

        ComboBox<String> cmbAnsicht = new ComboBox<>();

        cmbAnsicht.getItems().addAll(
                "Offene Auffälligkeiten",
                "Geklärte Auffälligkeiten",
                "Alle Auffälligkeiten"
        );

        cmbAnsicht.getSelectionModel().selectFirst();
        cmbAnsicht.setPrefWidth(190);

        Button btnPruefen = new Button("Aktualisieren");
        Button btnSchliessen = new Button("Schließen");

        Button btnEntscheidung =
                new Button("Prüfentscheidung festhalten");

        btnEntscheidung.setDisable(true);

        TableView<OcrPruefungEintrag> table = new TableView<>();

        table.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, alterEintrag, neuerEintrag) ->
                        btnEntscheidung.setDisable(
                                neuerEintrag == null
                        )
                );

        table.setPlaceholder(
                new Label("Keine Prüfergebnisse vorhanden")
        );

        table.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN
        );

        table.setStyle("""
            -fx-table-cell-border-color: #d8d2c6;
            -fx-table-header-border-color: #b8b2a8;
            -fx-focus-color: transparent;
            -fx-faint-focus-color: transparent;
            """);

        TableColumn<OcrPruefungEintrag, Number> colBildIndex =
                new TableColumn<>("Bildindex");

        colBildIndex.setCellValueFactory(cellData ->
                new SimpleIntegerProperty(
                        cellData.getValue().getBildIndex()
                )
        );

        colBildIndex.setPrefWidth(90);
        colBildIndex.setSortable(false);

        TableColumn<OcrPruefungEintrag, String> colDateiname =
                new TableColumn<>("Dateiname");

        colDateiname.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getDateiname()
                        )
                )
        );

        colDateiname.setPrefWidth(250);
        colDateiname.setSortable(false);

        TableColumn<OcrPruefungEintrag, String> colLogischeSeite =
                new TableColumn<>("Logische Seite");

        colLogischeSeite.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getLogischeSeite()
                        )
                )
        );

        colLogischeSeite.setPrefWidth(110);
        colLogischeSeite.setSortable(false);

        TableColumn<OcrPruefungEintrag, String> colQuelle =
                new TableColumn<>("OCR-Quelle");

        colQuelle.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getOcrQuelle()
                        )
                )
        );

        colQuelle.setPrefWidth(120);
        colQuelle.setSortable(false);

        TableColumn<OcrPruefungEintrag, String> colFormat =
                new TableColumn<>("OCR-Format");

        colFormat.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getOcrFormat()
                        )
                )
        );

        colFormat.setPrefWidth(110);
        colFormat.setSortable(false);

        TableColumn<OcrPruefungEintrag, String> colStatus =
                new TableColumn<>("Status");

        colStatus.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getOcrStatus()
                        )
                )
        );

        colStatus.setPrefWidth(180);
        colStatus.setSortable(false);

        TableColumn<OcrPruefungEintrag, String> colEntscheidung =
                new TableColumn<>("Prüfentscheidung");

        colEntscheidung.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        bezeichnungEntscheidungsart(
                                cellData.getValue().getEntscheidungsart()
                        )
                )
        );

        colEntscheidung.setPrefWidth(190);
        colEntscheidung.setSortable(false);


        TableColumn<OcrPruefungEintrag, String> colPruefBemerkung =
                new TableColumn<>("Bemerkung");

        colPruefBemerkung.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getPruefBemerkung()
                        )
                )
        );

        colPruefBemerkung.setPrefWidth(300);
        colPruefBemerkung.setSortable(false);


        TableColumn<OcrPruefungEintrag, String> colGepruefteQuelle =
                new TableColumn<>("Geprüfte Quelle");

        colGepruefteQuelle.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        nullZuLeer(
                                cellData.getValue().getGepruefteQuelle()
                        )
                )
        );

        colGepruefteQuelle.setPrefWidth(220);
        colGepruefteQuelle.setSortable(false);

        table.getColumns().addAll(
                colBildIndex,
                colDateiname,
                colLogischeSeite,
                colQuelle,
                colFormat,
                colStatus,
                colEntscheidung,
                colPruefBemerkung,
                colGepruefteQuelle
        );

        table.setRowFactory(tv -> new TableRow<>() {

            {
                selectedProperty().addListener((obs, alt, ausgewaehlt) ->
                        aktualisiereZeilenStil()
                );
            }

            @Override
            protected void updateItem(
                    OcrPruefungEintrag item,
                    boolean empty
            ) {
                super.updateItem(item, empty);
                aktualisiereZeilenStil();
            }

            private void aktualisiereZeilenStil() {

                OcrPruefungEintrag item = getItem();

                if (item == null || isEmpty()) {
                    setStyle("");
                    return;
                }

                if (isSelected()) {
                    setStyle("""
                    -fx-background-color: #b7d8cb;
                    -fx-text-fill: #1f2f27;
                    -fx-border-color: transparent transparent #8faf9f transparent;
                    -fx-border-width: 0 0 1 0;
                    """);
                    return;
                }

                if ("Kein OCR-Datensatz".equals(item.getOcrStatus())) {
                    setStyle("""
                    -fx-background-color: #f6dfbd;
                    -fx-text-fill: #5a3b12;
                    -fx-border-color: transparent transparent #d8c39f transparent;
                    -fx-border-width: 0 0 1 0;
                    """);
                } else if ("OCR leer".equals(item.getOcrStatus())) {
                    setStyle("""
                    -fx-background-color: #f3ebc8;
                    -fx-text-fill: #514817;
                    -fx-border-color: transparent transparent #d6cfa8 transparent;
                    -fx-border-width: 0 0 1 0;
                    """);
                } else {
                    setStyle("");
                }
            }
        });

        table.setOnMouseClicked(event -> {

            if (event.getButton() != MouseButton.PRIMARY
                    || event.getClickCount() != 2) {
                return;
            }

            OcrPruefungEintrag eintrag =
                    table.getSelectionModel().getSelectedItem();

            if (eintrag == null) {
                return;
            }

            if (onTrefferOeffnen != null) {
                onTrefferOeffnen.accept(eintrag);
            }
        });

        Runnable pruefungLaden = () -> {

            btnPruefen.setDisable(true);
            cmbAnsicht.setDisable(true);
            lblErgebnis.setText("OCR-Prüfung läuft...");

            try {
                OcrPruefungRepository repository =
                        new OcrPruefungRepository();

                String ansicht =
                        ermittleAnsichtCode(
                                cmbAnsicht.getValue()
                        );

                List<OcrPruefungEintrag> ergebnisse =
                        repository.pruefeBand(
                                bandID,
                                ansicht
                        );

                table.setItems(
                        FXCollections.observableArrayList(ergebnisse)
                );

                table.getSelectionModel().clearSelection();

                if (ergebnisse.isEmpty()) {
                    lblErgebnis.setText(
                            "Keine Auffälligkeiten gefunden."
                    );
                } else if (ergebnisse.size() == 1) {
                    lblErgebnis.setText(
                            "1 Auffälligkeit gefunden."
                    );
                } else {
                    lblErgebnis.setText(
                            ergebnisse.size()
                                    + " Auffälligkeiten gefunden."
                    );
                }

            } catch (Exception ex) {
                ex.printStackTrace();

                Alert alert = new Alert(
                        Alert.AlertType.ERROR
                );

                alert.initOwner(stage);
                alert.setTitle("OCR-Prüfung");
                alert.setHeaderText(
                        "Die OCR-Prüfung ist fehlgeschlagen."
                );

                alert.setContentText(
                        ex.getMessage() != null
                                ? ex.getMessage()
                                : "Unbekannter Fehler"
                );

                alert.showAndWait();

                lblErgebnis.setText(
                        "OCR-Prüfung fehlgeschlagen."
                );

            } finally {
                btnPruefen.setDisable(false);
                cmbAnsicht.setDisable(false);
            }
        };

        btnPruefen.setOnAction(e ->
                pruefungLaden.run()
        );

        cmbAnsicht.setOnAction(e ->
                pruefungLaden.run()
        );

        btnEntscheidung.setOnAction(e -> {

            OcrPruefungEintrag eintrag =
                    table.getSelectionModel().getSelectedItem();

            if (eintrag == null) {
                return;
            }

            Optional<OcrPruefentscheidungDialog.Ergebnis> ergebnis =
                    OcrPruefentscheidungDialog.showAndWait(
                            stage,
                            eintrag.getDateiname(),
                            eintrag.getOcrStatus(),
                            eintrag.getEntscheidungsart(),
                            eintrag.getPruefBemerkung(),
                            eintrag.getGepruefteQuelle(),
                            eintrag.getIstErledigt()
                    );

            if (ergebnis.isEmpty()) {
                return;
            }

            OcrPruefentscheidungDialog.Ergebnis entscheidung =
                    ergebnis.get();

            try {
                OcrPruefentscheidungRepository repository =
                        new OcrPruefentscheidungRepository();

                repository.speichernOderAktualisieren(
                        eintrag.getBandID(),
                        eintrag.getBildIndex(),
                        entscheidung.entscheidungsart(),
                        entscheidung.bemerkung(),
                        entscheidung.gepruefteQuelle(),
                        entscheidung.istErledigt()
                );

                Alert alert = new Alert(
                        Alert.AlertType.INFORMATION
                );

                alert.initOwner(stage);
                alert.setTitle("OCR-Prüfentscheidung");
                alert.setHeaderText(
                        "Die Prüfentscheidung wurde gespeichert."
                );

                alert.setContentText(
                        eintrag.getDateiname()
                );

                alert.showAndWait();
                pruefungLaden.run();

            } catch (Exception ex) {
                ex.printStackTrace();

                Alert alert = new Alert(
                        Alert.AlertType.ERROR
                );

                alert.initOwner(stage);
                alert.setTitle("OCR-Prüfentscheidung");
                alert.setHeaderText(
                        "Die Prüfentscheidung konnte nicht gespeichert werden."
                );

                alert.setContentText(
                        ex.getMessage() != null
                                ? ex.getMessage()
                                : "Unbekannter Fehler"
                );

                alert.showAndWait();
            }
        });

        btnSchliessen.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonLeiste = new HBox(
                10,
                btnPruefen,
                btnEntscheidung,
                lblErgebnis,
                spacer,
                btnSchliessen
        );

        buttonLeiste.setAlignment(Pos.CENTER_LEFT);

        HBox ansichtLeiste = new HBox(
                8,
                new Label("Ansicht:"),
                cmbAnsicht
        );

        ansichtLeiste.setAlignment(Pos.CENTER_LEFT);

        VBox kopf = new VBox(
                8,
                new Label("Ausgewählter Band:"),
                lblBand,
                ansichtLeiste
        );

        kopf.setPadding(new Insets(0, 0, 10, 0));

        BorderPane root = new BorderPane();

        root.setPadding(new Insets(12));
        root.setTop(kopf);
        root.setCenter(table);
        root.setBottom(buttonLeiste);

        BorderPane.setMargin(
                buttonLeiste,
                new Insets(10, 0, 0, 0)
        );

        root.setStyle("""
                -fx-background-color: #f7f6ef;
                """);

        Scene scene = new Scene(root, 950, 620);

        stage.setScene(scene);
        stage.show();

        Platform.runLater(
                pruefungLaden
        );
    }

    private static String nullZuLeer(String wert) {
        return wert != null ? wert : "";
    }

    private static String ermittleAnsichtCode(
            String ansichtText
    ) {
        if ("Geklärte Auffälligkeiten".equals(ansichtText)) {
            return "GEKLAERT";
        }

        if ("Alle Auffälligkeiten".equals(ansichtText)) {
            return "ALLE";
        }

        return "OFFEN";
    }

    private static String bezeichnungEntscheidungsart(
            String entscheidungsart
    ) {
        if (entscheidungsart == null
                || entscheidungsart.isBlank()) {
            return "";
        }

        return switch (entscheidungsart) {
            case "QUELLE_OHNE_OCR" ->
                    "Quelle liefert kein OCR";

            case "LEERE_SEITE" ->
                    "Leere Seite";

            case "OCR_FEHLERHAFT" ->
                    "OCR-Datei fehlerhaft";

            case "ZUORDNUNG_UNKLAR" ->
                    "OCR-Zuordnung unklar";

            case "OCR_NICHT_ERFORDERLICH" ->
                    "OCR nicht erforderlich";

            case "MANUELLE_NACHARBEIT" ->
                    "Manuelle Nachbearbeitung";

            case "SONSTIGES" ->
                    "Sonstiger Sonderfall";

            default ->
                    entscheidungsart;
        };
    }
}