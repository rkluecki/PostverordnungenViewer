package de.kluecki.db.UI;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.OrdinataOcrKontrolle;
import de.kluecki.db.repository.OrdinataOcrKontrolleRepository;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.scene.shape.Circle;
import javafx.scene.Node;
import java.util.function.Consumer;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

import java.sql.Connection;
import java.util.List;

public class OrdinataOcrKontrolleDialog {

    private static Stage offeneStage;

    public static void show(
            Stage ownerStage,
            Consumer<OrdinataOcrKontrolle> beiDoppelklick) {

        if (offeneStage != null && offeneStage.isShowing()) {
            offeneStage.setIconified(false);
            offeneStage.toFront();
            offeneStage.requestFocus();
            return;
        }

        Stage stage = new Stage();
        offeneStage = stage;

        stage.setOnHidden(event -> offeneStage = null);

        stage.setTitle("OCR-Kontrollliste");

        Label lblTitel = new Label("OCR-Kontrollliste");
        lblTitel.setStyle("""
            -fx-font-size: 25px;
            -fx-font-weight: bold;
            -fx-text-fill: #2f5f57;
            """);

        Label lblUntertitel = new Label(
                "Übersicht über Archivquellen, OCR-Status, Import und offene Arbeitsschritte"
        );
        lblUntertitel.setStyle("""
            -fx-font-size: 13px;
            -fx-text-fill: #5f6f6b;
            """);

        VBox titelBox = new VBox(4, lblTitel, lblUntertitel);

        Label lblGesamtWert = erstelleKartenWert("0");
        Label lblOffenWert = erstelleKartenWert("0");
        Label lblErledigtWert = erstelleKartenWert("0");

        VBox karteGesamt = erstelleKennzahlenKarte(
                "Gesamt",
                lblGesamtWert
        );

        VBox karteOffen = erstelleKennzahlenKarte(
                "Offen",
                lblOffenWert
        );

        VBox karteErledigt = erstelleKennzahlenKarte(
                "Abgeschlossen",
                lblErledigtWert
        );

        HBox kartenBox = new HBox(
                12,
                karteGesamt,
                karteOffen,
                karteErledigt
        );

        kartenBox.setAlignment(Pos.CENTER_RIGHT);

        Region kopfAbstand = new Region();
        HBox.setHgrow(kopfAbstand, Priority.ALWAYS);

        HBox kopfBox = new HBox(
                20,
                titelBox,
                kopfAbstand,
                kartenBox
        );

        kopfBox.setAlignment(Pos.CENTER_LEFT);

        CheckBox chkNurOffeneEintraege =
                new CheckBox(
                        "Nur offene Einträge anzeigen"
                );

        Label lblPrioritaetsFilter =
                new Label("Priorität:");

        ComboBox<Auswahlwert> cmbPrioritaetsFilter =
                new ComboBox<>();

        cmbPrioritaetsFilter.getItems().addAll(
                new Auswahlwert(null, "Alle"),
                new Auswahlwert("SEHR_HOCH", "Sehr hoch"),
                new Auswahlwert("HOCH", "Hoch"),
                new Auswahlwert("MITTEL", "Mittel"),
                new Auswahlwert("NIEDRIG", "Niedrig")
        );

        cmbPrioritaetsFilter.getSelectionModel().selectFirst();
        cmbPrioritaetsFilter.setPrefWidth(120);

        Label lblOcrStatusFilter =
                new Label("OCR-Status:");

        ComboBox<Auswahlwert> cmbOcrStatusFilter =
                new ComboBox<>();

        cmbOcrStatusFilter.getItems().addAll(
                new Auswahlwert(null, "Alle"),
                new Auswahlwert("UNBEKANNT", "Unbekannt"),
                new Auswahlwert("OCR_FEHLT", "OCR fehlt"),
                new Auswahlwert(
                        "OCR_TEILWEISE",
                        "OCR teilweise vorhanden"
                ),
                new Auswahlwert(
                        "OCR_VOLLSTAENDIG",
                        "OCR vollständig"
                ),
                new Auswahlwert(
                        "OCR_FEHLERHAFT",
                        "OCR fehlerhaft"
                ),
                new Auswahlwert(
                        "EIGENE_OCR_NOETIG",
                        "Eigene OCR nötig"
                ),
                new Auswahlwert(
                        "MAGISTER_VORGESEHEN",
                        "Bearbeitung mit Magister vorgesehen"
                )
        );

        cmbOcrStatusFilter.getSelectionModel().selectFirst();
        cmbOcrStatusFilter.setPrefWidth(220);

        Button btnFilterZuruecksetzen =
                new Button("Filter zurücksetzen");

        btnFilterZuruecksetzen.setStyle("""
            -fx-background-color: #e6efec;
            -fx-border-color: #9bbab2;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 5 12 5 12;
            """);

        chkNurOffeneEintraege.setStyle("""
            -fx-font-size: 12px;
            -fx-text-fill: #465e58;
            """);

        HBox filterLeiste =
                new HBox(
                        14,
                        chkNurOffeneEintraege,
                        lblPrioritaetsFilter,
                        cmbPrioritaetsFilter,
                        lblOcrStatusFilter,
                        cmbOcrStatusFilter,
                        btnFilterZuruecksetzen
                );

        filterLeiste.setAlignment(Pos.CENTER_LEFT);
        filterLeiste.setPadding(
                new Insets(2, 0, 0, 0)
        );

        TableView<OrdinataOcrKontrolle> table =
                new TableView<>();

        table.setId("ocrKontrolllisteTable");

        table.setTooltip(
                new Tooltip(
                        "Doppelklick auf einen Eintrag öffnet den verknüpften Band auf Seite 1."
                )
        );

        table.setRowFactory(tableView -> {

            TableRow<OrdinataOcrKontrolle> zeile =
                    new TableRow<>();

            zeile.setOnMouseClicked(event -> {

                if (event.getClickCount() != 2
                        || zeile.isEmpty()) {
                    return;
                }

                OrdinataOcrKontrolle eintrag =
                        zeile.getItem();

                if (eintrag == null
                        || beiDoppelklick == null) {
                    return;
                }

                beiDoppelklick.accept(eintrag);
            });

            return zeile;
        });

        TableColumn<OrdinataOcrKontrolle, String> colGebiet =
                new TableColumn<>("Gebiet");

        colGebiet.setCellValueFactory(data ->
                new SimpleStringProperty(
                        textOderLeer(
                                data.getValue()
                                        .getGebietBezeichnung()
                        )
                )
        );

        colGebiet.setCellFactory(column ->
                new TableCell<>() {

                    private final Label text = new Label();
                    private final HBox inhalt =
                            new HBox(7);

                    {
                        inhalt.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setText(null);
                            setGraphic(null);
                            return;
                        }

                        text.setText(item);

                        Node wappen =
                                OrdinataGebietsWappenFactory.erstelle(item);

                        if (wappen != null) {
                            inhalt.getChildren().setAll(
                                    wappen,
                                    text
                            );
                        } else {
                            inhalt.getChildren().setAll(text);
                        }

                        setText(null);
                        setGraphic(inhalt);
                    }
                });

