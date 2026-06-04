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

    private static final int TREFFER_PRO_SEITE = 1000;

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
        btnSuchen.setStyle("""
        -fx-background-color: #e7d7bd;
        -fx-border-color: #b99b6b;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-text-fill: #3f2f1f;
        -fx-font-weight: bold;
        -fx-padding: 5 14 5 14;
        """);

        Button btnSuchhilfe = new Button("Hilfe zur Suche");
        btnSuchhilfe.setTooltip(new Tooltip("Erklärung der Suchbereiche, Sucharten, Trefferblöcke und Wildcards"));
        btnSuchhilfe.setStyle("""
        -fx-background-color: #f3eadb;
        -fx-border-color: #c9b28e;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-text-fill: #4f3b26;
        -fx-padding: 5 12 5 12;
        """);

        final int[] aktuellerOffset = {0};
        final boolean[] offsetVorSucheZuruecksetzen = {true};

        Button btnVorherigeTreffer = new Button("← Vorherige Treffer");
        btnVorherigeTreffer.setDisable(true);
        btnVorherigeTreffer.setStyle("""
        -fx-background-color: #f3eadb;
        -fx-border-color: #c9b28e;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-text-fill: #4f3b26;
        -fx-padding: 6 12 6 12;
        """);

        Button btnWeitereTreffer = new Button("Weitere Treffer →");
        btnWeitereTreffer.setDisable(true);
        btnWeitereTreffer.setStyle("""
        -fx-background-color: #f3eadb;
        -fx-border-color: #c9b28e;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-text-fill: #4f3b26;
        -fx-padding: 6 12 6 12;
        """);

        Label lblStatus = new Label("Noch keine Suche gestartet.");
        lblStatus.setMaxWidth(Double.MAX_VALUE);
        lblStatus.setPadding(new Insets(6, 10, 6, 10));
        lblStatus.setStyle("""
        -fx-font-size: 12px;
        -fx-text-fill: #5a4632;
        -fx-background-color: #fff8e8;
        -fx-border-color: #d6c3a3;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        """);

        TableView<SeitenOCRSuchtreffer> tblTreffer = new TableView<>();

        Label lblKeineTreffer = new Label("Keine Treffer vorhanden");
        lblKeineTreffer.setStyle("""
        -fx-font-size: 13px;
        -fx-text-fill: #6b5438;
        """);

        tblTreffer.setPlaceholder(lblKeineTreffer);

        TableColumn<SeitenOCRSuchtreffer, String> colGebiet = new TableColumn<>("Gebiet");
        colGebiet.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getGebiet() != null
                                ? cellData.getValue().getGebiet()
                                : ""
                )
        );
        colGebiet.setPrefWidth(110);

        TableColumn<SeitenOCRSuchtreffer, String> colBand = new TableColumn<>("Band");
        colBand.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getBandAnzeige() != null
                                ? cellData.getValue().getBandAnzeige()
                                : ""
                )
        );
        colBand.setPrefWidth(150);

        TableColumn<SeitenOCRSuchtreffer, String> colDateiname = new TableColumn<>("Dateiname");
        colDateiname.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getDateiname() != null
                                ? cellData.getValue().getDateiname()
                                : ""
                )
        );
        colDateiname.setPrefWidth(110);

        TableColumn<SeitenOCRSuchtreffer, String> colSeite = new TableColumn<>("Seite");
        colSeite.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getLogischeSeite() != null
                                ? cellData.getValue().getLogischeSeite()
                                : ""
                )
        );
        colSeite.setPrefWidth(105);

        TableColumn<SeitenOCRSuchtreffer, String> colTrefferArt = new TableColumn<>("Trefferart");
        colTrefferArt.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTrefferArt() != null
                                ? cellData.getValue().getTrefferArt()
                                : ""
                )
        );
        colTrefferArt.setPrefWidth(140);

        TableColumn<SeitenOCRSuchtreffer, String> colAusschnitt = new TableColumn<>("Textausschnitt");
        colAusschnitt.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().getTextAusschnitt() != null
                                ? cellData.getValue().getTextAusschnitt()
                                : ""
                )
        );
        colAusschnitt.setPrefWidth(620);

        colAusschnitt.setCellFactory(column -> new TableCell<>() {

            @Override
            protected void updateItem(String text, boolean empty) {
                super.updateItem(text, empty);

                if (empty || text == null || text.isBlank()) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(text);
                    setTooltip(new Tooltip(text));
                }
            }
        });

        tblTreffer.getColumns().addAll(colGebiet, colBand, colDateiname, colSeite, colTrefferArt, colAusschnitt);
        tblTreffer.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tblTreffer.setPrefHeight(460);
        tblTreffer.setFixedCellSize(28);
        tblTreffer.setStyle("""
        -fx-font-size: 12px;
        -fx-table-cell-border-color: #e1d8c8;
        -fx-control-inner-background: #fffdf8;
        -fx-background-color: #fffdf8;
        -fx-border-color: #cfc0a8;
        -fx-border-radius: 4;
        -fx-background-radius: 4;
        """);

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

        // TODO Paginierung: Suchausführung im nächsten Schritt in Hilfsmethode auslagern.
        btnSuchen.setOnAction(e -> {
            if (offsetVorSucheZuruecksetzen[0]) {
                aktuellerOffset[0] = 0;
            }

            offsetVorSucheZuruecksetzen[0] = true;
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
                        suchart,
                        aktuellerOffset[0],
                        TREFFER_PRO_SEITE
                );

            } else if ("alle Gebiete".equals(suchbereich)) {

                treffer = repository.sucheOcrTextAlleGebiete(
                        suchbegriff,
                        suchart,
                        aktuellerOffset[0],
                        TREFFER_PRO_SEITE
                );

            }  else {

                treffer = repository.sucheOcrText(
                        bandId,
                        suchbegriff,
                        suchart,
                        aktuellerOffset[0],
                        TREFFER_PRO_SEITE
                );

            for (SeitenOCRSuchtreffer t : treffer) {
                t.setGebiet(gebiet);
                t.setBandAnzeige(bandAnzeige);
            }
        }

            tblTreffer.setItems(FXCollections.observableArrayList(treffer));

            btnVorherigeTreffer.setDisable(aktuellerOffset[0] == 0);
            btnWeitereTreffer.setDisable(treffer.size() < TREFFER_PRO_SEITE);

            String statusZusatz = " | Bereich: " + suchbereich + " | Suchart: " + suchart;

            if (treffer.isEmpty()) {
                if (aktuellerOffset[0] == 0) {
                    lblStatus.setText("Keine Treffer für: " + suchbegriff + statusZusatz);
                } else {
                    lblStatus.setText(
                            "Keine weiteren Treffer ab Treffer "
                                    + (aktuellerOffset[0] + 1)
                                    + " für: "
                                    + suchbegriff
                                    + statusZusatz
                    );
                }
            } else {
                int von = aktuellerOffset[0] + 1;
                int bis = aktuellerOffset[0] + treffer.size();

                lblStatus.setText(
                        "Treffer "
                                + von
                                + "–"
                                + bis
                                + " für: "
                                + suchbegriff
                                + statusZusatz
                );
            }
        });

        btnWeitereTreffer.setOnAction(e -> {
            aktuellerOffset[0] += TREFFER_PRO_SEITE;
            offsetVorSucheZuruecksetzen[0] = false;
            btnSuchen.fire();
        });

        btnVorherigeTreffer.setOnAction(e -> {
            aktuellerOffset[0] = Math.max(0, aktuellerOffset[0] - TREFFER_PRO_SEITE);
            offsetVorSucheZuruecksetzen[0] = false;
            btnSuchen.fire();
        });

        btnSuchhilfe.setOnAction(e -> zeigeSuchhilfeDialog(dialog));

        txtSuche.setOnAction(e -> {
            if (!btnSuchen.isDisabled()) {
                btnSuchen.fire();
            }
        });

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setStyle("""
        -fx-background-color: #eeeeee;
        -fx-border-color: #c8c8c8;
        -fx-border-radius: 5;
        -fx-background-radius: 5;
        -fx-text-fill: #333333;
        -fx-padding: 6 14 6 14;
        """);
        btnSchliessen.setOnAction(e -> dialog.close());

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        grid.setPadding(new Insets(10));
        grid.setStyle("""
        -fx-background-color: #fbf7ef;
        -fx-border-color: #d8c7aa;
        -fx-border-radius: 6;
        -fx-background-radius: 6;
        """);

        Label lblSuchbereich = new Label("Suchbereich:");
        Label lblSuchart = new Label("Suchart:");
        Label lblSuchbegriff = new Label("Suchbegriff:");

        lblSuchbereich.setStyle("-fx-font-weight: bold; -fx-text-fill: #5a4632;");
        lblSuchart.setStyle("-fx-font-weight: bold; -fx-text-fill: #5a4632;");
        lblSuchbegriff.setStyle("-fx-font-weight: bold; -fx-text-fill: #5a4632;");

        lblGebiet.setStyle("-fx-font-weight: bold; -fx-text-fill: #5a4632;");
        lblBand.setStyle("-fx-font-weight: bold; -fx-text-fill: #5a4632;");

        lblGebietWert.setStyle("-fx-text-fill: #333333;");
        lblBandWert.setStyle("-fx-text-fill: #333333;");

        grid.add(lblGebiet, 0, 0);
        grid.add(lblGebietWert, 1, 0);

        grid.add(lblBand, 0, 1);
        grid.add(lblBandWert, 1, 1);

        grid.add(lblSuchbereich, 0, 2);
        grid.add(cmbSuchbereich, 1, 2);

        grid.add(lblSuchart, 0, 3);
        grid.add(cmbSuchart, 1, 3);

        grid.add(lblSuchbegriff, 0, 4);
        grid.add(txtSuche, 1, 4);
        grid.add(btnSuchen, 2, 4);
        grid.add(btnSuchhilfe, 3, 4);

        Label lblTrefferNavigation = new Label("Trefferblöcke:");
        lblTrefferNavigation.setStyle("""
        -fx-font-size: 12px;
        -fx-text-fill: #6b5438;
        -fx-font-weight: bold;
        """);

        HBox trefferNavigation = new HBox(8, lblTrefferNavigation, btnVorherigeTreffer, btnWeitereTreffer);
        trefferNavigation.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Region buttonAbstand = new Region();
        HBox.setHgrow(buttonAbstand, Priority.ALWAYS);

        HBox buttons = new HBox(8, trefferNavigation, buttonAbstand, btnSchliessen);
        buttons.setPadding(new Insets(8, 0, 0, 0));
        buttons.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        VBox root = new VBox(12);
        root.setPadding(new Insets(14));

        Label lblTrefferHinweis = new Label("Hinweis: Doppelklick auf einen Treffer öffnet die Seite und markiert den OCR-Treffer.");
        lblTrefferHinweis.setStyle("""
        -fx-font-size: 11px;
        -fx-text-fill: #7a684f;
        """);

        root.getChildren().addAll(
                lblTitel,
                grid,
                lblStatus,
                lblTrefferHinweis,
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

        Label lblUntertitel = new Label("Suchbereiche, Sucharten, Trefferblöcke und Wildcards");
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
        
        
                SUCHBEREICHE
                ============================================================
        
                1) aktueller Band
                ------------------------------------------------------------
                Sucht nur im gerade geöffneten Band/Jahr.
        
                Diese Suche ist sinnvoll, wenn man gezielt im aktuell ausgewählten
                Band kontrollieren möchte.
        
        
                2) aktuelles Gebiet
                ------------------------------------------------------------
                Sucht in allen Bänden/Jahren des aktuell ausgewählten Gebietes.
        
                Beispiel:
                  Wenn als Gebiet Baden gewählt ist, wird über die Baden-Bände
                  gesucht.
        
        
                3) alle Gebiete
                ------------------------------------------------------------
                Sucht über alle erfassten Gebiete und Bände hinweg.
        
                Diese Suche ist besonders nützlich, wenn man nicht weiß, in welchem
                Gebiet oder Jahr ein Begriff vorkommt.
        
        
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
        
        
                TREFFERBLÖCKE / PAGINIERUNG
                ============================================================
        
                Die OCR-Suche lädt Treffer blockweise.
        
                Standardmäßig werden bis zu 1000 Treffer angezeigt.
        
                Gibt es weitere Treffer, wird der Button aktiv:
        
                  Weitere Treffer →
        
                Damit wird der nächste Trefferblock geladen, zum Beispiel:
        
                  Treffer 1–1000 für: könig
                  Treffer 1001–2000 für: könig
                  Treffer 2001–3000 für: könig
        
                Mit dem Button
        
                  ← Vorherige Treffer
        
                kann man wieder zum vorherigen Trefferblock zurückblättern.
        
                Wenn weniger als 1000 Treffer im aktuellen Block gefunden werden,
                gibt es keinen weiteren Trefferblock und der Button „Weitere Treffer →“
                bleibt deaktiviert.
        
        
                STATUSANZEIGE
                ============================================================
        
                Die Statuszeile oberhalb der Trefferliste zeigt an, welcher Bereich
                gerade angezeigt wird.
        
                Beispiele:
        
                  Noch keine Suche gestartet.
        
                  Treffer 1–555 für: könig
        
                  Treffer 1–1000 für: könig
        
                  Treffer 1001–1800 für: könig
        
                  Keine Treffer für: könig
        
                  Keine weiteren Treffer ab Treffer 1001 für: könig
        
        
                WICHTIGE HINWEISE
                ============================================================
        
                • Bei Wildcard werden % und _ bewusst als Platzhalter verwendet.
        
                • Bei den anderen Sucharten werden % und _ wie normaler Text
                  behandelt.
        
                • Die Trefferliste zeigt, ob der Treffer im Original-OCR,
                  in der korrigierten Fassung oder in beiden Fassungen gefunden wurde.
        
                • Per Doppelklick auf einen Treffer springt Ordinata zur Seite
                  und markiert den gefundenen Begriff im OCR-Feld.
        
                • Bei Treffern aus anderen Gebieten oder Bänden wechselt Ordinata
                  automatisch in den passenden Kontext.
        
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

        Scene scene = new Scene(root, 760, 680);
        hilfeDialog.setScene(scene);
        hilfeDialog.showAndWait();
    }
}