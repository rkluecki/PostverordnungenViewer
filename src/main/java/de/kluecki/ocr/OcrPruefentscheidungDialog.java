package de.kluecki.ocr;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;

import java.util.Optional;

public final class OcrPruefentscheidungDialog {

    private OcrPruefentscheidungDialog() {
    }

    public record Ergebnis(
            String entscheidungsart,
            String bemerkung,
            String gepruefteQuelle,
            boolean istErledigt
    ) {
    }

    public static Optional<Ergebnis> showAndWait(
            Window owner,
            String dateiname,
            String aktuellerStatus,
            String vorhandeneEntscheidungsart,
            String vorhandeneBemerkung,
            String vorhandeneQuelle,
            Boolean vorhandenesIstErledigt
    ) {
        Dialog<Ergebnis> dialog = new Dialog<>();

        dialog.setTitle("OCR-Prüfentscheidung");
        dialog.setHeaderText(
                dateiname != null && !dateiname.isBlank()
                        ? dateiname
                        : "OCR-Auffälligkeit"
        );

        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType btnSpeichern = new ButtonType(
                "Speichern",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane()
                .getButtonTypes()
                .addAll(
                        btnSpeichern,
                        ButtonType.CANCEL
                );

        ComboBox<EntscheidungsartItem> cmbEntscheidungsart =
                new ComboBox<>();

        cmbEntscheidungsart.getItems().addAll(
                new EntscheidungsartItem(
                        "QUELLE_OHNE_OCR",
                        "Quelle liefert kein OCR"
                ),
                new EntscheidungsartItem(
                        "LEERE_SEITE",
                        "Leere Seite"
                ),
                new EntscheidungsartItem(
                        "OCR_FEHLERHAFT",
                        "OCR-Datei fehlerhaft"
                ),
                new EntscheidungsartItem(
                        "ZUORDNUNG_UNKLAR",
                        "OCR-Zuordnung unklar"
                ),
                new EntscheidungsartItem(
                        "OCR_NICHT_ERFORDERLICH",
                        "OCR nicht erforderlich"
                ),
                new EntscheidungsartItem(
                        "MANUELLE_NACHARBEIT",
                        "Manuelle Nachbearbeitung erforderlich"
                ),
                new EntscheidungsartItem(
                        "SONSTIGES",
                        "Sonstiger Sonderfall"
                )
        );

        boolean entscheidungGefunden = false;

        if (vorhandeneEntscheidungsart != null
                && !vorhandeneEntscheidungsart.isBlank()) {

            for (EntscheidungsartItem item
                    : cmbEntscheidungsart.getItems()) {

                if (vorhandeneEntscheidungsart.equals(item.code())) {
                    cmbEntscheidungsart.getSelectionModel().select(item);
                    entscheidungGefunden = true;
                    break;
                }
            }
        }

        if (!entscheidungGefunden) {
            cmbEntscheidungsart.getSelectionModel().selectFirst();
        }
        cmbEntscheidungsart.setMaxWidth(Double.MAX_VALUE);

        TextArea txtBemerkung = new TextArea();
        txtBemerkung.setPromptText(
                "Begründung oder Prüfergebnis"
        );
        txtBemerkung.setPrefRowCount(5);
        txtBemerkung.setWrapText(true);

        txtBemerkung.setText(
                vorhandeneBemerkung != null
                        ? vorhandeneBemerkung
                        : ""
        );

        TextField txtGepruefteQuelle = new TextField();
        txtGepruefteQuelle.setPromptText(
                "Zum Beispiel: BLB-METS, Werk 7007501"
        );

        txtGepruefteQuelle.setText(
                vorhandeneQuelle != null
                        ? vorhandeneQuelle
                        : ""
        );

        CheckBox chkIstErledigt = new CheckBox(
                "Auffälligkeit ist fachlich geklärt"
        );

        chkIstErledigt.setSelected(
                vorhandenesIstErledigt == null
                        || vorhandenesIstErledigt
        );

        Label lblBisherigerStatus = new Label(
                aktuellerStatus != null
                        ? aktuellerStatus
                        : ""
        );

        GridPane grid = new GridPane();

        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        grid.add(new Label("Bisheriger Status:"), 0, 0);
        grid.add(lblBisherigerStatus, 1, 0);

        grid.add(new Label("Entscheidung:"), 0, 1);
        grid.add(cmbEntscheidungsart, 1, 1);

        grid.add(new Label("Bemerkung:"), 0, 2);
        grid.add(txtBemerkung, 1, 2);

        grid.add(new Label("Geprüfte Quelle:"), 0, 3);
        grid.add(txtGepruefteQuelle, 1, 3);

        grid.add(chkIstErledigt, 1, 4);

        GridPane.setFillWidth(
                cmbEntscheidungsart,
                true
        );

        GridPane.setFillWidth(
                txtBemerkung,
                true
        );

        GridPane.setFillWidth(
                txtGepruefteQuelle,
                true
        );

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().setPrefWidth(610);

        dialog.setResultConverter(buttonType -> {

            if (buttonType != btnSpeichern) {
                return null;
            }

            EntscheidungsartItem auswahl =
                    cmbEntscheidungsart.getValue();

            if (auswahl == null) {
                return null;
            }

            return new Ergebnis(
                    auswahl.code(),
                    txtBemerkung.getText(),
                    txtGepruefteQuelle.getText(),
                    chkIstErledigt.isSelected()
            );
        });

        return dialog.showAndWait();
    }

    private record EntscheidungsartItem(
            String code,
            String bezeichnung
    ) {
        @Override
        public String toString() {
            return bezeichnung;
        }
    }
}