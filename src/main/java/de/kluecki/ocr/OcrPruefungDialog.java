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

        Button btnPruefen = new Button("Prüfung starten");
        Button btnSchliessen = new Button("Schließen");

        TableView<OcrPruefungEintrag> table = new TableView<>();

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

        table.getColumns().addAll(
                colBildIndex,
                colDateiname,
                colLogischeSeite,
                colQuelle,
                colFormat,
                colStatus
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

        btnPruefen.setOnAction(e -> {

            btnPruefen.setDisable(true);
            lblErgebnis.setText("OCR-Prüfung läuft...");

            try {
                OcrPruefungRepository repository =
                        new OcrPruefungRepository();

                List<OcrPruefungEintrag> ergebnisse =
                        repository.pruefeBand(bandID);

                table.setItems(
                        FXCollections.observableArrayList(ergebnisse)
                );

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
            }
        });

        btnSchliessen.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonLeiste = new HBox(
                10,
                btnPruefen,
                lblErgebnis,
                spacer,
                btnSchliessen
        );

        buttonLeiste.setAlignment(Pos.CENTER_LEFT);

        VBox kopf = new VBox(
                6,
                new Label("Ausgewählter Band:"),
                lblBand
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
    }

    private static String nullZuLeer(String wert) {
        return wert != null ? wert : "";
    }
}