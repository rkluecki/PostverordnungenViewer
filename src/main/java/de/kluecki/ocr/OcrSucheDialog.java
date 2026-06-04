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
            String gebiet,
            String bandAnzeige,
            Consumer<SeitenOCRSuchtreffer> onTrefferAuswahl
    ) {
        Stage dialog = new Stage();
        dialog.setTitle("OCR-Text suchen");
        dialog.initOwner(owner);
        dialog.initModality(Modality.NONE);

        Label lblTitel = new Label("OCR-Text suchen");
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

        Label lblGebiet = new Label("Aktuelles Gebiet:");
        Label lblGebietWert = new Label(
                gebiet != null && !gebiet.isBlank()
                        ? gebiet
                        : "(kein Gebiet verfügbar)"
        );

        TextField txtSuche = new TextField();
        txtSuche.setPromptText("Suchbegriff eingeben...");
        txtSuche.setPrefColumnCount(30);

        ComboBox<String> cmbSuchart = new ComboBox<>();
        cmbSuchart.getItems().addAll(
                "enthält",
                "exakt",
                "beginnt mit",
                "endet mit",
                "Wildcard"
        );
        cmbSuchart.getSelectionModel().select("enthält");
        cmbSuchart.setPrefWidth(130);

        ComboBox<String> cmbSuchbereich = new ComboBox<>();
        cmbSuchbereich.getItems().addAll(
                "aktueller Band",
                "aktuelles Gebiet",
                "alle Gebiete"
        );
        cmbSuchbereich.getSelectionModel().select("aktueller Band");
        cmbSuchbereich.setPrefWidth(160);

        Button btnSuchen = new Button("Suchen");
        btnSuchen.setDisable(true);

        Button btnSuchhilfe = new Button("Hilfe zur Suche");
        btnSuchhilfe.setTooltip(new Tooltip("Erklärung der Sucharten und Wildcards"));

        Label lblStatus = new Label("Noch keine Suche gestartet.");
        lblStatus.setStyle("""
                -fx-font-size: 12px;
                -fx-text-fill: #666666;
                """);

        TableView<SeitenOCRSuchtreffer> tblTreffer = new TableView<>();
        tblTreffer.setPlaceholder(new Label("Keine Treffer vorhanden"));

        TableColumn<SeitenOCRSuchtreffer, String> colGebiet = new TableColumn<>("Gebiet");
        colGebiet.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getGebiet() != null
                                ? cellData.getValue().getGebiet()
                                : ""
                )
        );
        colGebiet.setPrefWidth(100);

        TableColumn<SeitenOCRSuchtreffer, String> colBand = new TableColumn<>("Band");
        colBand.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getBandAnzeige() != null
                                ? cellData.getValue().getBandAnzeige()
                                : ""
                )
        );
        colBand.setPrefWidth(130);

        TableColumn<SeitenOCRSuchtreffer, String> colDateiname = new TableColumn<>("Dateiname");
        colDateiname.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateiname() != null
                                ? cellData.getValue().getDateiname()
                                : ""
                )
        );
        colDateiname.setPrefWidth(90);

        TableColumn<SeitenOCRSuchtreffer, String> colSeite = new TableColumn<>("Logische Seite");
        colSeite.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getLogischeSeite() != null
                                ? cellData.getValue().getLogischeSeite()
                                : ""
                )
        );
        colSeite.setPrefWidth(110);

        TableColumn<SeitenOCRSuchtreffer, String> colTrefferArt = new TableColumn<>("Trefferart");
        colTrefferArt.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTrefferArt() != null
                                ? cellData.getValue().getTrefferArt()
                                : ""
                )
        );
        colTrefferArt.setPrefWidth(150);

        TableColumn<SeitenOCRSuchtreffer, String> colAusschnitt = new TableColumn<>("Textausschnitt");
        colAusschnitt.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTextAusschnitt() != null
                                ? cellData.getValue().getTextAusschnitt()
                                : ""
                )
        );
        colAusschnitt.setPrefWidth(520);

        tblTreffer.getColumns().addAll(colGebiet, colBand, colDateiname, colSeite, colTrefferArt, colAusschnitt);
        tblTreffer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblTreffer.setPrefHeight(460);

        tblTreffer.setRowFactory(tv -> {
            TableRow<SeitenOCRSuchtreffer> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    SeitenOCRSuchtreffer treffer = row.getItem();

                    if (onTrefferAuswahl != null && treffer != null) {
                        onTrefferAuswahl.accept(treffer);
                    }

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

            String suchart = cmbSuchart.getValue() != null
                    ? cmbSuchart.getValue()
                    : "enthält";

            String suchbereich = cmbSuchbereich.getValue() != null
                    ? cmbSuchbereich.getValue()
                    : "aktueller Band";

            List<SeitenOCRSuchtreffer> treffer;

            if ("aktuelles Gebiet".equals(suchbereich)) {

                treffer = repository.sucheOcrTextImGebiet(
                        gebiet,
                        suchbegriff,
                        suchart
                );

            } else if ("alle Gebiete".equals(suchbereich)) {

                treffer = repository.sucheOcrTextAlleGebiete(
                        suchbegriff,
                        suchart
                );

            }  else {

            treffer = repository.sucheOcrText(
                    bandId,
                    suchbegriff,
                    suchart
            );

            for (SeitenOCRSuchtreffer t : treffer) {
                t.setGebiet(gebiet);
                t.setBandAnzeige(bandAnzeige);
            }
        }

            tblTreffer.setItems(FXCollections.observableArrayList(treffer));

            if (treffer.isEmpty()) {
                lblStatus.setText("Keine Treffer für: " + suchbegriff);
            } else if (treffer.size() == 1) {
                lblStatus.setText("1 Treffer für: " + suchbegriff);
            } else {
                lblStatus.setText(treffer.size() + " Treffer für: " + suchbegriff);
            }
        });

        btnSuchhilfe.setOnAction(e -> zeigeSuchhilfeDialog(dialog));

        txtSuche.setOnAction(e -> {
            if (!btnSuchen.isDisabled()) {
                btnSuchen.fire();
            }
        });

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setOnAction(e -> dialog.close());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);

        grid.add(lblGebiet, 0, 0);
        grid.add(lblGebietWert, 1, 0);

        grid.add(lblBand, 0, 1);
        grid.add(lblBandWert, 1, 1);

        grid.add(new Label("Suchbereich:"), 0, 2);
        grid.add(cmbSuchbereich, 1, 2);

        grid.add(new Label("Suchart:"), 0, 3);
        grid.add(cmbSuchart, 1, 3);

        grid.add(new Label("Suchbegriff:"), 0, 4);
        grid.add(txtSuche, 1, 4);
        grid.add(btnSuchen, 2, 4);
        grid.add(btnSuchhilfe, 3, 4);

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

        Scene scene = new Scene(root, 1250, 650);
        dialog.setScene(scene);
        dialog.show();
    }

    private static void zeigeSuchhilfeDialog(Stage owner) {

        Stage hilfeDialog = new Stage();
        hilfeDialog.setTitle("Hilfe zur OCR-Suche");
        hilfeDialog.initOwner(owner);
        hilfeDialog.initModality(Modality.APPLICATION_MODAL);

        Label lblTitel = new Label("Hilfe zur OCR-Suche");
        lblTitel.setStyle("""
            -fx-font-size: 18px;
            -fx-font-weight: bold;
            -fx-text-fill: #2b2b2b;
            """);

        Label lblUntertitel = new Label("Sucharten, Beispiele und Wildcards");
        lblUntertitel.setStyle("""
            -fx-font-size: 13px;
            -fx-text-fill: #666666;
            """);

        TextArea txtHilfe = new TextArea();
        txtHilfe.setEditable(false);
        txtHilfe.setWrapText(true);

        txtHilfe.setStyle("""
            -fx-font-family: 'Consolas';
            -fx-font-size: 13px;
            -fx-control-inner-background: #fffdf7;
            """);

        txtHilfe.setText(
                """
                Die OCR-Suche durchsucht:
    
                  • Original-OCR
                  • korrigierte OCR-Fassung
    
                Wenn eine korrigierte Fassung vorhanden ist, wird sie bevorzugt
                für Trefferanzeige, Textausschnitt und Markierung verwendet.
    
    
                SUCHARTEN
                ============================================================
    
                1) enthält
                ------------------------------------------------------------
                Findet den Suchbegriff irgendwo im OCR-Text.
    
                Beispiel:
                  Suchbegriff: könig
    
                Findet z. B.:
                  könig
                  königlich
                  königlichen
                  Königreich
    
    
                2) exakt
                ------------------------------------------------------------
                Findet den Suchbegriff als eigenes Wort oder genaue Phrase.
    
                Beispiel:
                  Suchbegriff: königlich
    
                Findet:
                  königlich
    
                Findet nicht:
                  königlichen
    
                Hinweis:
                  Diese Suchart ist gut, wenn ein Begriff nicht nur als Teil
                  eines längeren Wortes gefunden werden soll.
    
    
                3) beginnt mit
                ------------------------------------------------------------
                Findet Wörter, die mit dem Suchbegriff beginnen.
    
                Beispiel:
                  Suchbegriff: könig
    
                Findet z. B.:
                  könig
                  königlich
                  königlichen
                  Königreich
    
                Findet nicht:
                  großköniglich, wenn könig nicht am Wortanfang steht.
    
    
                4) endet mit
                ------------------------------------------------------------
                Findet Wörter, die mit dem Suchbegriff enden.
    
                Beispiel:
                  Suchbegriff: lich
    
                Findet z. B.:
                  königlich
                  amtlich
                  schriftlich
    
                Findet nicht:
                  lichter, weil nach lich noch weitere Buchstaben folgen.
    
    
                5) Wildcard
                ------------------------------------------------------------
                Erlaubt Platzhalter.
    
                  %   = beliebig viele Zeichen
                  _   = genau ein Zeichen
    
                Beispiele:
    
                  könig%
                  findet z. B. königlich, königlichen, Königreich
    
                  %lich
                  findet z. B. königlich, amtlich, schriftlich
    
                  k_nig%
                  findet z. B. könig..., wenn genau ein Zeichen zwischen
                  k und nig steht
    
    
                WICHTIGE HINWEISE
                ============================================================
    
                • Bei Wildcard werden % und _ bewusst als Platzhalter verwendet.
    
                • Bei den anderen Sucharten werden % und _ wie normaler Text
                  behandelt.
    
                • Die Trefferliste zeigt, ob der Treffer im Original-OCR,
                  in der korrigierten Fassung oder in beiden Fassungen gefunden wurde.
    
                • Per Doppelklick auf einen Treffer springt Ordinata zur Seite
                  und markiert den gefundenen Begriff im OCR-Feld.
    
                • Mit den Pfeilbuttons im OCR-Bereich kann man bei mehreren
                  Treffern auf einer Seite vor- und zurückspringen.
                """
        );

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setOnAction(e -> hilfeDialog.close());

        HBox buttonLeiste = new HBox(btnSchliessen);
        buttonLeiste.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        VBox root = new VBox(8, lblTitel, lblUntertitel, txtHilfe, buttonLeiste);
        root.setPadding(new Insets(14));
        root.setStyle("""
            -fx-background-color: #f8f5ef;
            """);

        VBox.setVgrow(txtHilfe, Priority.ALWAYS);

        Scene scene = new Scene(root, 720, 620);
        hilfeDialog.setScene(scene);
        hilfeDialog.showAndWait();
    }
}