        colGebiet.setPrefWidth(150);

        TableColumn<OrdinataOcrKontrolle, String> colBand =
                new TableColumn<>("Band/Jahr");

        colBand.setCellValueFactory(data ->
                new SimpleStringProperty(
                        textOderLeer(
                                data.getValue()
                                        .getBandJahrAnzeige()
                        )
                )
        );

        colBand.setPrefWidth(90);

        TableColumn<OrdinataOcrKontrolle, String> colUnterband =
                new TableColumn<>("Unterband");

        colUnterband.setCellValueFactory(data ->
                new SimpleStringProperty(
                        textOderLeer(
                                data.getValue()
                                        .getUnterbandTitel()
                        )
                )
        );

        colUnterband.setPrefWidth(120);

        TableColumn<OrdinataOcrKontrolle, String> colArchiv =
                new TableColumn<>("Archiv / Quelle");

        colArchiv.setCellValueFactory(data -> {

            OrdinataOcrKontrolle eintrag = data.getValue();

            String archiv = textOderLeer(
                    eintrag.getArchivName()
            );

            String quelle = textOderLeer(
                    eintrag.getQuellenTitel()
            );

            if (!archiv.isBlank() && !quelle.isBlank()) {
                return new SimpleStringProperty(
                        archiv + " – " + quelle
                );
            }

            if (!archiv.isBlank()) {
                return new SimpleStringProperty(archiv);
            }

            return new SimpleStringProperty(quelle);
        });

        colArchiv.setPrefWidth(210);
        aktiviereTooltip(colArchiv);

        TableColumn<OrdinataOcrKontrolle, String> colQuellenStatus =
                new TableColumn<>("Quellenstatus");

