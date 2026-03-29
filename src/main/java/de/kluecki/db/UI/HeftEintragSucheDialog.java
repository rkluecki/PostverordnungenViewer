package de.kluecki.db.UI;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.HeftEintrag;
import de.kluecki.db.repository.HeftEintragRepository;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import java.sql.SQLException;

public class HeftEintragSucheDialog {
    private static String letzterSuchtext = "";

    public static void show(Stage ownerStage, int bandId, String gebiet, String band, List<String> gebiete, Consumer<HeftEintrag> onSelect)  {
        Stage stage = new Stage();
        stage.setTitle("Suche HeftEinträge");
        stage.initModality(Modality.APPLICATION_MODAL);


        if (ownerStage != null) {
            stage.initOwner(ownerStage);
        }

        Label lblGebiet = new Label("Gebiet:");
        ComboBox<String> cmbGebiet = new ComboBox<>();
        cmbGebiet.setPrefWidth(180);
        cmbGebiet.getItems().addAll(gebiete);
        cmbGebiet.setValue(gebiet);

        Label lblTitel = new Label("Titel enthält:");
        TextField txtTitel = new TextField();
        HBox.setHgrow(txtTitel, Priority.ALWAYS);
        txtTitel.setMinWidth(150);
        txtTitel.setText(letzterSuchtext);
        txtTitel.setPromptText("Suchtext eingeben");

        TableColumn<HeftEintrag, String> colGebiet = new TableColumn<>("Gebiet");
        colGebiet.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getGebietAnzeige() != null
                                ? data.getValue().getGebietAnzeige()
                                : ""
                )
        );
        colGebiet.setPrefWidth(110);

        TableColumn<HeftEintrag, String> colBand = new TableColumn<>("Band/Jahr");
        colBand.setCellValueFactory(data -> {
            HeftEintrag eintrag = data.getValue();

            String bandAnzeige = eintrag.getBandJahrAnzeige();

            if (bandAnzeige != null && !bandAnzeige.isBlank()) {
                return new SimpleStringProperty(bandAnzeige);
            }

            return new SimpleStringProperty(band != null ? band : "");
        });

        TableColumn<HeftEintrag, String> colHeft = new TableColumn<>("Heft");
        colHeft.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getHeftNummerAnzeige() != null
                                ? data.getValue().getHeftNummerAnzeige()
                                : ""
                )
        );

        colHeft.setPrefWidth(70);
        colHeft.setMaxWidth(90);

        Button btnSuchen = new Button("Suchen");
        Button btnSchliessen = new Button("Schließen");
        Label lblTreffer = new Label("");

        ToggleGroup suchModusGruppe = new ToggleGroup();

        RadioButton rbGebiet = new RadioButton("gewähltes Gebiet");
        rbGebiet.setToggleGroup(suchModusGruppe);
        rbGebiet.setSelected(true);

        RadioButton rbBand = new RadioButton("nur aktuelles Band");
        rbBand.setToggleGroup(suchModusGruppe);

        RadioButton rbAlleGebiete = new RadioButton("alle Gebiete");
        rbAlleGebiete.setToggleGroup(suchModusGruppe);

        rbBand.selectedProperty().addListener((obs, alt, neu) -> {

            if (neu) {
                cmbGebiet.setValue(gebiet);
                cmbGebiet.setDisable(true);
            } else {
                cmbGebiet.setDisable(false);
            }

        });

        VBox suchModusBox = new VBox(3, rbGebiet, rbBand, rbAlleGebiete);

        txtTitel.setOnAction(e -> btnSuchen.fire());

        TableView<HeftEintrag> table = new TableView<>();

        table.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {

            if (neu == null) {
                return;
            }

            if (onSelect != null) {
                Platform.runLater(() -> onSelect.accept(neu));
            }

        });

        TableColumn<HeftEintrag, Number> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getHeftEintragID()));

        TableColumn<HeftEintrag, String> colTitel = new TableColumn<>("Titel");
        colTitel.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getTitel() != null ? data.getValue().getTitel() : ""
                ));

        TableColumn<HeftEintrag, Number> colSeiteVon = new TableColumn<>("Seite von");
        colSeiteVon.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeiteVon()));

        TableColumn<HeftEintrag, Number> colSeiteBis = new TableColumn<>("Seite bis");
        colSeiteBis.setCellValueFactory(data ->
                new SimpleIntegerProperty(data.getValue().getSeiteBis()));

        table.getColumns().addAll(colId, colTitel, colGebiet, colBand, colHeft, colSeiteVon, colSeiteBis);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        table.setRowFactory(tv -> {
            TableRow<HeftEintrag> row = new TableRow<>();

            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    HeftEintrag ausgewaehlt = row.getItem();

                    stage.close();

                    if (onSelect != null) {
                        Platform.runLater(() -> onSelect.accept(ausgewaehlt));
                    }
                }
            });

            return row;
        });

        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {

                HeftEintrag ausgewaehlt =
                        table.getSelectionModel().getSelectedItem();

                if (ausgewaehlt != null) {

                    stage.close();

                    if (onSelect != null) {
                        Platform.runLater(() ->
                                onSelect.accept(ausgewaehlt));
                    }
                }

                event.consume();
            }
        });

        btnSuchen.setOnAction(e -> {

            String suchtext = txtTitel.getText();

            if (rbBand.isSelected() && bandId <= 0) {

                Alert alert = new Alert(Alert.AlertType.WARNING);

                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte zuerst ein Band auswählen.");

                alert.showAndWait();

                return;
            }

            letzterSuchtext = suchtext;

            if (suchtext == null || suchtext.isBlank()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte Suchtext eingeben.");
                alert.showAndWait();
                return;
            }

            try {
                HeftEintragRepository repo =
                        new HeftEintragRepository(DatabaseConnection.getConnection());

                table.getItems().clear();

                String ausgewaehltesGebiet = cmbGebiet.getValue();

                List<HeftEintrag> treffer;

                if (rbBand.isSelected()
                        && bandId > 0
                        && ausgewaehltesGebiet != null
                        && ausgewaehltesGebiet.equals(gebiet)) {

                    treffer = repo.findByTitelContainsAndBandId(suchtext, bandId);

                } else if (rbAlleGebiete.isSelected()) {

                    treffer = repo.findByTitelContains(suchtext);

                } else {

                    treffer = repo.findByTitelContainsAndGebiet(suchtext, ausgewaehltesGebiet);
                }

                treffer.sort((a, b) -> {

                    String bandA = a.getBandJahrAnzeige() != null ? a.getBandJahrAnzeige() : "";
                    String bandB = b.getBandJahrAnzeige() != null ? b.getBandJahrAnzeige() : "";

                    int bandVergleich = bandA.compareTo(bandB);

                    if (bandVergleich != 0) {
                        return bandVergleich;
                    }

                    String heftA = a.getHeftNummerAnzeige() != null ? a.getHeftNummerAnzeige() : "";
                    String heftB = b.getHeftNummerAnzeige() != null ? b.getHeftNummerAnzeige() : "";

                    int heftVergleich = heftA.compareTo(heftB);

                    if (heftVergleich != 0) {
                        return heftVergleich;
                    }

                    return Integer.compare(a.getSeiteVon(), b.getSeiteVon());
                });

                table.getItems().addAll(treffer);

                colSeiteVon.setSortType(TableColumn.SortType.ASCENDING);
                table.getSortOrder().add(colSeiteVon);

                if (treffer.isEmpty()) {
                    lblTreffer.setText("Keine Treffer");
                } else {
                    lblTreffer.setText(treffer.size() + " Treffer");
                }

                if (!treffer.isEmpty()) {
                    table.getSelectionModel().selectFirst();
                    table.scrollTo(0);
                    table.requestFocus();
                }

            } catch (SQLException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Datenbankfehler");
                alert.setHeaderText(null);
                alert.setContentText("Die Suche konnte nicht ausgeführt werden.");
                alert.showAndWait();

                ex.printStackTrace();
            }
        });

        btnSchliessen.setOnAction(e -> stage.close());

        HBox suchBox = new HBox(
                15,
                lblGebiet,
                cmbGebiet,
                lblTitel,
                txtTitel,
                suchModusBox,
                btnSuchen
        );

        suchModusBox.setPadding(new Insets(0,10,0,10));

        VBox.setVgrow(table, Priority.ALWAYS);

        HBox buttonBox = new HBox(20, lblTreffer, btnSchliessen);

        VBox root = new VBox(10, suchBox, table, buttonBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 820, 450);
        stage.setMinWidth(800);
        stage.setScene(scene);

        stage.setOnShown(event -> Platform.runLater(() -> {
            if (ownerStage != null) {
                stage.setX(ownerStage.getX() + ownerStage.getWidth() - stage.getWidth() - 20);
                stage.setY(ownerStage.getY() + 250);
            }

            stage.toFront();
        }));

        stage.showAndWait();
    }
}