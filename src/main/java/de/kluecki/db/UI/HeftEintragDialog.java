package de.kluecki.db.UI;

import de.kluecki.db.model.HeftEintrag;
import de.kluecki.db.repository.HeftEintragRepository;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.time.LocalDate;

public class HeftEintragDialog {

    public static boolean showDialog(
            HeftEintragRepository repository,
            int seiteVon,
            int seiteBis,
            Runnable onSaved) {

        Stage stage = new Stage();
        stage.setTitle("Neue Verordnung (HeftEintrag)");
        stage.initModality(Modality.APPLICATION_MODAL);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(15));
        grid.setHgap(10);
        grid.setVgap(10);

        TextField txtNro = new TextField();
        TextField txtTitel = new TextField();

        DatePicker dpDatum = new DatePicker();
        dpDatum.setValue(LocalDate.now());

        TextField txtVon = new TextField(String.valueOf(seiteVon));
        txtVon.setEditable(false);

        TextField txtBis = new TextField(String.valueOf(seiteBis));
        txtBis.setEditable(false);

        grid.add(new Label("Nro:"),0,0);
        grid.add(txtNro,1,0);

        grid.add(new Label("Titel:"),0,1);
        grid.add(txtTitel,1,1);

        grid.add(new Label("Datum:"),0,2);
        grid.add(dpDatum,1,2);

        grid.add(new Label("Seite von:"),0,3);
        grid.add(txtVon,1,3);

        grid.add(new Label("Seite bis:"),0,4);
        grid.add(txtBis,1,4);

        Button btnSave = new Button("Speichern");
        Button btnCancel = new Button("Abbrechen");

        btnSave.setOnAction(e -> {

            if(txtTitel.getText().trim().isEmpty()){

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setContentText("Titel fehlt");
                alert.showAndWait();

                return;
            }

            HeftEintrag eintrag = new HeftEintrag();

            eintrag.setNro(txtNro.getText());
            eintrag.setTitel(txtTitel.getText());
            eintrag.setDatum(dpDatum.getValue());
            eintrag.setSeiteVon(seiteVon);
            eintrag.setSeiteBis(seiteBis);

            try {

                repository.insert(eintrag);

            } catch (Exception ex) {

                ex.printStackTrace();

                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Fehler");
                alert.setHeaderText("HeftEintrag konnte nicht gespeichert werden");
                alert.setContentText(ex.getMessage());
                alert.showAndWait();

                return;
            }

            if(onSaved != null){
                onSaved.run();
            }

            stage.close();

        });

        btnCancel.setOnAction(e -> stage.close());

        grid.add(btnSave,0,6);
        grid.add(btnCancel,1,6);

        Scene scene = new Scene(grid,400,300);

        stage.setScene(scene);
        stage.showAndWait();

        return true;

    }

}