        colQuellenStatus.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatiereStatus(
                                data.getValue()
                                        .getQuellenStatus()
                        )
                )
        );

        colQuellenStatus.setCellFactory(column ->
                new TableCell<>() {

                    private final Circle punkt = new Circle(5);
                    private final Label text = new Label();
                    private final HBox inhalt =
                            new HBox(7, punkt, text);

                    {
                        inhalt.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setGraphic(null);
                            setText(null);
                            return;
                        }

                        text.setText(item);

                        OrdinataOcrKontrolle eintrag =
                                getTableRow() != null
                                        ? getTableRow().getItem()
                                        : null;

                        String quellenStatus =
                                eintrag != null
                                        ? eintrag.getQuellenStatus()
                                        : null;

                        punkt.setFill(
                                switch (quellenStatus != null
                                        ? quellenStatus
                                        : "") {

                                    case "QUELLE_GEFUNDEN" ->
                                            Color.web("#4cad72");

                                    case "QUELLE_TEILWEISE" ->
                                            Color.web("#f4bd32");

                                    case "QUELLE_FEHLT" ->
                                            Color.web("#e53945");

                                    case "NUR_EINZELFUNDE" ->
                                            Color.web("#7e69b2");

                                    case "NICHT_DIGITAL" ->
                                            Color.web("#6f7f7a");

                                    default ->
                                            Color.web("#a9b4b0");
                                }
                        );

                        punkt.setStroke(
                                Color.rgb(70, 85, 80, 0.35)
                        );

                        punkt.setStrokeWidth(0.8);

                        setText(null);
                        setGraphic(inhalt);
                    }
                });

        colQuellenStatus.setPrefWidth(180);

        TableColumn<OrdinataOcrKontrolle, String> colOcrStatus =
                new TableColumn<>("OCR-Status");

        colOcrStatus.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatiereStatus(
                                data.getValue()
                                        .getOcrStatus()
                        )
                )
        );

        colOcrStatus.setCellFactory(column ->
                new TableCell<>() {

                    private final Circle punkt = new Circle(5);
                    private final Label text = new Label();
                    private final HBox inhalt =
                            new HBox(7, punkt, text);

                    {
                        inhalt.setAlignment(Pos.CENTER_LEFT);
                        inhalt.setMaxHeight(Region.USE_PREF_SIZE);

                        inhalt.setStyle("""
                    -fx-background-color: #edf5f2;
                    -fx-border-color: #c7ddd7;
                    -fx-border-radius: 5;
                    -fx-background-radius: 5;
                    -fx-padding: 2 6 2 6;
                    """);
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setGraphic(null);
                            setText(null);
                            return;
                        }

                        text.setText(item);

                        OrdinataOcrKontrolle eintrag =
                                getTableRow() != null
                                        ? getTableRow().getItem()
                                        : null;

                        String ocrStatus =
                                eintrag != null
                                        ? eintrag.getOcrStatus()
                                        : null;

                        punkt.setFill(
                                switch (ocrStatus != null
                                        ? ocrStatus
                                        : "") {

                                    case "OCR_VOLLSTAENDIG" ->
                                            Color.web("#4cad72");

                                    case "OCR_TEILWEISE" ->
                                            Color.web("#f2c300");

                                    case "OCR_FEHLT" ->
                                            Color.web("#e53945");

                                    case "OCR_FEHLERHAFT" ->
                                            Color.web("#f47b20");

                                    case "EIGENE_OCR_NOETIG" ->
                                            Color.web("#3f8edb");

                                    case "MAGISTER_VORGESEHEN" ->
                                            Color.web("#7e69b2");

                                    default ->
                                            Color.web("#a9b4b0");
                                }
                        );

                        punkt.setStroke(
                                Color.rgb(70, 85, 80, 0.35)
                        );

                        punkt.setStrokeWidth(0.8);

                        setText(null);
                        setGraphic(inhalt);
                    }
                });

        colOcrStatus.setPrefWidth(125);

        TableColumn<OrdinataOcrKontrolle, String> colImportStatus =
                new TableColumn<>("Importstatus");

        colImportStatus.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatiereStatus(
                                data.getValue()
                                        .getImportStatus()
                        )
                )
        );

        colImportStatus.setPrefWidth(145);

        TableColumn<OrdinataOcrKontrolle, String> colPruefStatus =
                new TableColumn<>("Prüfstatus");

        colPruefStatus.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatiereStatus(
                                data.getValue()
                                        .getPruefStatus()
                        )
                )
        );

        colPruefStatus.setPrefWidth(135);

        TableColumn<OrdinataOcrKontrolle, String> colErschliessbarkeit =
                new TableColumn<>("Erschließbarkeit");

        colErschliessbarkeit.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatiereStatus(
                                data.getValue()
                                        .getErschliessbarkeit()
                        )
                )
        );

        colErschliessbarkeit.setCellFactory(column ->
                new TableCell<>() {

                    private final Circle punkt = new Circle(5);
                    private final Label text = new Label();
                    private final HBox inhalt =
                            new HBox(7, punkt, text);

                    {
                        inhalt.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setGraphic(null);
                            setText(null);
                            return;
                        }

                        text.setText(item);

                        OrdinataOcrKontrolle eintrag =
                                getTableRow() != null
                                        ? getTableRow().getItem()
                                        : null;

                        String erschliessbarkeit =
                                eintrag != null
                                        ? eintrag.getErschliessbarkeit()
                                        : null;

                        punkt.setFill(
                                switch (erschliessbarkeit != null
                                        ? erschliessbarkeit
                                        : "") {

                                    case "GUT" ->
                                            Color.web("#4cad72");

                                    case "TEILWEISE" ->
                                            Color.web("#f4bd32");

                                    case "SCHWIERIG" ->
                                            Color.web("#f47b20");

                                    case "NUR_EINZELFUNDE" ->
                                            Color.web("#7e69b2");

                                    case "QUELLE_VORHANDEN_OCR_FEHLT" ->
                                            Color.web("#3f8edb");

                                    case "DERZEIT_NICHT_ERSCHLIESSBAR" ->
                                            Color.web("#e53945");

                                    default ->
                                            Color.web("#a9b4b0");
                                }
                        );

                        punkt.setStroke(
                                Color.rgb(70, 85, 80, 0.35)
                        );

                        punkt.setStrokeWidth(0.8);

                        setText(null);
                        setGraphic(inhalt);
                    }
                });

        colErschliessbarkeit.setPrefWidth(220);

        TableColumn<OrdinataOcrKontrolle, String> colPrioritaet =
                new TableColumn<>("Priorität");

        colPrioritaet.setCellValueFactory(data ->
                new SimpleStringProperty(
                        formatiereStatus(
                                data.getValue()
                                        .getPrioritaet()
                        )
                )
        );

        colPrioritaet.setCellFactory(column ->
                new TableCell<>() {

                    private final Circle punkt = new Circle(5);
                    private final Label text = new Label();
                    private final HBox inhalt =
                            new HBox(7, punkt, text);

                    {
                        inhalt.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setGraphic(null);
                            setText(null);
                            return;
                        }

                        text.setText(item);

                        OrdinataOcrKontrolle eintrag =
                                getTableRow() != null
                                        ? getTableRow().getItem()
                                        : null;

                        String prioritaet =
                                eintrag != null
                                        ? eintrag.getPrioritaet()
                                        : null;

                        punkt.setFill(
                                switch (prioritaet != null
                                        ? prioritaet
                                        : "") {

                                    case "SEHR_HOCH" ->
                                            Color.web("#e53945");

                                    case "HOCH" ->
                                            Color.web("#f47b20");

                                    case "MITTEL" ->
                                            Color.web("#f4bd32");

                                    case "NIEDRIG" ->
                                            Color.web("#4cad72");

                                    default ->
                                            Color.web("#a9b4b0");
                                }
                        );

                        punkt.setStroke(
                                Color.rgb(70, 85, 80, 0.35)
                        );

                        punkt.setStrokeWidth(0.8);

                        setText(null);
                        setGraphic(inhalt);
                    }
                });

        colPrioritaet.setPrefWidth(105);

        TableColumn<OrdinataOcrKontrolle, String> colNaechsterSchritt =
                new TableColumn<>("Nächster Schritt");

        colNaechsterSchritt.setCellValueFactory(data ->
                new SimpleStringProperty(
                        textOderLeer(
                                data.getValue()
                                        .getNaechsterSchritt()
                        )
                )
        );

        colNaechsterSchritt.setCellFactory(column ->
                new TableCell<>() {

                    private final Label symbol = new Label();
                    private final Label text = new Label();
                    private final HBox inhalt =
                            new HBox(7, symbol, text);

                    {
                        inhalt.setAlignment(Pos.CENTER_LEFT);
                    }

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setGraphic(null);
                            setText(null);
                            setTooltip(null);
                            return;
                        }

                        OrdinataOcrKontrolle eintrag =
                                getTableRow() != null
                                        ? getTableRow().getItem()
                                        : null;

                        boolean istErledigt =
                                eintrag != null
                                        && eintrag.isIstErledigt();

                        if (istErledigt) {

                            symbol.setText("✓");
                            symbol.setStyle("""
                        -fx-font-size: 15px;
                        -fx-font-weight: bold;
                        -fx-text-fill: #3f9b65;
                        """);

                        } else {

                            symbol.setText("→");
                            symbol.setStyle("""
                        -fx-font-size: 15px;
                        -fx-font-weight: bold;
                        -fx-text-fill: #4f8077;
                        """);
                        }

                        text.setText(item);

                        setText(null);
                        setGraphic(inhalt);
                        setTooltip(new Tooltip(item));
                    }
                });

        colNaechsterSchritt.setPrefWidth(210);

        TableColumn<OrdinataOcrKontrolle, String> colBemerkung =
                new TableColumn<>("Bemerkung");

        colBemerkung.setCellValueFactory(data ->
                new SimpleStringProperty(
                        textOderLeer(
                                data.getValue()
                                        .getBemerkung()
                        )
                )
        );

        colBemerkung.setPrefWidth(220);
        aktiviereTooltip(colBemerkung);

        table.getColumns().addAll(
                colGebiet,
                colBand,
                colUnterband,
                colArchiv,
                colQuellenStatus,
                colOcrStatus,
                colImportStatus,
                colPruefStatus,
                colErschliessbarkeit,
                colPrioritaet,
                colNaechsterSchritt,
                colBemerkung
        );

        table.setColumnResizePolicy(
                TableView.UNCONSTRAINED_RESIZE_POLICY
        );

        table.setPlaceholder(
                new Label(
                        "Noch keine Einträge in der OCR-Kontrollliste vorhanden."
                )
        );

        Label lblStatus = new Label(
                "OCR-Kontrollliste wird geladen …"
        );

        lblStatus.setStyle("""
            -fx-text-fill: #64736f;
            -fx-font-size: 12px;
            """);

        chkNurOffeneEintraege.setOnAction(event ->
                ladeDaten(
                        table,
                        lblGesamtWert,
                        lblOffenWert,
                        lblErledigtWert,
                        lblStatus,
                        chkNurOffeneEintraege.isSelected(),
                        cmbPrioritaetsFilter.getValue().code(),
                        cmbOcrStatusFilter.getValue().code()
                )
        );

        cmbPrioritaetsFilter.setOnAction(event ->
                ladeDaten(
                        table,
                        lblGesamtWert,
                        lblOffenWert,
                        lblErledigtWert,
                        lblStatus,
                        chkNurOffeneEintraege.isSelected(),
                        cmbPrioritaetsFilter.getValue().code(),
                        cmbOcrStatusFilter.getValue().code()
                )
        );

        cmbOcrStatusFilter.setOnAction(event ->
                ladeDaten(
                        table,
                        lblGesamtWert,
                        lblOffenWert,
                        lblErledigtWert,
                        lblStatus,
                        chkNurOffeneEintraege.isSelected(),
                        cmbPrioritaetsFilter.getValue().code(),
                        cmbOcrStatusFilter.getValue().code()
                )
        );

        btnFilterZuruecksetzen.setOnAction(event -> {

            chkNurOffeneEintraege.setSelected(false);

            cmbPrioritaetsFilter.getSelectionModel()
                    .selectFirst();

            cmbOcrStatusFilter.getSelectionModel()
                    .selectFirst();

            ladeDaten(
                    table,
                    lblGesamtWert,
                    lblOffenWert,
                    lblErledigtWert,
                    lblStatus,
                    false,
                    null,
                    null
            );
        });

        Button btnEintragAnlegen = new Button("Eintrag anlegen");
        btnEintragAnlegen.setPrefWidth(130);

        btnEintragAnlegen.setStyle("""
            -fx-background-color: #dcece7;
            -fx-border-color: #7fa99f;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 7 16 7 16;
            """);

        btnEintragAnlegen.setOnAction(event -> {

            boolean wurdeGespeichert =
                    OrdinataOcrKontrolleBearbeitenDialog
                            .zeigeZumAnlegen(stage);

            if (!wurdeGespeichert) {
                return;
            }

            ladeDaten(
                    table,
                    lblGesamtWert,
                    lblOffenWert,
                    lblErledigtWert,
                    lblStatus,
                    chkNurOffeneEintraege.isSelected(),
                    cmbPrioritaetsFilter.getValue().code(),
                    cmbOcrStatusFilter.getValue().code()
            );
        });

        Button btnEintragBearbeiten = new Button("Eintrag bearbeiten");
        btnEintragBearbeiten.setPrefWidth(145);

        btnEintragBearbeiten.setStyle("""
            -fx-background-color: #dcece7;
            -fx-border-color: #7fa99f;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 7 16 7 16;
            """);

        btnEintragBearbeiten.setOnAction(event -> {

            OrdinataOcrKontrolle ausgewaehlterEintrag =
                    table.getSelectionModel()
                            .getSelectedItem();

            if (ausgewaehlterEintrag == null) {

                Alert alert =
                        new Alert(
                                Alert.AlertType.INFORMATION
                        );

                alert.setTitle("Kein Eintrag ausgewählt");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Bitte zuerst einen Kontrolllisteneintrag auswählen."
                );

                alert.initOwner(stage);
                alert.showAndWait();
                return;
            }

            boolean wurdeGespeichert =
                    OrdinataOcrKontrolleBearbeitenDialog
                            .zeigeZumBearbeiten(
                                    stage,
                                    ausgewaehlterEintrag
                            );

            if (!wurdeGespeichert) {
                return;
            }

            ladeDaten(
                    table,
                    lblGesamtWert,
                    lblOffenWert,
                    lblErledigtWert,
                    lblStatus,
                    chkNurOffeneEintraege.isSelected(),
                    cmbPrioritaetsFilter.getValue().code(),
                    cmbOcrStatusFilter.getValue().code()
            );
        });

        Button btnEintragLoeschen = new Button("Eintrag löschen");
        btnEintragLoeschen.setPrefWidth(130);

        btnEintragLoeschen.setStyle("""
            -fx-background-color: #f3e1de;
            -fx-border-color: #c28f87;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 7 16 7 16;
            """);

        btnEintragLoeschen.setOnAction(event -> {

            OrdinataOcrKontrolle ausgewaehlterEintrag =
                    table.getSelectionModel()
                            .getSelectedItem();

            if (ausgewaehlterEintrag == null) {

                Alert alert =
                        new Alert(
                                Alert.AlertType.INFORMATION
                        );

                alert.setTitle("Kein Eintrag ausgewählt");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Bitte zuerst einen Kontrolllisteneintrag auswählen."
                );

                alert.initOwner(stage);
                alert.showAndWait();
                return;
            }

            Alert bestaetigung =
                    new Alert(
                            Alert.AlertType.CONFIRMATION
                    );

            bestaetigung.setTitle(
                    "Kontrolllisteneintrag löschen"
            );

            bestaetigung.setHeaderText(
                    "Soll der ausgewählte Kontrolllisteneintrag wirklich gelöscht werden?"
            );

            bestaetigung.setContentText(
                    textOderLeer(
                            ausgewaehlterEintrag.getGebietBezeichnung()
                    )
                            + " – "
                            + textOderLeer(
                            ausgewaehlterEintrag.getBandJahrAnzeige()
                    )
            );

            bestaetigung.initOwner(stage);

            ButtonType ergebnis =
                    bestaetigung.showAndWait()
                            .orElse(ButtonType.CANCEL);

            if (ergebnis != ButtonType.OK) {
                return;
            }

            try (Connection connection =
                         DatabaseConnection.getConnection()) {

                OrdinataOcrKontrolleRepository repository =
                        new OrdinataOcrKontrolleRepository(
                                connection
                        );

                repository.delete(
                        ausgewaehlterEintrag
                                .getOrdinataOcrKontrolleID()
                );

                ladeDaten(
                        table,
                        lblGesamtWert,
                        lblOffenWert,
                        lblErledigtWert,
                        lblStatus,
                        chkNurOffeneEintraege.isSelected(),
                        cmbPrioritaetsFilter.getValue().code(),
                        cmbOcrStatusFilter.getValue().code()
                );

            } catch (Exception ex) {

                ex.printStackTrace();

                Alert alert =
                        new Alert(
                                Alert.AlertType.ERROR
                        );

                alert.setTitle("Löschfehler");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Der OCR-Kontrolllisteneintrag konnte nicht gelöscht werden."
                );

                alert.initOwner(stage);
                alert.showAndWait();
            }
        });

        Button btnAktualisieren = new Button("Aktualisieren");
        btnAktualisieren.setPrefWidth(120);

        btnAktualisieren.setStyle("""
            -fx-background-color: #dcece7;
            -fx-border-color: #7fa99f;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 7 16 7 16;
            """);

        btnAktualisieren.setOnAction(event ->
                ladeDaten(
                        table,
                        lblGesamtWert,
                        lblOffenWert,
                        lblErledigtWert,
                        lblStatus,
                        chkNurOffeneEintraege.isSelected(),
                        cmbPrioritaetsFilter.getValue().code(),
                        cmbOcrStatusFilter.getValue().code()
                )
        );

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setPrefWidth(110);

        btnSchliessen.setStyle("""
            -fx-background-color: #e6efec;
            -fx-border-color: #9bbab2;
            -fx-border-radius: 4;
            -fx-background-radius: 4;
            -fx-padding: 7 16 7 16;
            """);

        btnSchliessen.setOnAction(event -> stage.close());

        Region untererAbstand = new Region();
        HBox.setHgrow(untererAbstand, Priority.ALWAYS);

        HBox untereLeiste = new HBox(
                10,
                lblStatus,
                untererAbstand,
                btnEintragAnlegen,
                btnEintragBearbeiten,
                btnEintragLoeschen,
                btnAktualisieren,
                btnSchliessen
        );

        untereLeiste.setAlignment(Pos.CENTER_LEFT);

        VBox.setVgrow(table, Priority.ALWAYS);

        VBox root = new VBox(
                14,
                kopfBox,
                filterLeiste,
                table,
                untereLeiste
        );

        root.setPadding(new Insets(22));

        root.setStyle("""
            -fx-background-color: #f3f0e8;
            -fx-border-color: #89afa5;
            -fx-border-width: 1;
            """);

        root.setEffect(
                new DropShadow(
                        18,
                        0,
                        0,
                        Color.rgb(0, 0, 0, 0.22)
                )
        );

        Scene scene = new Scene(root, 1550, 850);

        var cssUrl =
                OrdinataOcrKontrolleDialog.class.getResource(
                        "/css/ordinata-ocr-kontrollliste.css"
                );

        if (cssUrl != null) {
            scene.getStylesheets().add(
                    cssUrl.toExternalForm()
            );
        } else {
            System.err.println(
                    "CSS-Datei für die OCR-Kontrollliste wurde nicht gefunden."
            );
        }

        scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                stage.close();
                event.consume();
            }
        });

        stage.setMinWidth(1250);
        stage.setMinHeight(700);
        stage.setScene(scene);

        stage.sizeToScene();

        // FIX: feste Scene-Breite/-Höhe statt stage.getWidth()/getHeight() verwenden,
