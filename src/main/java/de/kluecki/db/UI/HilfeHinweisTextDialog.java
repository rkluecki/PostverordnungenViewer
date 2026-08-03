package de.kluecki.db.UI;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

import java.util.Optional;

public final class HilfeHinweisTextDialog {

    private HilfeHinweisTextDialog() {
    }

    public static Optional<String> anzeigen(
            Window owner,
            String titel,
            String kopftext,
            String vorhandenerText
    ) {
        Dialog<String> dialog = new Dialog<>();

        dialog.setTitle(titel);
        dialog.setHeaderText(kopftext);

        if (owner != null) {
            dialog.initOwner(owner);
        }

        ButtonType speichernButton = new ButtonType(
                "Speichern",
                ButtonBar.ButtonData.OK_DONE
        );

        dialog.getDialogPane().getButtonTypes().addAll(
                speichernButton,
                ButtonType.CANCEL
        );

        TextArea txtText = new TextArea();
        txtText.setPromptText("Hinweistext eingeben");
        txtText.setWrapText(true);
        txtText.setPrefRowCount(10);
        txtText.setPrefColumnCount(60);

        if (vorhandenerText != null) {
            txtText.setText(vorhandenerText);
        }

        VBox inhalt = new VBox(8, new Label("Text:"), txtText);
        inhalt.setPadding(new Insets(10));

        dialog.getDialogPane().setContent(inhalt);
        dialog.getDialogPane().setPrefWidth(650);
        dialog.getDialogPane().setPrefHeight(380);

        Button speichern =
                (Button) dialog.getDialogPane().lookupButton(speichernButton);

        speichern.disableProperty().bind(
                txtText.textProperty().isEmpty()
        );

        dialog.setResultConverter(button -> {
            if (button != speichernButton) {
                return null;
            }

            String text = txtText.getText().trim();
            return text.isEmpty() ? null : text;
        });

        dialog.setOnShown(e -> {
            txtText.requestFocus();
            txtText.positionCaret(txtText.getText().length());
        });

        return dialog.showAndWait();
    }
}