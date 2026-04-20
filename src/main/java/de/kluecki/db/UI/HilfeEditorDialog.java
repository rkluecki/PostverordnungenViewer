package de.kluecki.db.UI;

import de.kluecki.db.model.HilfeHinweis;
import de.kluecki.db.model.HilfeSchritt;
import de.kluecki.db.model.HilfeThema;
import de.kluecki.db.repository.HilfeRepository;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class HilfeEditorDialog {

    public static void show(Stage owner) {
        HilfeRepository repo = new HilfeRepository();

        Stage stage = new Stage();
        stage.setTitle("Hilfe-Editor");
        stage.initOwner(owner);
        stage.initModality(Modality.WINDOW_MODAL);

        ListView<HilfeThema> lstThemen = new ListView<>();
        ListView<HilfeSchritt> lstSchritte = new ListView<>();
        ListView<HilfeHinweis> lstHinweise = new ListView<>();

        lstHinweise.setOnMouseClicked(e -> {

            if (e.getClickCount() == 2) {

                HilfeHinweis hinweis =
                        lstHinweise.getSelectionModel().getSelectedItem();

                if (hinweis == null) return;

                TextInputDialog dialog = new TextInputDialog(hinweis.getText());
                dialog.setTitle("Hinweis bearbeiten");
                dialog.setHeaderText("Hinweis ändern");
                dialog.setContentText("Text:");

                dialog.showAndWait().ifPresent(neuText -> {

                    String clean = neuText.trim();
                    if (clean.isEmpty()) return;

                    hinweis.setText(clean);

                    repo.updateHinweis(hinweis);

                    HilfeSchritt schritt =
                            lstSchritte.getSelectionModel().getSelectedItem();

                    if (schritt != null) {
                        List<HilfeHinweis> neu =
                                repo.findHinweiseBySchrittId(schritt.getHilfeSchrittID());

                        lstHinweise.getItems().setAll(neu);
                    }
                });
            }
        });

        lstHinweise.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(HilfeHinweis item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTyp() + ": " + item.getText());
                }
            }
        });

        lstSchritte.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(HilfeSchritt item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("Schritt " + item.getReihenfolge() + ": " + item.getText());
                }
            }
        });

        lstSchritte.setOnMouseClicked(e -> {

            if (e.getClickCount() == 2) {

                HilfeSchritt schritt =
                        lstSchritte.getSelectionModel().getSelectedItem();

                if (schritt == null) {
                    return;
                }

                bearbeiteSchritt(repo, lstThemen, lstSchritte, schritt);
            }
        });

        Button btnThemaNeu = new Button("Thema neu");
        Button btnSchrittNeu = new Button("Schritt neu");
        Button btnSchrittLoeschen = new Button("Schritt löschen");
        Button btnSchliessen = new Button("Schließen");
        Button btnThemaLoeschen = new Button("Thema löschen");
        Button btnSchrittHoch = new Button("↑");
        Button btnSchrittRunter = new Button("↓");
        Button btnHinweisNeu = new Button("Hinweis neu");
        Button btnHinweisLoeschen = new Button("Hinweis löschen");

        btnHinweisLoeschen.setDisable(true);
        btnSchrittHoch.setDisable(true);
        btnSchrittRunter.setDisable(true);
        btnThemaLoeschen.setDisable(true);
        btnSchrittNeu.setDisable(true);
        btnSchrittLoeschen.setDisable(true);
        btnHinweisNeu.setDisable(true);

        lstThemen.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(HilfeThema item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getTitel());
            }
        });

        List<HilfeThema> themen = repo.findAlleThemen();
        lstThemen.setItems(FXCollections.observableArrayList(themen));

        lstThemen.getSelectionModel().selectedItemProperty().addListener((obs, alt, thema) -> {

            btnSchrittHoch.setDisable(thema == null);
            btnSchrittRunter.setDisable(thema == null);
            btnThemaLoeschen.setDisable(thema == null);
            lstSchritte.getItems().clear();

            boolean aktiv = thema != null;

            btnSchrittNeu.setDisable(!aktiv);
            btnSchrittLoeschen.setDisable(!aktiv);

            if (thema == null) {
                return;
            }

            List<HilfeSchritt> schritte =
                    repo.findSchritteZuThema(thema.getHilfeThemaID());

            lstSchritte.getItems().setAll(schritte);
        });

        lstSchritte.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {
            boolean aktiv = neu != null;

            btnSchrittHoch.setDisable(!aktiv);
            btnSchrittRunter.setDisable(!aktiv);
            btnHinweisNeu.setDisable(neu == null);

            lstHinweise.getItems().clear();

            if (neu != null) {
                List<HilfeHinweis> hinweise =
                        repo.findHinweiseBySchrittId(neu.getHilfeSchrittID());

                lstHinweise.getItems().setAll(hinweise);
            }
        });

        lstHinweise.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {
            btnHinweisLoeschen.setDisable(neu == null);
        });

        BorderPane links = new BorderPane();
        links.setTop(new Label("Themen"));
        links.setCenter(lstThemen);
        BorderPane.setMargin(lstThemen, new Insets(5, 0, 0, 0));

        Label lblRechts = new Label("Schritte / Hinweise");

        SplitPane rechtsSplit = new SplitPane();
        rechtsSplit.setOrientation(javafx.geometry.Orientation.VERTICAL);
        rechtsSplit.getItems().addAll(lstSchritte, lstHinweise);
        rechtsSplit.setDividerPositions(0.6);

        BorderPane rechts = new BorderPane();
        rechts.setTop(lblRechts);
        rechts.setCenter(rechtsSplit);
        BorderPane.setMargin(rechtsSplit, new Insets(5, 0, 0, 0));

        SplitPane splitPane = new SplitPane(links, rechts);
        splitPane.setDividerPositions(0.35);

        btnThemaNeu.setOnAction(e -> {

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Neues Thema");
            dialog.setHeaderText("Neues Hilfethema anlegen");
            dialog.setContentText("Titel:");

            dialog.showAndWait().ifPresent(titel -> {

                String cleanTitel = titel.trim();

                if (cleanTitel.isEmpty()) {
                    return;
                }

                HilfeThema neuesThema = new HilfeThema();
                neuesThema.setTitel(cleanTitel);
                neuesThema.setSortierung(999); // erstmal ans Ende
                neuesThema.setIstAktiv(true);

                repo.insertThema(neuesThema);

                // Liste neu laden
                List<HilfeThema> themenNeu = repo.findAlleThemen();
                lstThemen.setItems(FXCollections.observableArrayList(themenNeu));

                // neu angelegtes Thema auswählen
                for (HilfeThema t : themenNeu) {
                    if (t.getTitel().equals(cleanTitel)) {
                        lstThemen.getSelectionModel().select(t);
                        lstThemen.scrollTo(t);
                        break;
                    }
                }
            });
        });

        btnSchrittNeu.setOnAction(e -> {

            HilfeThema thema = lstThemen.getSelectionModel().getSelectedItem();

            if (thema == null) {
                return;
            }

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Neuer Schritt");
            dialog.setHeaderText("Neuen Schritt anlegen");
            dialog.setContentText("Schritttext:");

            dialog.showAndWait().ifPresent(text -> {

                String cleanText = text.trim();

                if (cleanText.isEmpty()) {
                    return;
                }

                List<HilfeSchritt> vorhandeneSchritte =
                        repo.findSchritteZuThema(thema.getHilfeThemaID());

                HilfeSchritt neuerSchritt = new HilfeSchritt();
                neuerSchritt.setHilfeThemaID(thema.getHilfeThemaID());
                neuerSchritt.setReihenfolge(vorhandeneSchritte.size() + 1);
                neuerSchritt.setText(cleanText);
                neuerSchritt.setIstAktiv(true);

                repo.insertSchritt(neuerSchritt);

                List<HilfeSchritt> neu =
                        repo.findSchritteZuThema(thema.getHilfeThemaID());

                lstSchritte.getItems().setAll(neu);
            });
        });

        btnSchrittLoeschen.setOnAction(e -> {

            HilfeSchritt schritt =
                    lstSchritte.getSelectionModel().getSelectedItem();

            if (schritt == null) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Hinweis");
                alert.setHeaderText(null);
                alert.setContentText("Bitte zuerst einen Schritt auswählen.");
                alert.showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Schritt löschen");
            confirm.setHeaderText(null);
            confirm.setContentText("Diesen Schritt wirklich löschen?");

            confirm.showAndWait().ifPresent(result -> {

                if (result == ButtonType.OK) {

                    repo.deleteSchrittById(schritt.getHilfeSchrittID());

                    HilfeThema thema =
                            lstThemen.getSelectionModel().getSelectedItem();

                    if (thema != null) {
                        List<HilfeSchritt> neu =
                                repo.findSchritteZuThema(thema.getHilfeThemaID());

                        lstSchritte.getItems().setAll(neu);
                    }
                }
            });
        });

        btnThemaLoeschen.setOnAction(e -> {

            HilfeThema thema = lstThemen.getSelectionModel().getSelectedItem();

            if (thema == null) {
                return;
            }

            List<HilfeSchritt> schritte =
                    repo.findSchritteZuThema(thema.getHilfeThemaID());

            if (!schritte.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Nicht möglich");
                alert.setHeaderText(null);
                alert.setContentText("Thema kann nicht gelöscht werden, da noch Schritte vorhanden sind.");
                alert.showAndWait();
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Thema löschen");
            confirm.setHeaderText(null);
            confirm.setContentText("Dieses Thema wirklich löschen?");

            confirm.showAndWait().ifPresent(result -> {

                if (result == ButtonType.OK) {

                    repo.deleteThemaById(thema.getHilfeThemaID());

                    List<HilfeThema> neu = repo.findAlleThemen();
                    lstThemen.setItems(FXCollections.observableArrayList(neu));

                    lstSchritte.getItems().clear();
                }
            });
        });

        btnSchliessen.setOnAction(e -> stage.close());

        btnHinweisNeu.setOnAction(e -> {

            HilfeSchritt schritt =
                    lstSchritte.getSelectionModel().getSelectedItem();

            if (schritt == null) {
                return;
            }

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Neuer Hinweis");
            dialog.setHeaderText("Hinweis erfassen");
            dialog.setContentText("Text:");

            dialog.showAndWait().ifPresent(text -> {

                String clean = text.trim();

                if (clean.isEmpty()) {
                    return;
                }

                HilfeHinweis hinweis = new HilfeHinweis();
                hinweis.setHilfeSchrittID(schritt.getHilfeSchrittID());
                hinweis.setTyp("INFO");
                hinweis.setText(clean);
                hinweis.setIstAktiv(true);

                repo.insertHinweis(hinweis);

                List<HilfeHinweis> neu =
                        repo.findHinweiseBySchrittId(schritt.getHilfeSchrittID());

                lstHinweise.getItems().setAll(neu);
            });
        });

        btnHinweisLoeschen.setOnAction(e -> {

            HilfeHinweis hinweis =
                    lstHinweise.getSelectionModel().getSelectedItem();

            if (hinweis == null) return;

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Hinweis löschen");
            confirm.setHeaderText(null);
            confirm.setContentText("Diesen Hinweis wirklich löschen?");

            confirm.showAndWait().ifPresent(result -> {

                if (result == ButtonType.OK) {

                    repo.deleteHinweisById(hinweis.getHilfeHinweisID());

                    HilfeSchritt schritt =
                            lstSchritte.getSelectionModel().getSelectedItem();

                    if (schritt != null) {
                        List<HilfeHinweis> neu =
                                repo.findHinweiseBySchrittId(schritt.getHilfeSchrittID());

                        lstHinweise.getItems().setAll(neu);
                    }
                }
            });
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(
                10,
                btnThemaNeu,
                btnThemaLoeschen,
                btnSchrittNeu,
                btnSchrittLoeschen,
                btnSchrittHoch,
                btnSchrittRunter,
                btnHinweisNeu,
                btnHinweisLoeschen,
                spacer,
                btnSchliessen
        );

        btnSchrittHoch.setOnAction(e -> {

            HilfeSchritt aktueller =
                    lstSchritte.getSelectionModel().getSelectedItem();

            if (aktueller == null) return;

            HilfeThema thema =
                    lstThemen.getSelectionModel().getSelectedItem();

            List<HilfeSchritt> liste =
                    repo.findSchritteZuThema(thema.getHilfeThemaID());

            int index = -1;

            for (int i = 0; i < liste.size(); i++) {
                if (liste.get(i).getHilfeSchrittID() == aktueller.getHilfeSchrittID()) {
                    index = i;
                    break;
                }
            }

            if (index <= 0) return;

            HilfeSchritt vorher = liste.get(index - 1);

            int temp = aktueller.getReihenfolge();
            aktueller.setReihenfolge(vorher.getReihenfolge());
            vorher.setReihenfolge(temp);

            repo.updateSchritt(aktueller);
            repo.updateSchritt(vorher);

            List<HilfeSchritt> neu =
                    repo.findSchritteZuThema(thema.getHilfeThemaID());

            lstSchritte.getItems().setAll(neu);
            lstSchritte.getSelectionModel().select(aktueller);
        });

        btnSchrittRunter.setOnAction(e -> {

            HilfeSchritt aktueller =
                    lstSchritte.getSelectionModel().getSelectedItem();

            if (aktueller == null) return;

            HilfeThema thema =
                    lstThemen.getSelectionModel().getSelectedItem();

            List<HilfeSchritt> liste =
                    repo.findSchritteZuThema(thema.getHilfeThemaID());

            int index = -1;

            for (int i = 0; i < liste.size(); i++) {
                if (liste.get(i).getHilfeSchrittID() == aktueller.getHilfeSchrittID()) {
                    index = i;
                    break;
                }
            }

            if (index >= liste.size() - 1) return;

            HilfeSchritt nachher = liste.get(index + 1);

            int temp = aktueller.getReihenfolge();
            aktueller.setReihenfolge(nachher.getReihenfolge());
            nachher.setReihenfolge(temp);

            repo.updateSchritt(aktueller);
            repo.updateSchritt(nachher);

            List<HilfeSchritt> neu =
                    repo.findSchritteZuThema(thema.getHilfeThemaID());

            lstSchritte.getItems().setAll(neu);
            lstSchritte.getSelectionModel().select(aktueller);
        });


        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        VBox root = new VBox(10, splitPane, buttonBar);
        root.setPadding(new Insets(10));

        Scene scene = new Scene(root, 900, 600);
        stage.setScene(scene);
        stage.showAndWait();
    }

    private static void bearbeiteSchritt(HilfeRepository repo,
                                         ListView<HilfeThema> lstThemen,
                                         ListView<HilfeSchritt> lstSchritte,
                                         HilfeSchritt schritt) {

        TextInputDialog dialog = new TextInputDialog(schritt.getText());
        dialog.setTitle("Schritt bearbeiten");
        dialog.setHeaderText("Schritttext ändern");
        dialog.setContentText("Text:");

        dialog.showAndWait().ifPresent(neuerText -> {

            String clean = neuerText.trim();

            if (clean.isEmpty()) {
                return;
            }

            schritt.setText(clean);

            repo.updateSchritt(schritt);

            HilfeThema thema =
                    lstThemen.getSelectionModel().getSelectedItem();

            if (thema != null) {
                List<HilfeSchritt> neu =
                        repo.findSchritteZuThema(thema.getHilfeThemaID());

                lstSchritte.getItems().setAll(neu);
            }
        });
    }
}