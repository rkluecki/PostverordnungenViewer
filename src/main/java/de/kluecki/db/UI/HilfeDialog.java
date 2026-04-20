package de.kluecki.db.UI;

import de.kluecki.db.model.HilfeHinweis;
import de.kluecki.db.model.HilfeSchritt;
import de.kluecki.db.model.HilfeThema;
import de.kluecki.db.repository.HilfeRepository;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class HilfeDialog {

    public static void show(Stage owner) {

        Stage stage = new Stage();
        stage.setTitle("Hilfe");
        stage.initModality(Modality.WINDOW_MODAL);

        if (owner != null) {
            stage.initOwner(owner);
        }

        ListView<HilfeThema> lstThemen = new ListView<>();
        lstThemen.setPrefWidth(250);

        HilfeRepository repo = new HilfeRepository();
        List<HilfeThema> themen = repo.findAllThemen();
        lstThemen.getItems().addAll(themen);

        lstThemen.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override
            protected void updateItem(HilfeThema item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getTitel());
                }
            }
        });

        // WICHTIG: rightPane VOR dem Listener anlegen
        Label lblPlatzhalter = new Label("Bitte links ein Thema auswählen");
        lblPlatzhalter.setStyle("-fx-padding: 20; -fx-font-size: 14px;");

        BorderPane rightPane = new BorderPane();
        rightPane.setCenter(lblPlatzhalter);

        lstThemen.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {

            if (neu == null) {
                return;
            }

            List<HilfeSchritt> schritte =
                    repo.findSchritteByThemaId(neu.getHilfeThemaID());

            schritte.sort(java.util.Comparator.comparingInt(HilfeSchritt::getReihenfolge));

            StringBuilder text = new StringBuilder();

            Label lblTitel = new Label(neu.getTitel());
            lblTitel.setStyle("-fx-font-weight: bold; -fx-padding: 10;");

            for (HilfeSchritt schritt : schritte) {
                text.append(schritt.getReihenfolge())
                        .append(". ")
                        .append(schritt.getText())
                        .append("\n\n");

                List<HilfeHinweis> hinweise =
                        repo.findHinweiseBySchrittId(schritt.getHilfeSchrittID());

                for (HilfeHinweis hinweis : hinweise) {
                    text.append("      >>> ")
                            .append(hinweis.getTyp().toUpperCase())
                            .append(": ")
                            .append(hinweis.getText())
                            .append("\n");
                }
            }

            ListView<String> lstSchritte = new ListView<>();

            for (String zeile : text.toString().split("\n")) {

                if (zeile != null && !zeile.trim().isEmpty()) {
                    lstSchritte.getItems().add(zeile);
                }
            }

            lstSchritte.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);

                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }

                    if (item.matches("\\d+\\..*")) {
                        setText(item);
                        setPadding(new javafx.geometry.Insets(4, 0, 4, 0));
                        setStyle(getStyle() + "-fx-font-weight: bold;");
                    } else {
                        setText(item);
                        setPadding(new javafx.geometry.Insets(4, 0, 4, 0));
                    }

                    if (item.contains(">>>")) {
                        setStyle("-fx-text-fill: #b00020;");
                    }
                }
            });

            BorderPane content = new BorderPane();
            content.setTop(lblTitel);
            content.setCenter(lstSchritte);

            rightPane.setCenter(content);
        });

        SplitPane splitPane = new SplitPane();
        splitPane.getItems().addAll(lstThemen, rightPane);
        splitPane.setDividerPositions(0.3);

        Scene scene = new Scene(splitPane, 900, 600);
        stage.setScene(scene);

        if (!lstThemen.getItems().isEmpty()) {
            lstThemen.getSelectionModel().selectFirst();
        }

        stage.showAndWait();
    }
}