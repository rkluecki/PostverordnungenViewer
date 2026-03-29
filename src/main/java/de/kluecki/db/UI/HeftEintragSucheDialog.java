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

    public static void show(int bandId, String gebiet, String band, List<String> gebiete, Consumer<HeftEintrag> onSelect) {
        Stage stage = new Stage();
        stage.setTitle("Suche HeftEinträge");
        stage.initModality(Modality.APPLICATION_MODAL);

        Label lblGebiet = new Label("Gebiet:");
        ComboBox<String> cmbGebiet = new ComboBox<>();
        cmbGebiet.setPrefWidth(180);
        cmbGebiet.getItems().addAll(gebiete);
        cmbGebiet.setValue(gebiet);

        Label lblTitel = new Label("Titel enthält:");
        TextField txtTitel = new TextField();
        txtTitel.setPromptText("Suchtext eingeben");

        TableColumn<HeftEintrag, String> colBand = new TableColumn<>("Band/Jahr");
        colBand.setCellValueFactory(data -> {
            HeftEintrag eintrag = data.getValue();

            String bandAnzeige = eintrag.getBandJahrAnzeige();

            if (bandAnzeige != null && !bandAnzeige.isBlank()) {
                return new SimpleStringProperty(bandAnzeige);
            }

            return new SimpleStringProperty(band != null ? band : "");
        });

        Button btnSuchen = new Button("Suchen");
        Button btnSchliessen = new Button("Schließen");
        Label lblTreffer = new Label("");

        txtTitel.setOnAction(e -> btnSuchen.fire());

        TableView<HeftEintrag> table = new TableView<>();

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

        table.getColumns().addAll(colId, colTitel, colBand, colSeiteVon, colSeiteBis);
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

                if (bandId <= 0 || (ausgewaehltesGebiet != null && !ausgewaehltesGebiet.equals(gebiet))) {
                    treffer = repo.findByTitelContainsAndGebiet(suchtext, ausgewaehltesGebiet);
                } else {
                    treffer = repo.findByTitelContainsAndBandId(suchtext, bandId);
                }

                table.getItems().addAll(treffer);

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
                10,
                lblGebiet,
                cmbGebiet,
                lblTitel,
                txtTitel,
                btnSuchen
        );

        VBox.setVgrow(table, Priority.ALWAYS);

        HBox buttonBox = new HBox(20, lblTreffer, btnSchliessen);

        VBox root = new VBox(10, suchBox, table, buttonBox);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 700, 450);
        stage.setScene(scene);
        stage.showAndWait();
    }
}