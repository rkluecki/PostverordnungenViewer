package de.kluecki.db.UI;

import de.kluecki.db.model.Veroeffentlichung;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class VeroeffentlichungWindow {

    public static Veroeffentlichung show(String gebiet, String band, int maxSeiten) {

        Stage stage = new Stage();
        stage.setTitle("Neue Veröffentlichung");
        stage.initModality(Modality.APPLICATION_MODAL);

        TextField txtTitel = new TextField();
        TextField txtNummer = new TextField();
        TextField txtSeiteVon = new TextField();
        TextField txtSeiteBis = new TextField();
        txtSeiteVon.setPrefWidth(80);
        txtSeiteBis.setPrefWidth(80);

        txtSeiteVon.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        txtSeiteBis.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(10);
        grid.setVgap(10);

        grid.add(new Label("Gebiet:"),0,0);
        grid.add(new Label(gebiet),1,0);

        grid.add(new Label("Band/Jahr:"),0,1);
        grid.add(new Label(band),1,1);

        grid.add(new Label("Titel:"),0,2);
        grid.add(txtTitel,1,2);

        grid.add(new Label("Nummer:"),0,3);
        grid.add(txtNummer,1,3);

        grid.add(new Label("Seite von:"),0,4);
        grid.add(txtSeiteVon,1,4);

        grid.add(new Label("Seite bis:"),0,5);
        grid.add(txtSeiteBis,1,5);

        Button btnSave = new Button("Speichern");
        Button btnCancel = new Button("Abbrechen");

        txtTitel.setOnAction(e -> txtNummer.requestFocus());
        txtNummer.setOnAction(e -> txtSeiteVon.requestFocus());
        txtSeiteVon.setOnAction(e -> txtSeiteBis.requestFocus());
        txtSeiteBis.setOnAction(e -> btnSave.fire());

        HBox buttons = new HBox(10, btnSave, btnCancel);

        grid.add(buttons,1,6);

        final Veroeffentlichung[] result = new Veroeffentlichung[1];

        btnSave.setOnAction(e -> {

            if (txtTitel.getText().trim().isEmpty()) {
                showAlert("Bitte Titel eingeben");
                return;
            }

            try {

                int von = Integer.parseInt(txtSeiteVon.getText().trim());
                int bis = Integer.parseInt(txtSeiteBis.getText().trim());

                if (von < 1 || bis < 1) {
                    showAlert("Seiten müssen größer 0 sein");
                    return;
                }

                if (von > maxSeiten || bis > maxSeiten) {
                    showAlert("Band hat nur " + maxSeiten + " Seiten");
                    return;
                }

                if (von > bis) {
                    showAlert("Startseite darf nicht größer als Endseite sein");
                    return;
                }

                Veroeffentlichung v = new Veroeffentlichung();

                v.setTitel(txtTitel.getText().trim());
                v.setNummer(txtNummer.getText().trim());
                v.setDatum(band);
                v.setSeiteVon(von);
                v.setSeiteBis(bis);

                result[0] = v;

                stage.close();

            } catch (Exception ex) {

                showAlert("Seiten müssen Zahlen sein");

            }

        });

        btnCancel.setOnAction(e -> stage.close());

        Scene scene = new Scene(grid,350,280);

        stage.setScene(scene);
        stage.showAndWait();

        return result[0];
    }

    private static void showAlert(String text){

        Alert alert = new Alert(Alert.AlertType.WARNING);

        alert.setTitle("Hinweis");
        alert.setHeaderText(null);
        alert.setContentText(text);

        alert.showAndWait();

    }
}