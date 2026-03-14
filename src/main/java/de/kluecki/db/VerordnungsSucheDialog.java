package de.kluecki.db;

import de.kluecki.db.model.VerordnungBetreff;
import de.kluecki.db.repository.VerordnungBetreffRepository;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.function.Consumer;

public class VerordnungsSucheDialog {

    private final VerordnungBetreffRepository repository;
    private final List<String> gebiete;
    private final Consumer<VerordnungBetreff> onSelect;

    private ComboBox<String> cmbGebiet;
    private TextField txtBand;
    private TextField txtTitel;
    private ListView<VerordnungBetreff> lstErgebnis;
    private Button btnSuchen;
    private Label lblTreffer;
    private static String lastGebiet = "Alle";
    private static String lastBand = "";
    private static String lastTitel = "";

    public VerordnungsSucheDialog(VerordnungBetreffRepository repository,
                                  List<String> gebiete,
                                  Consumer<VerordnungBetreff> onSelect) {
        this.repository = repository;
        this.gebiete = gebiete;
        this.onSelect = onSelect;
    }

    public void open() {
        Stage stage = new Stage();
        stage.setTitle("Verordnungen suchen");

        stage.setAlwaysOnTop(true);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(12));

        root.setTop(createSearchPane());
        root.setCenter(createResultPane());

        Scene scene = new Scene(root, 720, 520);

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                stage.close();
            }
        });

        stage.setScene(scene);
        stage.show();

        Platform.runLater(() -> txtTitel.requestFocus());
    }

    private VBox createSearchPane() {
        Label lblTitel = new Label("Suche");
        lblTitel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(12));
        grid.setStyle("""
            -fx-background-color: #f8f8f8;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 1;
        """);

        Label lblGebiet = new Label("Gebiet:");
        cmbGebiet = new ComboBox<>();
        cmbGebiet.getItems().add("Alle");
        if (gebiete != null) {
            cmbGebiet.getItems().addAll(gebiete);
        }
        cmbGebiet.getSelectionModel().select(lastGebiet);
        cmbGebiet.setPrefWidth(220);

        Label lblBand = new Label("Band/Jahr:");
        txtBand = new TextField();
        txtBand.setText(lastBand);
        txtBand.setPrefWidth(220);
        txtBand.setPromptText("z. B. 1842 oder VA842");
        txtBand.setOnAction(e -> fuehreSucheAus());

        Label lblBetreff = new Label("Betreff:");
        txtTitel = new TextField();
        txtTitel.setText(lastTitel);
        txtTitel.setPrefWidth(320);
        txtTitel.setPromptText("z. B. Porto*, *Taxe*, Bekanntmachung");
        txtTitel.setOnAction(e -> fuehreSucheAus());

        btnSuchen = new Button("Suchen");
        btnSuchen.setPrefWidth(100);
        btnSuchen.setOnAction(e -> fuehreSucheAus());

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setOnAction(e -> schliesseFenster());

        grid.add(lblGebiet, 0, 0);
        grid.add(cmbGebiet, 1, 0);

        grid.add(lblBand, 0, 1);
        grid.add(txtBand, 1, 1);

        grid.add(lblBetreff, 0, 2);
        grid.add(txtTitel, 1, 2);

        grid.add(btnSuchen, 1, 3);
        grid.add(btnSchliessen, 2, 3);

        VBox box = new VBox(8, lblTitel, grid);
        box.setPadding(new Insets(0, 0, 12, 0));

        return box;
    }

    private VBox createResultPane() {
        Label lblErgebnisse = new Label("Ergebnisse");
        lblErgebnisse.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        lblTreffer = new Label("0 Treffer");
        lblTreffer.setStyle("-fx-text-fill: #555;");

        lstErgebnis = new ListView<>();
        lstErgebnis.setFixedCellSize(50);
        lstErgebnis.setStyle("-fx-font-size: 12px;");
        lstErgebnis.setPlaceholder(new Label("Bitte Suche starten."));

        lstErgebnis.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(VerordnungBetreff item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(
                            item.getGebiet()
                                    + "   " + item.getBandJahr()
                                    + "   Seiten " + item.getSeiteVon()
                                    + "–" + item.getSeiteBis()
                                    + "\n" + item.getTitel()
                    );
                }
            }
        });

        lstErgebnis.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                uebernehmeAuswahl();
            }
        });

        lstErgebnis.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                uebernehmeAuswahl();
                e.consume();
            }
        });

        VBox box = new VBox(5, lblErgebnisse, lblTreffer, lstErgebnis);
        VBox.setVgrow(lstErgebnis, Priority.ALWAYS);

        return box;
    }

    private void fuehreSucheAus() {
        try {
            String gebiet = cmbGebiet.getValue();
            String band = txtBand.getText();
            String titel = txtTitel.getText();
            lastGebiet = gebiet;
            lastBand = band;
            lastTitel = titel;

            lstErgebnis.getItems().clear();
            lstErgebnis.getItems().addAll(
                    repository.search(gebiet, band, titel)
            );

            int anzahl = lstErgebnis.getItems().size();
            lblTreffer.setText(anzahl + (anzahl == 1 ? " Treffer" : " Treffer"));

            if (anzahl > 0) {
                lstErgebnis.getSelectionModel().selectFirst();
                lstErgebnis.requestFocus();
            }

        } catch (Exception e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Fehler");
            alert.setHeaderText("Suche konnte nicht ausgeführt werden");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
        }
    }

    private void uebernehmeAuswahl() {
        VerordnungBetreff selected = lstErgebnis.getSelectionModel().getSelectedItem();

        if (selected == null || onSelect == null) {
            return;
        }

        schliesseFenster();
        onSelect.accept(selected);
    }

    private void schliesseFenster() {
        if (cmbGebiet != null && cmbGebiet.getScene() != null) {
            Stage stage = (Stage) cmbGebiet.getScene().getWindow();
            stage.close();
        }
    }
}