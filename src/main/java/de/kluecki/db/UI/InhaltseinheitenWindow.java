package de.kluecki.db.UI;

import de.kluecki.db.InhaltTabellenEintrag;
import de.kluecki.db.InhaltTypen;
import de.kluecki.db.model.HeftEintrag;
import de.kluecki.db.repository.InhaltseinheitRepository;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class InhaltseinheitenWindow {

    private static TextField txtNr;
    private static TextField txtTitel;
    private static TextField txtSeiteVon;
    private static TextField txtSeiteBis;
    private static ComboBox<String> cmbTyp;
    private static TextArea txtBeschreibung;

    public static void open(HeftEintrag heftEintrag) {

        txtNr = new TextField();
        txtNr.setEditable(false);
        txtNr.setFocusTraversable(false);
        txtTitel = new TextField();
        txtSeiteVon = new TextField();
        txtSeiteBis = new TextField();

        cmbTyp = new ComboBox<>();
        cmbTyp.getItems().addAll(InhaltTypen.getStandardTypen());
        cmbTyp.setEditable(false);

        txtSeiteVon.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtSeiteVon.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        txtSeiteBis.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*")) {
                txtSeiteBis.setText(newVal.replaceAll("[^\\d]", ""));
            }
        });

        txtBeschreibung = new TextArea();

        txtNr.setOnAction(e -> txtTitel.requestFocus());
        txtTitel.setOnAction(e -> txtSeiteVon.requestFocus());
        txtSeiteVon.setOnAction(e -> txtSeiteBis.requestFocus());

        txtSeiteBis.focusedProperty().addListener((obs, alt, neu) -> {
            if (neu) {
                return;
            }

            String seiteVonText = txtSeiteVon.getText().trim();
            String seiteBisText = txtSeiteBis.getText().trim();

            if (seiteVonText.isEmpty() || seiteBisText.isEmpty()) {
                return;
            }

            int von = Integer.parseInt(seiteVonText);
            int bis = Integer.parseInt(seiteBisText);

            if (bis < von) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Seite bis darf nicht kleiner als Seite von sein.");
                alert.showAndWait();

                txtSeiteBis.clear();
                Platform.runLater(() -> txtSeiteBis.requestFocus());
            }
        });

        Stage stage = new Stage();
        stage.setTitle("Inhalt");
        stage.initModality(Modality.APPLICATION_MODAL);

        Label lblKopf = new Label("HeftEintrag");
        lblKopf.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");

        Label lblBetreff = new Label(heftEintrag.getTitel());

        String seitenText;
        if (heftEintrag.getSeiteBis() == null || heftEintrag.getSeiteBis().equals(heftEintrag.getSeiteVon())) {
            seitenText = "Seiten: " + heftEintrag.getSeiteVon();
        } else {
            seitenText = "Seiten: " + heftEintrag.getSeiteVon() + " - " + heftEintrag.getSeiteBis();
        }

        Label lblSeiten = new Label(seitenText);

        VBox topBox = new VBox(6, lblKopf, lblBetreff, lblSeiten);
        topBox.setPadding(new Insets(10));

        Label lblListe = new Label("Inhalt");
        lblListe.setStyle("-fx-font-weight: bold;");

        TableView<InhaltTabellenEintrag> table = new TableView<>();
        ObservableList<InhaltTabellenEintrag> daten = FXCollections.observableArrayList();
        InhaltseinheitRepository repository = new InhaltseinheitRepository();

        daten.clear();
        daten.addAll(repository.findByHeftEintragId(heftEintrag.getHeftEintragID()));

        TableColumn<InhaltTabellenEintrag, String> colNr = new TableColumn<>("Nr.");
        TableColumn<InhaltTabellenEintrag, String> colTitel = new TableColumn<>("Titel");
        TableColumn<InhaltTabellenEintrag, String> colSeiten = new TableColumn<>("Seite");

        colNr.setPrefWidth(45);
        colTitel.setPrefWidth(220);
        colSeiten.setPrefWidth(80);

        colTitel.setMinWidth(200);
        colTitel.setMaxWidth(Double.MAX_VALUE);
        colTitel.setResizable(true);

        colTitel.prefWidthProperty().bind(
                table.widthProperty()
                        .subtract(colNr.widthProperty())
                        .subtract(colSeiten.widthProperty())
                        .subtract(20)
        );

        colNr.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNr()));

        colTitel.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getTitel()));

        colSeiten.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getSeite()));

        table.getColumns().addAll(colNr, colTitel, colSeiten);

        table.setRowFactory(tv -> {
            TableRow<InhaltTabellenEintrag> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    InhaltTabellenEintrag eintrag = row.getItem();

                    txtNr.setText(eintrag.getNr());
                    txtTitel.setText(eintrag.getTitel());

                    if (eintrag.getSeite().contains("-")) {
                        String[] teile = eintrag.getSeite().split("-");
                        txtSeiteVon.setText(teile[0].trim());
                        txtSeiteBis.setText(teile[1].trim());
                    } else {
                        txtSeiteVon.setText(eintrag.getSeite());
                        txtSeiteBis.clear();
                    }

                    cmbTyp.setValue(eintrag.getTyp());
                    txtBeschreibung.setText(eintrag.getBeschreibung());

                    txtTitel.requestFocus();
                }
            });

            return row;
        });

        table.setItems(daten);

        txtNr.setText(ermittleNaechsteNr(table));

        table.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {
            if (neu == null) {
                return;
            }

            txtNr.setText(neu.getNr());
            txtTitel.setText(neu.getTitel());
            cmbTyp.setValue(neu.getTyp());
            txtBeschreibung.setText(neu.getBeschreibung());

            if (neu.getSeite().contains("-")) {
                String[] teile = neu.getSeite().split("-");
                txtSeiteVon.setText(teile[0].trim());
                txtSeiteBis.setText(teile[1].trim());
            } else {
                txtSeiteVon.setText(neu.getSeite());
                txtSeiteBis.clear();
            }
        });

        VBox leftBox = new VBox(8, lblListe, table);
        leftBox.setPadding(new Insets(10));
        VBox.setVgrow(table, Priority.ALWAYS);

        Label lblBearbeitungMitte = new Label("Details");
        lblBearbeitungMitte.setStyle("-fx-font-weight: bold;");

        Label lblNr = new Label("Nr:");
        Label lblTitelFeld = new Label("Titel:");
        Label lblSeiteVon = new Label("Seite von:");
        Label lblSeiteBis = new Label("Seite bis:");
        Label lblTyp = new Label("Typ:");
        Label lblBeschreibung = new Label("Beschreibung:");

        txtBeschreibung.setPrefHeight(120);

        Button btnOCR = new Button("OCR Text");
        Button btnNeu = new Button("Neu");
        Button btnSpeichern = new Button("Speichern");
        Button btnLoeschen = new Button("Löschen");

        btnNeu.setOnAction(e -> {

            table.getSelectionModel().clearSelection();

            txtNr.setText(ermittleNaechsteNr(table));

            txtTitel.clear();
            txtSeiteVon.clear();
            txtSeiteBis.clear();

            cmbTyp.getSelectionModel().clearSelection();

            txtBeschreibung.clear();

            Platform.runLater(() -> txtTitel.requestFocus());
        });

        btnSpeichern.setOnAction(e -> {

            String nr = txtNr.getText().trim();
            String titel = txtTitel.getText().trim();
            String seiteVon = txtSeiteVon.getText().trim();
            String seiteBis = txtSeiteBis.getText().trim();

            if (nr.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte eine Nummer eingeben.");
                alert.showAndWait();
                return;
            }

            if (!nr.matches("\\d+")) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Die Nummer muss numerisch sein.");
                alert.showAndWait();
                return;
            }

            if (!seiteBis.isEmpty()) {
                int von = Integer.parseInt(seiteVon);
                int bis = Integer.parseInt(seiteBis);

                if (bis < von) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Hinweis");
                    alert.setHeaderText(null);
                    alert.setContentText("Seite bis darf nicht kleiner als Seite von sein.");
                    alert.showAndWait();

                    txtSeiteBis.requestFocus();
                    return;
                }
            }

            String typ = cmbTyp.getValue() == null ? "" : cmbTyp.getValue().trim();
            String beschreibung = txtBeschreibung.getText().trim();

            if (titel.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte einen Titel eingeben.");
                alert.showAndWait();
                return;
            }

            if (typ.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte einen Typ auswählen.");
                alert.showAndWait();
                cmbTyp.requestFocus();
                return;
            }

            if (seiteVon.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte Seite von eingeben.");
                alert.showAndWait();
                return;
            }

            int lfdNr = Integer.parseInt(nr);
            int inhaltstypId = InhaltTypen.getTypId(typ);
            int seiteVonInt = Integer.parseInt(seiteVon);

            Integer seiteBisInt = null;
            if (!seiteBis.isEmpty()) {
                seiteBisInt = Integer.parseInt(seiteBis);
            }

            int heftVon = heftEintrag.getSeiteVon();
            int heftBis = heftEintrag.getSeiteBis();

            if (seiteVonInt < heftVon) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Seite liegt vor dem HeftEintrag Bereich.\n" +
                                "Erlaubt: " + heftVon + " - " + heftBis
                );

                alert.showAndWait();
                return;
            }

            if (seiteBisInt != null && seiteBisInt > heftBis) {

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText(
                        "Seite liegt hinter dem HeftEintrag Bereich.\n" +
                                "Erlaubt: " + heftVon + " - " + heftBis
                );

                alert.showAndWait();
                return;
            }

            String seite = seiteVon;
            if (!seiteBis.isEmpty()) {
                if (seiteVon.equals(seiteBis)) {
                    seite = seiteVon;
                } else {
                    seite = seiteVon + "-" + seiteBis;
                }
            }

            InhaltTabellenEintrag aktuellBearbeitet =
                    table.getSelectionModel().getSelectedItem();

            for (InhaltTabellenEintrag eintrag : table.getItems()) {

                if (eintrag == aktuellBearbeitet) {
                    continue;
                }

                String vorhandeneSeite = eintrag.getSeite();

                int vorhandenVon;
                int vorhandenBis;

                if (vorhandeneSeite.contains("-")) {

                    String[] teile = vorhandeneSeite.split("-");

                    vorhandenVon = Integer.parseInt(teile[0].trim());
                    vorhandenBis = Integer.parseInt(teile[1].trim());

                } else {

                    vorhandenVon = Integer.parseInt(vorhandeneSeite);
                    vorhandenBis = vorhandenVon;
                }

                int neuVon = seiteVonInt;
                int neuBis = seiteBisInt != null ? seiteBisInt : seiteVonInt;

                boolean ueberschneidung =
                        neuVon <= vorhandenBis &&
                                neuBis >= vorhandenVon;

                if (ueberschneidung) {

                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Hinweis");
                    alert.setHeaderText(null);
                    alert.setContentText(
                            "Überschneidung mit Inhalt Nr. "
                                    + eintrag.getNr()
                                    + " (" + eintrag.getSeite() + ")"
                    );

                    alert.showAndWait();

                    return;
                }
            }

            for (InhaltTabellenEintrag eintrag : table.getItems()) {
                if (eintrag == aktuellBearbeitet) {
                    continue;
                }

                if (eintrag.getNr().equals(nr)) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setTitle("Hinweis");
                    alert.setHeaderText(null);
                    alert.setContentText("Diese Nummer existiert bereits.");
                    alert.showAndWait();
                    return;
                }
            }

            if (aktuellBearbeitet == null) {
                repository.insert(
                        heftEintrag.getHeftEintragID(),
                        lfdNr,
                        titel,
                        inhaltstypId,
                        seiteVonInt,
                        seiteBisInt,
                        beschreibung
                );
            }

            if (aktuellBearbeitet == null) {
                InhaltTabellenEintrag neuerEintrag =
                        new InhaltTabellenEintrag(nr, titel, seite, typ, beschreibung);
                daten.add(neuerEintrag);
                table.getSelectionModel().select(neuerEintrag);

                txtNr.setText(ermittleNaechsteNr(table));
            } else{

                repository.update(
                        heftEintrag.getHeftEintragID(),
                        lfdNr,
                        titel,
                        inhaltstypId,
                        seiteVonInt,
                        seiteBisInt,
                        beschreibung
                );

                aktuellBearbeitet.setNr(nr);
                aktuellBearbeitet.setTitel(titel);
                aktuellBearbeitet.setSeite(seite);
                aktuellBearbeitet.setTyp(typ);
                aktuellBearbeitet.setBeschreibung(beschreibung);

                table.refresh();
            };
        });

        btnLoeschen.setOnAction(e -> {
            InhaltTabellenEintrag ausgewaehlt =
                    table.getSelectionModel().getSelectedItem();

            if (ausgewaehlt == null) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte zuerst einen Eintrag auswählen.");
                alert.showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Löschen");
            confirm.setHeaderText(null);
            confirm.setContentText("Eintrag wirklich löschen?");

            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

                int lfdNr = Integer.parseInt(ausgewaehlt.getNr());

                repository.deleteByHeftEintragIdUndLfdNr(
                        heftEintrag.getHeftEintragID(),
                        lfdNr
                );

                daten.remove(ausgewaehlt);
                table.getSelectionModel().clearSelection();

                txtNr.setText(ermittleNaechsteNr(table));
                txtTitel.clear();
                txtSeiteVon.clear();
                txtSeiteBis.clear();
                cmbTyp.setValue(null);
                txtBeschreibung.clear();
            }
        });

        HBox buttonBox = new HBox(10, btnNeu, btnSpeichern, btnLoeschen);

        VBox rightBox = new VBox(
                6,
                lblBearbeitungMitte,
                lblNr, txtNr,
                lblTitelFeld, txtTitel,
                lblSeiteVon, txtSeiteVon,
                lblSeiteBis, txtSeiteBis,
                lblTyp, cmbTyp,
                lblBeschreibung, txtBeschreibung,
                btnOCR,
                buttonBox
        );

        rightBox.setPadding(new Insets(10));

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(leftBox, rightBox);
        splitPane.setDividerPositions(0.5);

        BorderPane root = new BorderPane();
        root.setTop(topBox);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 900, 650);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        stage.setScene(scene);
        stage.show();
    }

    private static String ermittleNaechsteNr(TableView<InhaltTabellenEintrag> table) {
        int maxNr = 0;

        for (InhaltTabellenEintrag eintrag : table.getItems()) {
            String nrText = eintrag.getNr();

            if (nrText != null && nrText.matches("\\d+")) {
                int nr = Integer.parseInt(nrText);
                if (nr > maxNr) {
                    maxNr = nr;
                }
            }
        }

        return String.valueOf(maxNr + 1);
    }
}