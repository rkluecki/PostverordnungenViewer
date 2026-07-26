package de.kluecki.db.UI;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.OrdinataOcrKontrolle;
import de.kluecki.db.repository.OrdinataOcrKontrolleRepository;
import javafx.event.ActionEvent;

import java.sql.Connection;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;

public class OrdinataOcrKontrolleBearbeitenDialog {

    private OrdinataOcrKontrolleBearbeitenDialog() {
    }

    public static boolean zeigeZumAnlegen(Stage ownerStage) {

        Dialog<ButtonType> dialog = new Dialog<>();

        dialog.setTitle("OCR-Kontrolllisteneintrag anlegen");
        dialog.setHeaderText("Neuen Eintrag für die OCR-Kontrollliste erfassen");

        if (ownerStage != null) {
            dialog.initOwner(ownerStage);
            dialog.initModality(Modality.WINDOW_MODAL);
        }

        ButtonType speichernButtonType =
                new ButtonType(
                        "Speichern",
                        ButtonBar.ButtonData.OK_DONE
                );

        ButtonType abbrechenButtonType =
                new ButtonType(
                        "Abbrechen",
                        ButtonBar.ButtonData.CANCEL_CLOSE
                );

        dialog.getDialogPane().getButtonTypes().addAll(
                speichernButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();

        grid.setHgap(12);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ColumnConstraints beschriftungSpalte =
                new ColumnConstraints();

        beschriftungSpalte.setMinWidth(155);
        beschriftungSpalte.setPrefWidth(175);

        ColumnConstraints eingabeSpalte =
                new ColumnConstraints();

        eingabeSpalte.setMinWidth(320);
        eingabeSpalte.setPrefWidth(460);
        eingabeSpalte.setHgrow(Priority.ALWAYS);

        grid.getColumnConstraints().addAll(
                beschriftungSpalte,
                eingabeSpalte
        );

        /*
         * Band und Quelle
         */

        Label lblBereichBandQuelle =
                erstelleBereichsUeberschrift(
                        "Band und Quelle"
                );

        TextField txtGebiet = new TextField();
        txtGebiet.setPromptText("z. B. Baden");

        TextField txtJahrVon = new TextField();
        txtJahrVon.setPromptText("z. B. 1837");

        TextField txtJahrBis = new TextField();
        txtJahrBis.setPromptText(
                "Optional bei mehrjährigen Zeiträumen"
        );

        TextField txtBandJahrAnzeige = new TextField();
        txtBandJahrAnzeige.setPromptText(
                "z. B. 1837 oder 1837–1839"
        );

        TextField txtUnterbandTitel = new TextField();
        txtUnterbandTitel.setPromptText(
                "Optional, z. B. Beiheft 1"
        );

        TextField txtArchivName = new TextField();
        txtArchivName.setPromptText(
                "Name des Archivs oder der Bibliothek"
        );

        TextField txtQuellenTitel = new TextField();
        txtQuellenTitel.setPromptText(
                "Titel der digitalen oder gedruckten Quelle"
        );

        TextField txtQuellenUrl = new TextField();
        txtQuellenUrl.setPromptText(
                "URL zur Quelle"
        );

        TextField txtManifestId = new TextField();
        txtManifestId.setPromptText(
                "Optional: Manifest-ID"
        );

        /*
         * Status
         */

        Label lblBereichStatus =
                erstelleBereichsUeberschrift(
                        "Status"
                );

        ComboBox<Auswahlwert> cmbQuellenStatus =
                new ComboBox<>();

        cmbQuellenStatus.getItems().addAll(
                new Auswahlwert("UNBEKANNT", "Unbekannt"),
                new Auswahlwert("QUELLE_FEHLT", "Quelle fehlt"),
                new Auswahlwert("QUELLE_GEFUNDEN", "Quelle gefunden"),
                new Auswahlwert("QUELLE_TEILWEISE", "Quelle teilweise vorhanden"),
                new Auswahlwert("NUR_EINZELFUNDE", "Nur Einzelfunde"),
                new Auswahlwert("NICHT_DIGITAL", "Nicht digital")
        );

        cmbQuellenStatus.getSelectionModel().selectFirst();
        cmbQuellenStatus.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Auswahlwert> cmbErschliessbarkeit =
                new ComboBox<>();

        cmbErschliessbarkeit.getItems().addAll(
                new Auswahlwert("GUT", "Gut"),
                new Auswahlwert("TEILWEISE", "Teilweise"),
                new Auswahlwert("SCHWIERIG", "Schwierig"),
                new Auswahlwert("NUR_EINZELFUNDE", "Nur Einzelfunde"),
                new Auswahlwert(
                        "QUELLE_VORHANDEN_OCR_FEHLT",
                        "Quelle vorhanden, OCR fehlt"
                ),
                new Auswahlwert(
                        "DERZEIT_NICHT_ERSCHLIESSBAR",
                        "Derzeit nicht erschließbar"
                )
        );

        cmbErschliessbarkeit.getSelectionModel().selectLast();
        cmbErschliessbarkeit.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Auswahlwert> cmbOcrStatus =
                new ComboBox<>();

        cmbOcrStatus.getItems().addAll(
                new Auswahlwert("UNBEKANNT", "Unbekannt"),
                new Auswahlwert("OCR_FEHLT", "OCR fehlt"),
                new Auswahlwert("OCR_TEILWEISE", "OCR teilweise vorhanden"),
                new Auswahlwert("OCR_VOLLSTAENDIG", "OCR vollständig"),
                new Auswahlwert("OCR_FEHLERHAFT", "OCR fehlerhaft"),
                new Auswahlwert("EIGENE_OCR_NOETIG", "Eigene OCR nötig"),
                new Auswahlwert(
                        "MAGISTER_VORGESEHEN",
                        "Bearbeitung mit Magister vorgesehen"
                )
        );

        cmbOcrStatus.getSelectionModel().selectFirst();
        cmbOcrStatus.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Auswahlwert> cmbImportStatus =
                new ComboBox<>();

        cmbImportStatus.getItems().addAll(
                new Auswahlwert("NICHT_ANGELEGT", "Band nicht angelegt"),
                new Auswahlwert("BAND_ANGELEGT", "Band angelegt"),
                new Auswahlwert("NICHT_IMPORTIERT", "Nicht importiert"),
                new Auswahlwert(
                        "TEILWEISE_IMPORTIERT",
                        "Teilweise importiert"
                ),
                new Auswahlwert(
                        "VOLLSTAENDIG_IMPORTIERT",
                        "Vollständig importiert"
                ),
                new Auswahlwert("IMPORT_FEHLER", "Importfehler")
        );

        cmbImportStatus.getSelectionModel().selectFirst();
        cmbImportStatus.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Auswahlwert> cmbPruefStatus =
                new ComboBox<>();

        cmbPruefStatus.getItems().addAll(
                new Auswahlwert("NICHT_GEPRUEFT", "Nicht geprüft"),
                new Auswahlwert(
                        "TEILWEISE_GEPRUEFT",
                        "Teilweise geprüft"
                ),
                new Auswahlwert("GEPRUEFT", "Geprüft"),
                new Auswahlwert(
                        "NACHPRUEFUNG_NOETIG",
                        "Nachprüfung nötig"
                )
        );

        cmbPruefStatus.getSelectionModel().selectFirst();
        cmbPruefStatus.setMaxWidth(Double.MAX_VALUE);

        ComboBox<Auswahlwert> cmbPrioritaet =
                new ComboBox<>();

        cmbPrioritaet.getItems().addAll(
                new Auswahlwert("SEHR_HOCH", "Sehr hoch"),
                new Auswahlwert("HOCH", "Hoch"),
                new Auswahlwert("MITTEL", "Mittel"),
                new Auswahlwert("NIEDRIG", "Niedrig")
        );

        cmbPrioritaet.getSelectionModel().select(
                cmbPrioritaet.getItems().get(2)
        );

        cmbPrioritaet.setMaxWidth(Double.MAX_VALUE);

        /*
         * Umfang und Arbeit
         */

        Label lblBereichUmfang =
                erstelleBereichsUeberschrift(
                        "Umfang und Arbeit"
                );

        TextField txtSeitenGesamt = new TextField();
        txtSeitenGesamt.setPromptText(
                "Optional"
        );

        TextField txtSeitenMitOcr = new TextField();
        txtSeitenMitOcr.setPromptText(
                "Optional"
        );

        TextField txtSeitenOhneOcr = new TextField();
        txtSeitenOhneOcr.setPromptText(
                "Optional"
        );

        TextArea txtNaechsterSchritt =
                new TextArea();

        txtNaechsterSchritt.setPromptText(
                "Welcher Arbeitsschritt soll als Nächstes erfolgen?"
        );

        txtNaechsterSchritt.setPrefRowCount(3);
        txtNaechsterSchritt.setWrapText(true);

        TextArea txtBemerkung = new TextArea();

        txtBemerkung.setPromptText(
                "Bemerkungen, Hinweise oder Forschungsnotizen"
        );

        txtBemerkung.setPrefRowCount(5);
        txtBemerkung.setWrapText(true);

        CheckBox chkIstErledigt =
                new CheckBox(
                        "Kontrolllisteneintrag ist abgeschlossen"
                );

        int zeile = 0;

        grid.add(lblBereichBandQuelle, 0, zeile++, 2, 1);

        grid.add(new Label("Gebiet:"), 0, zeile);
        grid.add(txtGebiet, 1, zeile++);

        grid.add(new Label("Jahr von:"), 0, zeile);
        grid.add(txtJahrVon, 1, zeile++);

        grid.add(new Label("Jahr bis:"), 0, zeile);
        grid.add(txtJahrBis, 1, zeile++);

        grid.add(new Label("Band-/Jahranzeige:"), 0, zeile);
        grid.add(txtBandJahrAnzeige, 1, zeile++);

        grid.add(new Label("Unterbandtitel:"), 0, zeile);
        grid.add(txtUnterbandTitel, 1, zeile++);

        grid.add(new Label("Archivname:"), 0, zeile);
        grid.add(txtArchivName, 1, zeile++);

        grid.add(new Label("Quellentitel:"), 0, zeile);
        grid.add(txtQuellenTitel, 1, zeile++);

        grid.add(new Label("Quellen-URL:"), 0, zeile);
        grid.add(txtQuellenUrl, 1, zeile++);

        grid.add(new Label("Manifest-ID:"), 0, zeile);
        grid.add(txtManifestId, 1, zeile++);

        grid.add(lblBereichStatus, 0, zeile++, 2, 1);

        grid.add(new Label("Quellenstatus:"), 0, zeile);
        grid.add(cmbQuellenStatus, 1, zeile++);

        grid.add(new Label("Erschließbarkeit:"), 0, zeile);
        grid.add(cmbErschliessbarkeit, 1, zeile++);

        grid.add(new Label("OCR-Status:"), 0, zeile);
        grid.add(cmbOcrStatus, 1, zeile++);

        grid.add(new Label("Importstatus:"), 0, zeile);
        grid.add(cmbImportStatus, 1, zeile++);

        grid.add(new Label("Prüfstatus:"), 0, zeile);
        grid.add(cmbPruefStatus, 1, zeile++);

        grid.add(new Label("Priorität:"), 0, zeile);
        grid.add(cmbPrioritaet, 1, zeile++);

        grid.add(lblBereichUmfang, 0, zeile++, 2, 1);

        grid.add(new Label("Seiten gesamt:"), 0, zeile);
        grid.add(txtSeitenGesamt, 1, zeile++);

        grid.add(new Label("Seiten mit OCR:"), 0, zeile);
        grid.add(txtSeitenMitOcr, 1, zeile++);

        grid.add(new Label("Seiten ohne OCR:"), 0, zeile);
        grid.add(txtSeitenOhneOcr, 1, zeile++);

        grid.add(new Label("Nächster Schritt:"), 0, zeile);
        grid.add(txtNaechsterSchritt, 1, zeile++);

        grid.add(new Label("Bemerkung:"), 0, zeile);
        grid.add(txtBemerkung, 1, zeile++);

        grid.add(new Label("Abschluss:"), 0, zeile);
        grid.add(chkIstErledigt, 1, zeile);

        ScrollPane scrollPane = new ScrollPane(grid);

        scrollPane.setFitToWidth(true);
        scrollPane.setPannable(false);

        scrollPane.setPrefViewportWidth(690);
        scrollPane.setPrefViewportHeight(650);

        dialog.getDialogPane().setContent(scrollPane);

        dialog.getDialogPane().setPrefWidth(750);
        dialog.getDialogPane().setPrefHeight(760);

        Button btnSpeichern =
                (Button) dialog.getDialogPane()
                        .lookupButton(speichernButtonType);

        boolean[] wurdeGespeichert = {false};

        btnSpeichern.addEventFilter(
                ActionEvent.ACTION,
                event -> {

                    String gebiet =
                            txtGebiet.getText() != null
                                    ? txtGebiet.getText().trim()
                                    : "";

                    if (gebiet.isBlank()) {

                        zeigeFehler(
                                "Pflichtfeld fehlt",
                                "Bitte ein Gebiet eingeben."
                        );

                        txtGebiet.requestFocus();
                        event.consume();
                        return;
                    }

                    Integer jahrVon;
                    Integer jahrBis;
                    Integer seitenGesamt;
                    Integer seitenMitOcr;
                    Integer seitenOhneOcr;

                    try {

                        jahrVon =
                                parseNullableInteger(
                                        txtJahrVon.getText(),
                                        "Jahr von"
                                );

                        jahrBis =
                                parseNullableInteger(
                                        txtJahrBis.getText(),
                                        "Jahr bis"
                                );

                        seitenGesamt =
                                parseNullableInteger(
                                        txtSeitenGesamt.getText(),
                                        "Seiten gesamt"
                                );

                        seitenMitOcr =
                                parseNullableInteger(
                                        txtSeitenMitOcr.getText(),
                                        "Seiten mit OCR"
                                );

                        seitenOhneOcr =
                                parseNullableInteger(
                                        txtSeitenOhneOcr.getText(),
                                        "Seiten ohne OCR"
                                );

                    } catch (IllegalArgumentException ex) {

                        zeigeFehler(
                                "Ungültige Eingabe",
                                ex.getMessage()
                        );

                        event.consume();
                        return;
                    }

                    if (jahrVon != null
                            && jahrBis != null
                            && jahrBis < jahrVon) {

                        zeigeFehler(
                                "Ungültiger Zeitraum",
                                "„Jahr bis“ darf nicht vor „Jahr von“ liegen."
                        );

                        txtJahrBis.requestFocus();
                        event.consume();
                        return;
                    }

                    if (seitenGesamt != null
                            && seitenMitOcr != null
                            && seitenMitOcr > seitenGesamt) {

                        zeigeFehler(
                                "Ungültige Seitenangabe",
                                "„Seiten mit OCR“ darf nicht größer als „Seiten gesamt“ sein."
                        );

                        txtSeitenMitOcr.requestFocus();
                        event.consume();
                        return;
                    }

                    if (seitenGesamt != null
                            && seitenOhneOcr != null
                            && seitenOhneOcr > seitenGesamt) {

                        zeigeFehler(
                                "Ungültige Seitenangabe",
                                "„Seiten ohne OCR“ darf nicht größer als „Seiten gesamt“ sein."
                        );

                        txtSeitenOhneOcr.requestFocus();
                        event.consume();
                        return;
                    }

                    OrdinataOcrKontrolle eintrag =
                            new OrdinataOcrKontrolle();

                    eintrag.setQuelleID(null);

                    eintrag.setGebietBezeichnung(gebiet);
                    eintrag.setJahrVon(jahrVon);
                    eintrag.setJahrBis(jahrBis);

                    eintrag.setBandJahrAnzeige(
                            textOderNull(
                                    txtBandJahrAnzeige.getText()
                            )
                    );

                    eintrag.setUnterbandTitel(
                            textOderNull(
                                    txtUnterbandTitel.getText()
                            )
                    );

                    eintrag.setArchivName(
                            textOderNull(
                                    txtArchivName.getText()
                            )
                    );

                    eintrag.setQuellenTitel(
                            textOderNull(
                                    txtQuellenTitel.getText()
                            )
                    );

                    eintrag.setQuellenUrl(
                            textOderNull(
                                    txtQuellenUrl.getText()
                            )
                    );

                    eintrag.setManifestId(
                            textOderNull(
                                    txtManifestId.getText()
                            )
                    );

                    eintrag.setQuellenStatus(
                            cmbQuellenStatus.getValue().code()
                    );

                    eintrag.setErschliessbarkeit(
                            cmbErschliessbarkeit.getValue().code()
                    );

                    eintrag.setOcrStatus(
                            cmbOcrStatus.getValue().code()
                    );

                    eintrag.setImportStatus(
                            cmbImportStatus.getValue().code()
                    );

                    eintrag.setPruefStatus(
                            cmbPruefStatus.getValue().code()
                    );

                    eintrag.setPrioritaet(
                            cmbPrioritaet.getValue().code()
                    );

                    eintrag.setSeitenGesamt(seitenGesamt);
                    eintrag.setSeitenMitOcr(seitenMitOcr);
                    eintrag.setSeitenOhneOcr(seitenOhneOcr);

                    eintrag.setNaechsterSchritt(
                            textOderNull(
                                    txtNaechsterSchritt.getText()
                            )
                    );

                    eintrag.setBemerkung(
                            textOderNull(
                                    txtBemerkung.getText()
                            )
                    );

                    eintrag.setIstErledigt(
                            chkIstErledigt.isSelected()
                    );

                    try (Connection connection =
                                 DatabaseConnection.getConnection()) {

                        OrdinataOcrKontrolleRepository repository =
                                new OrdinataOcrKontrolleRepository(
                                        connection
                                );

                        repository.insert(eintrag);

                        wurdeGespeichert[0] = true;

                    } catch (Exception ex) {

                        ex.printStackTrace();

                        zeigeFehler(
                                "Speicherfehler",
                                "Der OCR-Kontrolllisteneintrag konnte nicht gespeichert werden."
                        );

                        event.consume();
                    }
                }
        );

        dialog.showAndWait();

        return wurdeGespeichert[0];
    }

    private static Integer parseNullableInteger(
            String text,
            String feldBezeichnung) {

        if (text == null || text.isBlank()) {
            return null;
        }

        String bereinigt = text.trim();

        try {

            int wert = Integer.parseInt(bereinigt);

            if (wert < 0) {
                throw new IllegalArgumentException(
                        "Das Feld „"
                                + feldBezeichnung
                                + "“ darf keinen negativen Wert enthalten."
                );
            }

            return wert;

        } catch (NumberFormatException ex) {

            throw new IllegalArgumentException(
                    "Das Feld „"
                            + feldBezeichnung
                            + "“ muss eine ganze Zahl enthalten."
            );
        }
    }

    private static String textOderNull(
            String text) {

        if (text == null || text.isBlank()) {
            return null;
        }

        return text.trim();
    }

    private static void zeigeFehler(
            String titel,
            String meldung) {

        Alert alert =
                new Alert(Alert.AlertType.ERROR);

        alert.setTitle(titel);
        alert.setHeaderText(null);
        alert.setContentText(meldung);
        alert.showAndWait();
    }

    private static Label erstelleBereichsUeberschrift(
            String text) {

        Label label = new Label(text);

        label.setMaxWidth(Double.MAX_VALUE);

        label.setStyle("""
            -fx-font-size: 14px;
            -fx-font-weight: bold;
            -fx-text-fill: #315f57;
            -fx-background-color: #e2efeb;
            -fx-border-color: #a9c5bd;
            -fx-border-width: 0 0 1 0;
            -fx-padding: 7 9 7 9;
            """);

        GridPane.setHgrow(label, Priority.ALWAYS);

        return label;
    }

    private record Auswahlwert(
            String code,
            String anzeige) {

        @Override
        public String toString() {
            return anzeige;
        }
    }
}