// da diese vor dem ersten show() noch nicht zuverlässig gesetzt sind.
        double fensterBreite = 1550;
        double fensterHoehe = 850;

        Rectangle2D sichtbarerBereich;

        if (ownerStage != null) {

            sichtbarerBereich =
                    Screen.getScreensForRectangle(
                                    ownerStage.getX(),
                                    ownerStage.getY(),
                                    ownerStage.getWidth(),
                                    ownerStage.getHeight()
                            )
                            .stream()
                            .findFirst()
                            .orElse(Screen.getPrimary())
                            .getVisualBounds();

        } else {

            sichtbarerBereich =
                    Screen.getPrimary().getVisualBounds();
        }

        double zielX =
                sichtbarerBereich.getMinX()
                        + (sichtbarerBereich.getWidth() - fensterBreite) / 2;

        double zielY =
                sichtbarerBereich.getMinY()
                        + (sichtbarerBereich.getHeight() - fensterHoehe) / 2;

        zielX = Math.max(
                sichtbarerBereich.getMinX(),
                Math.min(
                        zielX,
                        sichtbarerBereich.getMaxX() - fensterBreite
                )
        );

        zielY = Math.max(
                sichtbarerBereich.getMinY(),
                Math.min(
                        zielY,
                        sichtbarerBereich.getMaxY() - fensterHoehe
                )
        );

        stage.setX(zielX);
        stage.setY(zielY);

        ladeDaten(
                table,
                lblGesamtWert,
                lblOffenWert,
                lblErledigtWert,
                lblStatus,
                chkNurOffeneEintraege.isSelected(),
                cmbPrioritaetsFilter.getValue().code(),
                cmbOcrStatusFilter.getValue().code()
        );

        stage.show();
    }

    private static void ladeDaten(
            TableView<OrdinataOcrKontrolle> table,
            Label lblGesamtWert,
            Label lblOffenWert,
            Label lblErledigtWert,
            Label lblStatus,
            boolean nurOffeneEintraege,
            String prioritaetsCode,
            String ocrStatusCode) {

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            OrdinataOcrKontrolleRepository repository =
                    new OrdinataOcrKontrolleRepository(
                            connection
                    );

            List<OrdinataOcrKontrolle> eintraege =
                    repository.findAll();

            List<OrdinataOcrKontrolle> angezeigteEintraege =
                    eintraege.stream()
                            .filter(eintrag ->
                                    !nurOffeneEintraege
                                            || !eintrag.isIstErledigt()
                            )
                            .filter(eintrag ->
                                    prioritaetsCode == null
                                            || prioritaetsCode.equals(
                                            eintrag.getPrioritaet()
                                    )
                            )
                            .filter(eintrag ->
                                    ocrStatusCode == null
                                            || ocrStatusCode.equals(
                                            eintrag.getOcrStatus()
                                    )
                            )
                            .toList();

            table.getItems().setAll(
                    angezeigteEintraege
            );

            long erledigt = eintraege.stream()
                    .filter(
                            OrdinataOcrKontrolle::isIstErledigt
                    )
                    .count();

            long offen = eintraege.size() - erledigt;

            lblGesamtWert.setText(
                    String.valueOf(eintraege.size())
            );

            lblOffenWert.setText(
                    String.valueOf(offen)
            );

            lblErledigtWert.setText(
                    String.valueOf(erledigt)
            );

            boolean filterAktiv =
                    nurOffeneEintraege
                            || prioritaetsCode != null
                            || ocrStatusCode != null;

            if (filterAktiv) {

                lblStatus.setText(
                        angezeigteEintraege.size()
                                + " von "
                                + eintraege.size()
                                + " Kontrolllisteneinträgen angezeigt"
                );

            } else {

                lblStatus.setText(
                        eintraege.size()
                                + " Kontrolllisteneinträge geladen"
                );
            }

        } catch (Exception e) {

            lblStatus.setText(
                    "OCR-Kontrollliste konnte nicht geladen werden."
            );

            Alert alert = new Alert(
                    Alert.AlertType.ERROR
            );

            alert.setTitle("Datenbankfehler");
            alert.setHeaderText(null);
            alert.setContentText(
                    "Die OCR-Kontrollliste konnte nicht geladen werden."
            );

            alert.showAndWait();

            e.printStackTrace();
        }
    }

    private static VBox erstelleKennzahlenKarte(
            String titel,
            Label wertLabel) {

        Label titelLabel = new Label(titel);

        titelLabel.setStyle("""
            -fx-font-size: 11px;
            -fx-text-fill: #60716c;
            """);

        VBox karte = new VBox(
                3,
                titelLabel,
                wertLabel
        );

        karte.setAlignment(Pos.CENTER_LEFT);
        karte.setPrefWidth(125);
        karte.setPadding(
                new Insets(9, 13, 9, 13)
        );

        karte.setStyle("""
            -fx-background-color: #e2efeb;
            -fx-border-color: #a9c5bd;
            -fx-border-radius: 5;
            -fx-background-radius: 5;
            """);

        return karte;
    }

    private static Label erstelleKartenWert(
            String wert) {

        Label label = new Label(wert);

        label.setStyle("""
            -fx-font-size: 19px;
            -fx-font-weight: bold;
            -fx-text-fill: #315f57;
            """);

        return label;
    }

    private static String textOderLeer(
            String wert) {

        return wert != null ? wert : "";
    }

    private static String formatiereStatus(
            String status) {

        if (status == null || status.isBlank()) {
            return "";
        }

        String text = status
                .toLowerCase()
                .replace('_', ' ');

        StringBuilder ergebnis =
                new StringBuilder();

        boolean wortanfang = true;

        for (char zeichen : text.toCharArray()) {

            if (wortanfang
                    && Character.isLetter(zeichen)) {

                ergebnis.append(
                        Character.toUpperCase(zeichen)
                );

                wortanfang = false;

            } else {

                ergebnis.append(zeichen);

                wortanfang = zeichen == ' ';
            }
        }

        return ergebnis.toString();
    }

    private static void aktiviereTooltip(
            TableColumn<OrdinataOcrKontrolle, String> spalte) {

        spalte.setCellFactory(column ->
                new TableCell<>() {

                    @Override
                    protected void updateItem(
                            String item,
                            boolean empty) {

                        super.updateItem(item, empty);

                        if (empty
                                || item == null
                                || item.isBlank()) {

                            setText(null);
                            setTooltip(null);
                            return;
                        }

                        setText(item);
                        setTooltip(new Tooltip(item));
                    }
                });
    }

    private record Auswahlwert(
            String code,
            String anzeige) {

        @Override
        public String toString() {
            return anzeige;
        }
    }

}