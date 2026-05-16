package de.kluecki.ocr;

import de.kluecki.db.model.SeitenOCRSuchtreffer;
import de.kluecki.db.repository.SeitenOCRRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.util.function.Consumer;

import java.util.List;

/**
 * Einfacher Suchdialog für OCR-Texte eines Bandes.
 *
 * Erster Ausbaustand:
 * - Suche nur innerhalb des aktuell ausgewählten Bandes
 * - Trefferliste mit Dateiname, logischer Seite und Textausschnitt
 * - Noch kein Springen zur Seite
 */
public class OcrSucheDialog {

    public static void show(
            Stage owner,
            int bandId,
            String bandAnzeige,
            Consumer<SeitenOCRSuchtreffer> onTrefferAuswahl
    ) {
        Stage dialog = new Stage();
        dialog.setTitle("OCR-Text suchen");
        dialog.initOwner(owner);
        dialog.initModality(Modality.APPLICATION_MODAL);

        Label lblTitel = new Label("OCR-Text im aktuellen Band suchen");
        lblTitel.setStyle("""
                -fx-font-size: 15px;
                -fx-font-weight: bold;
                """);

        Label lblBand = new Label("Aktueller Band:");
        Label lblBandWert = new Label(
                bandAnzeige != null && !bandAnzeige.isBlank()
                        ? bandAnzeige
                        : "(kein Bandname verfügbar)"
        );

        TextField txtSuche = new TextField();
        txtSuche.setPromptText("Suchbegriff eingeben...");
        txtSuche.setPrefColumnCount(30);

        Button btnSuchen = new Button("Suchen");
        btnSuchen.setDisable(true);

        Label lblStatus = new Label("Noch keine Suche gestartet.");
        lblStatus.setStyle("""
                -fx-font-size: 12px;
                -fx-text-fill: #666666;
                """);

        TableView<SeitenOCRSuchtreffer> tblTreffer = new TableView<>();
        tblTreffer.setPlaceholder(new Label("Keine Treffer vorhanden"));

        TableColumn<SeitenOCRSuchtreffer, String> colDateiname = new TableColumn<>("Dateiname");
        colDateiname.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateiname() != null
                                ? cellData.getValue().getDateiname()
                                : ""
                )
        );
        colDateiname.setPrefWidth(100);

        TableColumn<SeitenOCRSuchtreffer, String> colSeite = new TableColumn<>("Logische Seite");
        colSeite.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getLogischeSeite() != null
                                ? cellData.getValue().getLogischeSeite()
                                : ""
                )
        );
        colSeite.setPrefWidth(110);

        TableColumn<SeitenOCRSuchtreffer, String> colAusschnitt = new TableColumn<>("Textausschnitt");
        colAusschnitt.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTextAusschnitt() != null
                                ? cellData.getValue().getTextAusschnitt()
                                : ""
                )
        );
        colAusschnitt.setPrefWidth(520);

        tblTreffer.getColumns().addAll(colDateiname, colSeite, colAusschnitt);
        tblTreffer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblTreffer.setPrefHeight(360);

        tblTreffer.setRowFactory(tv -> {
            TableRow<SeitenOCRSuchtreffer> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    SeitenOCRSuchtreffer treffer = row.getItem();

                    if (onTrefferAuswahl != null && treffer != null) {
                        onTrefferAuswahl.accept(treffer);
                    }

                    dialog.close();
                }
            });

            return row;
        });

        txtSuche.textProperty().addListener((obs, alt, neu) -> {
            btnSuchen.setDisable(neu == null || neu.trim().isBlank());
        });

        btnSuchen.setOnAction(e -> {
            String suchbegriff = txtSuche.getText() != null
                    ? txtSuche.getText().trim()
                    : "";

            if (suchbegriff.isBlank()) {
                return;
            }

            SeitenOCRRepository repository = new SeitenOCRRepository();

            List<SeitenOCRSuchtreffer> treffer =
                    repository.sucheOcrText(bandId, suchbegriff);

            tblTreffer.setItems(FXCollections.observableArrayList(treffer));

            if (treffer.isEmpty()) {
                lblStatus.setText("Keine Treffer für: " + suchbegriff);
            } else if (treffer.size() == 1) {
                lblStatus.setText("1 Treffer für: " + suchbegriff);
            } else {
                lblStatus.setText(treffer.size() + " Treffer für: " + suchbegriff);
            }
        });

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setOnAction(e -> dialog.close());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(lblBand, 0, 0);
        grid.add(lblBandWert, 1, 0);
        grid.add(new Label("Suchbegriff:"), 0, 1);
        grid.add(txtSuche, 1, 1);
        grid.add(btnSuchen, 2, 1);

        HBox buttons = new HBox(8, btnSchliessen);
        buttons.setPadding(new Insets(8, 0, 0, 0));

        VBox root = new VBox(12);
        root.setPadding(new Insets(14));
        root.getChildren().addAll(
                lblTitel,
                grid,
                lblStatus,
                tblTreffer,
                buttons
        );

        Scene scene = new Scene(root, 780, 520);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
}