/*
 * Hauptklasse der Anwendung Postverordnungen.
 *
 * Zweck:
 * - Startet die JavaFX-Anwendung
 * - baut Hauptfenster, Navigation und Dokumentansicht auf
 * - verbindet UI, Bildanzeige und fachliche Auswahl
 *
 * Aktueller Stand:
 * - Die UI arbeitet noch überwiegend mit der Altstruktur
 *   rund um Veroeffentlichung und VerordnungBetreff.
 * - Die neue Zielstruktur mit HeftEintrag ist fachlich und
 *   technisch bereits vorbereitet, aber noch nicht vollständig
 *   in die Oberfläche integriert.
 *
 * Fachliche Zielstruktur:
 * Heft -> HeftEintrag -> Inhaltseinheit
 *
 * Wichtige Hinweise:
 * - Diese Klasse enthält aktuell Alt- und Neulogik parallel.
 * - Deshalb keine vorschnellen Umbauten in einem Schritt.
 * - Änderungen nur klein, lokal und in lauffähigem Zustand.
 *
 * Später:
 * - UI schrittweise stärker auf Heft und HeftEintrag ausrichten
 * - Verantwortlichkeiten nach und nach aus dieser Klasse auslagern
 */

package de.kluecki.db;

import de.kluecki.db.UI.DocumentViewerWindow;
import de.kluecki.db.UI.InhaltseinheitenWindow;
import de.kluecki.db.model.HeftEintrag;
import de.kluecki.db.model.HeftEintragTyp;
import de.kluecki.db.model.SeitenMapping;
import de.kluecki.db.print.PrintPdfService;
import de.kluecki.db.repository.*;
import javafx.animation.PauseTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.Modality;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.*;
import javafx.beans.property.SimpleStringProperty;
import de.kluecki.db.model.Heft;
import javafx.scene.layout.GridPane;
import javafx.scene.control.DatePicker;
import de.kluecki.db.config.Config;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

import javafx.scene.control.cell.TextFieldTableCell;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;


public class PostverordnungenApp extends Application {

    // Navigation links
    private ListView<String> gebietListView;
    private ListView<String> bandListView;
    private ListView<Heft> heftListView;

    // Dokumentanzeige / Bild
    private ImageView imageView;
    private ScrollPane imageScrollPane;
    private Image currentImage;

    // Bildnavigation
    private final List<Path> aktuelleBildliste = new ArrayList<>();
    private int aktuellerBildIndex = -1;

    // Anzeigezustand Bild
    private double zoomFactor = 1.0;
    private boolean fitToWindow = true;
    private double rotationAngle = 0;

    // Buttons Bildnavigation
    private Button btnErsteSeite;
    private Button btnZurueck;
    private Button btnWeiter;
    private Button btnLetzteSeite;

    // Statusanzeige / UI-Labels
    private Label lblSeitenstand;
    private TextField txtSeiteDirekt;
    private Label lblBandTitel;
    private Label lblSeitenmarkierung;
    private Label lblAktuellerInhalt;
    private Label lblForschungsnotiz;
    private Label statusLabel;

    // Aktuelle fachliche Auswahl (Navigation)
    private String aktuellesGebiet;
    private String aktuellesBand;
    private Integer markierteStartSeite = null;
    private Integer markierteEndeSeite = null;

    // Drucken
    private Button btnHeftDrucken;
    private Button btnHeftPdf;

    private Button btnHeftEintragDrucken;
    private Button btnHeftEintragPdf;

    private Button btnInhaltPrint;
    private Button btnInhaltPdf;

    private Button btnHeftAendern;
    private Button btnHeftLoeschen;
    private Button btnHeftEintragAendern;
    private Button btnInhaltseinheiten;

    // Tabellen / Detailansichten
    // Neue Zielstruktur
    private TableView<HeftEintrag> tblHeftEintraege; // neue Zielstruktur

    private ListView<InhaltTabellenEintrag> lstInhalteDetail;

    // Repository Zugriff (Datenbank)
    private HeftEintragRepository heftEintragRepository; // neue Struktur
    private HeftRepository heftRepository;
    private QuelleRepository quelleRepository;  // Basisstruktur
    private final SeitenMappingRepository seitenMappingRepository = new SeitenMappingRepository();

    // Sonstiges
    private List<String> gebieteCache = null;
    private List<HeftEintrag> aktuelleHeftEintragListe = new ArrayList<>();
    private ComboBox<String> cmbTypFilter;
    private boolean mappingEnterGedrueckt = false;
    private int mappingZielZeile = -1;
    private boolean mappingAutoWeiter = false;

    private enum NavigationLevel {
        HEFT,
        HEFTEINTRAG,
        INHALT
    }

    private enum MappingStatus {
        NICHT_VORHANDEN,
        VOLLSTAENDIG,
        UNVOLLSTAENDIG
    }

    private NavigationLevel aktuellesLevel = NavigationLevel.HEFT;

    private static final String BACKUP_DIR =
            "D:\\Postgeschichte_PC\\Postverordnungen_Backup";




    @Override
    public void start(Stage stage) {

        try {
            heftEintragRepository = new HeftEintragRepository(DatabaseConnection.getConnection());
            heftRepository = new HeftRepository(DatabaseConnection.getConnection());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Datenbankfehler",
                    "Die Datenbankverbindung konnte nicht aufgebaut werden.\n\n" +
                            "Bitte SQL-Server-Zugang prüfen.\n\n" +
                            "Technischer Hinweis:\n" + e.getMessage());
            return;
        }

        quelleRepository = new QuelleRepository();

        BorderPane root = new BorderPane();

        root.setTop(createMenuBar());
        root.setBottom(createStatusBar());

        gebietListView = new ListView<>();
        gebietListView.getItems().addAll(loadGebiete());

        bandListView = new ListView<>();
        heftListView = new ListView<>();

        VBox imageToolbar = createImageToolbar();
        HBox bildNavigation = createImageNavigationBar();

        lblBandTitel = new Label("Kein Band gewählt");

        lblSeitenmarkierung = new Label("Keine Seitenmarkierung");
        lblSeitenmarkierung.setStyle("""
            -fx-padding: 4 0 4 0;
            -fx-font-style: italic;
            -fx-text-fill: #444444;
        """);

        lblAktuellerInhalt = new Label("Kein Inhaltseintrag auf dieser Seite");
        lblAktuellerInhalt.setStyle("""
            -fx-padding: 4 0 4 0;
            -fx-font-style: italic;
            -fx-text-fill: #444444;
""");

        lblForschungsnotiz = new Label("");
        lblForschungsnotiz.setStyle("""
    -fx-padding: 4 0 4 0;
    -fx-font-style: italic;
    -fx-text-fill: #666666;
""");

        // Tabellen
        tblHeftEintraege = createHeftEintraegeTable();

        // Testgröße neue Struktur
        tblHeftEintraege.setPrefHeight(180);

        lstInhalteDetail = new ListView<>();
        lstInhalteDetail.setPrefHeight(160);
        lstInhalteDetail.setFixedCellSize(40);

        lstInhalteDetail.setCellFactory(param -> new ListCell<InhaltTabellenEintrag>() {

            @Override
            protected void updateItem(InhaltTabellenEintrag item, boolean empty) {

                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                }
                else {
                    setText(item.getNr() + "  " + item.getTitel() + "  S. " + item.getSeite());
                }
            }
        });

        lstInhalteDetail.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {

            aktuellesLevel = NavigationLevel.INHALT;

            if (neu != null) {
                int seiteVon = neu.getSeiteVon();

                if (seiteVon > 0) {
                    springeZuSeite(seiteVon);
                }
            }

            updateOutputButtons();
        });

        currentImage = new Image(getClass().getResourceAsStream("/images/VA842_A001.gif"));
        imageScrollPane = createImageViewer();

        BorderPane documentPane = createDocumentPane(imageToolbar, bildNavigation);

        VBox navigationPane = createNavigationPane();

        SplitPane mainPane = new SplitPane();
        mainPane.getItems().addAll(navigationPane, documentPane);
        mainPane.setDividerPositions(0.30);

        root.setCenter(mainPane);

        configureSelectionListeners();
        configureKeyboardNavigation(root);

        updateImageView();
        updateNavigationState();
        updateOutputButtons();

        Scene scene = new Scene(root);
        stage.setMaximized(true);
        stage.setTitle("Postverordnungen");
        stage.setScene(scene);
        stage.show();
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        Menu menuDatei = new Menu("Datei");
        Menu menuBearbeiten = new Menu("Bearbeiten");
        Menu menuAnsicht = new Menu("Ansicht");
        Menu menuStammdaten = new Menu("Stammdaten");
        Menu menuSuche = new Menu("Suche");

        MenuItem miBackupErstellen = new MenuItem("Backup erstellen");
        miBackupErstellen.setOnAction(e -> backupErstellen());

        menuDatei.getItems().add(
                miBackupErstellen
        );

        MenuItem mnuHeftEintraegeSuchen = new MenuItem("HeftEinträge suchen...");

        mnuHeftEintraegeSuchen.setOnAction(e -> {

            String gewaehltesGebiet = gebietListView.getSelectionModel().getSelectedItem();

            if (gewaehltesGebiet == null || gewaehltesGebiet.isBlank()) {
                showAlert("Hinweis", "Bitte zuerst ein Gebiet auswählen.");
                return;
            }

            int bandId = 0;

            if (aktuellesBand != null && !aktuellesBand.isBlank()) {
                bandId = ermittleBandId(gewaehltesGebiet, aktuellesBand);

                if (bandId <= 0) {
                    showAlert("Fehler", "BandID konnte nicht ermittelt werden.");
                    return;
                }
            }

            Stage ownerStage = (Stage) menuBar.getScene().getWindow();

            de.kluecki.db.UI.HeftEintragSucheDialog.show(
                    ownerStage,
                    bandId,
                    gewaehltesGebiet,
                    aktuellesBand,
                    loadGebiete(),
                    heftEintrag -> {
                        if (heftEintrag != null) {
                            waehleKontextUndTrefferAus(heftEintrag);
                        }
                    }
            );
        });

        menuSuche.getItems().add(mnuHeftEintraegeSuchen);

        Menu menuHilfe = new Menu("Hilfe");

        MenuItem miKurzhilfe = new MenuItem("Kurzhilfe");
        miKurzhilfe.setOnAction(e -> zeigeKurzhilfe());

        MenuItem miUeber = new MenuItem("Über Postverordnungen");
        miUeber.setOnAction(e -> zeigeUeberDialog());

        menuHilfe.getItems().addAll(miKurzhilfe, miUeber);

        MenuItem miBandJahrAnlegen = new MenuItem("Band/Jahr anlegen");
        miBandJahrAnlegen.setOnAction(e -> oeffneBandJahrDialog());

        MenuItem miBandJahrLoeschen = new MenuItem("Band/Jahr löschen");
        miBandJahrLoeschen.setOnAction(e -> oeffneBandJahrLoeschenDialog());

        MenuItem mnuGebiet = new MenuItem("Gebiet anlegen");
        mnuGebiet.setOnAction(e -> oeffneGebietDialog());

        MenuItem miGebietUmbenennen = new MenuItem("Gebiet umbenennen");
        miGebietUmbenennen.setOnAction(e -> oeffneGebietUmbenennenDialog());

        MenuItem miGebietLoeschen = new MenuItem("Gebiet löschen");
        miGebietLoeschen.setOnAction(e -> oeffneGebietLoeschenDialog());

        MenuItem miSeitenmappingBearbeiten = new MenuItem("Seitenmapping bearbeiten");
        miSeitenmappingBearbeiten.setOnAction(e -> oeffneSeitenmappingDialog());

        miSeitenmappingBearbeiten.setAccelerator(
                new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN)
        );

        menuStammdaten.getItems().add(miBandJahrAnlegen);
        menuStammdaten.getItems().add(miBandJahrLoeschen);
        menuStammdaten.getItems().add(mnuGebiet);
        menuStammdaten.getItems().add(miGebietUmbenennen);
        menuStammdaten.getItems().add(miGebietLoeschen);
        menuStammdaten.getItems().add(new SeparatorMenuItem());
        menuStammdaten.getItems().add(miSeitenmappingBearbeiten);

        menuBar.getMenus().addAll(
                menuDatei,
                menuStammdaten,
                menuSuche,
                menuHilfe);
        return menuBar;
    }

    private void zeigeKurzhilfe() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Kurzhilfe");
        alert.setHeaderText("Kurzhilfe Postverordnungen");
        alert.setContentText(
                "1. Gebiet auswählen\n" +
                        "2. Band/Jahr auswählen\n" +
                        "3. Heft auswählen\n" +
                        "4. HeftEintrag auswählen\n" +
                        "5. Inhalt auswählen\n\n" +
                        "Druck und PDF sind möglich für:\n" +
                        "- Heft\n" +
                        "- HeftEintrag\n" +
                        "- Inhalt\n\n" +
                        "Seitenmarkierung wird zum Erfassen von Heft und HeftEintrag verwendet."
        );
        alert.showAndWait();
    }

    private void zeigeUeberDialog() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Über Postverordnungen");
        alert.setHeaderText("Postverordnungen");
        alert.setContentText(
                "Anwendung zur Erfassung und Navigation historischer Postverordnungen.\n\n" +
                        "Aktuelle Zielstruktur:\n" +
                        "Heft -> HeftEintrag -> Inhaltseinheit\n\n" +
                        "Stand:\n" +
                        "Navigation, Erfassung, Druck und PDF sind für den Kernbereich funktionsfähig."
        );
        alert.showAndWait();
    }

    private void oeffneBandJahrLoeschenDialog() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Band/Jahr löschen");
        dialog.setHeaderText("Band/Jahr entfernen");

        ButtonType loeschenButtonType =
                new ButtonType("Löschen", ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechenButtonType =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
                loeschenButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> cmbGebiet = new ComboBox<>();
        cmbGebiet.getItems().addAll(loadGebiete());
        cmbGebiet.setPrefWidth(220);

        ComboBox<String> cmbBand = new ComboBox<>();
        cmbBand.setPrefWidth(220);

        cmbGebiet.setOnAction(e -> {
            String gebiet = cmbGebiet.getValue();

            if (gebiet != null) {
                cmbBand.getItems().setAll(loadBaende(gebiet));
            }
        });

        grid.add(new Label("Gebiet:"), 0, 0);
        grid.add(cmbGebiet, 1, 0);

        grid.add(new Label("Band/Jahr:"), 0, 1);
        grid.add(cmbBand, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Button btnLoeschen =
                (Button) dialog.getDialogPane().lookupButton(loeschenButtonType);

        btnLoeschen.addEventFilter(ActionEvent.ACTION, event -> {

            String gebiet = cmbGebiet.getValue();
            String band = cmbBand.getValue();

            if (gebiet == null) {
                showAlert("Fehler", "Bitte ein Gebiet auswählen.");
                event.consume();
                return;
            }

            if (band == null) {
                showAlert("Fehler", "Bitte ein Band/Jahr auswählen.");
                event.consume();
                return;
            }

            int bandId = ermittleBandId(gebiet, band);

            if (heftRepository.hatHefteZuBand(bandId)) {
                showAlert("Fehler", "Band kann nicht gelöscht werden, solange noch Hefte existieren.");
                event.consume();
                return;
            }

            File rootDir = new File(Config.getImageRootPath());
            File gebietDir = new File(rootDir, gebiet);
            File bandDir = new File(gebietDir, band);

            File[] inhalt = bandDir.listFiles();

            if (inhalt != null && inhalt.length > 0) {
                showAlert("Fehler", "Band kann nur gelöscht werden, wenn der Ordner leer ist.");
                event.consume();
                return;
            }

            try {
                quelleRepository.deleteBand(bandId);

                if (bandDir.exists() && !bandDir.delete()) {
                    showAlert("Fehler", "Band wurde in der DB gelöscht, aber der Ordner konnte nicht gelöscht werden.");
                    event.consume();
                    return;
                }

                List<String> baende = loadBaende(gebiet);
                bandListView.getItems().setAll(baende);
                updateStatusLabel(baende.size());

                bandListView.getSelectionModel().clearSelection();

                heftListView.getItems().clear();
                tblHeftEintraege.getItems().clear();
                lstInhalteDetail.getItems().clear();

                if (gebiet.equals(aktuellesGebiet) && band.equals(aktuellesBand)) {
                    aktuellesBand = null;
                    lblBandTitel.setText("Kein Band gewählt");
                    aktuelleBildliste.clear();
                    aktuellerBildIndex = -1;
                    currentImage = null;
                    imageView.setImage(null);
                    resetSeitenmarkierung();
                    updateNavigationState();
                }

                showAlert("Erfolg", "Band/Jahr wurde gelöscht.");

            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Fehler", "Band/Jahr konnte nicht gelöscht werden.");
                event.consume();
            }
        });

        dialog.showAndWait();
    }

    private void oeffneGebietLoeschenDialog() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Gebiet löschen");
        dialog.setHeaderText("Vorhandenes Gebiet löschen");

        ButtonType loeschenButtonType =
                new ButtonType("Löschen", ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechenButtonType =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
                loeschenButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> cmbGebiet = new ComboBox<>();
        cmbGebiet.getItems().addAll(loadGebiete());
        cmbGebiet.setPrefWidth(220);

        grid.add(new Label("Gebiet:"), 0, 0);
        grid.add(cmbGebiet, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Button btnLoeschen =
                (Button) dialog.getDialogPane().lookupButton(loeschenButtonType);

        btnLoeschen.addEventFilter(ActionEvent.ACTION, event -> {
            String gebiet = cmbGebiet.getValue();

            if (gebiet == null || gebiet.isBlank()) {
                showAlert("Fehler", "Bitte ein Gebiet auswählen.");
                event.consume();
                return;
            }

            if (quelleRepository.hatBaenderZuGebiet(gebiet)) {
                showAlert("Fehler", "Gebiet kann nicht gelöscht werden, solange noch Band/Jahr-Daten in der DB vorhanden sind.");
                event.consume();
                return;
            }

            File rootDir = new File(Config.getImageRootPath());
            File gebietDir = new File(rootDir, gebiet);

            File[] inhalt = gebietDir.listFiles();

            if (inhalt != null && inhalt.length > 0) {
                showAlert("Fehler", "Gebiet kann nur gelöscht werden, wenn es leer ist.");
                event.consume();
                return;
            }

            if (!gebietDir.delete()) {
                showAlert("Fehler", "Gebiet konnte nicht gelöscht werden.");
                event.consume();
                return;
            }

            invalidateGebieteCache();

            gebietListView.getItems().setAll(loadGebiete());
            gebietListView.getSelectionModel().clearSelection();
            bandListView.getItems().clear();
            heftListView.getItems().clear();
            tblHeftEintraege.getItems().clear();
            lstInhalteDetail.getItems().clear();
        });

        dialog.showAndWait();
    }

    private void oeffneGebietUmbenennenDialog() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Gebiet umbenennen");
        dialog.setHeaderText("Vorhandenes Gebiet umbenennen");

        ButtonType speichernButtonType =
                new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechenButtonType =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
                speichernButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> cmbGebiet = new ComboBox<>();
        cmbGebiet.getItems().addAll(loadGebiete());
        cmbGebiet.setPrefWidth(220);

        TextField txtNeuerName = new TextField();
        txtNeuerName.setPromptText("Neuer Gebietsname");

        grid.add(new Label("Gebiet:"), 0, 0);
        grid.add(cmbGebiet, 1, 0);

        grid.add(new Label("Neuer Name:"), 0, 1);
        grid.add(txtNeuerName, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Button btnSpeichern =
                (Button) dialog.getDialogPane().lookupButton(speichernButtonType);

        btnSpeichern.addEventFilter(ActionEvent.ACTION, event -> {

            String altesGebiet = cmbGebiet.getValue();
            String neuerName = txtNeuerName.getText().trim();

            if (altesGebiet == null || altesGebiet.isBlank()) {
                showAlert("Fehler", "Bitte ein Gebiet auswählen.");
                event.consume();
                return;
            }

            if (neuerName.isBlank()) {
                showAlert("Fehler", "Bitte einen neuen Namen eingeben.");
                event.consume();
                return;
            }

            if (neuerName.matches(".*[\\\\/:*?\"<>|].*")) {
                showAlert("Fehler", "Der neue Name enthält ungültige Zeichen.");
                event.consume();
                return;
            }

            File rootDir = new File(Config.getImageRootPath());
            File alterOrdner = new File(rootDir, altesGebiet);
            File neuerOrdner = new File(rootDir, neuerName);

            if (neuerOrdner.exists()) {
                showAlert("Fehler", "Ein Gebiet mit diesem Namen existiert bereits.");
                event.consume();
                return;
            }

            if (!alterOrdner.renameTo(neuerOrdner)) {
                showAlert("Fehler", "Gebiet konnte nicht umbenannt werden.");
                event.consume();
                return;
            }

            invalidateGebieteCache();

            gebietListView.getItems().setAll(loadGebiete());
            gebietListView.getSelectionModel().select(neuerName);
            gebietListView.scrollTo(neuerName);
            gebietListView.requestFocus();
        });

        dialog.showAndWait();
    }

    private void oeffneGebietDialog() {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Gebiet anlegen");
        dialog.setHeaderText("Neues Gebiet erfassen");

        ButtonType speichernButtonType =
                new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechenButtonType =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
                speichernButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtGebiet = new TextField();
        txtGebiet.setPromptText("z.B. Preußen");

        grid.add(new Label("Gebiet:"), 0, 0);
        grid.add(txtGebiet, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Button btnSpeichern =
                (Button) dialog.getDialogPane().lookupButton(speichernButtonType);

        btnSpeichern.addEventFilter(ActionEvent.ACTION, event -> {

            String gebiet = txtGebiet.getText().trim();

            if (gebiet.isBlank()) {
                showAlert("Fehler", "Bitte ein Gebiet eingeben.");
                event.consume();
                return;
            }

            if (gebiet.matches(".*[\\\\/:*?\"<>|].*")) {
                showAlert("Fehler", "Gebiet enthält ungültige Zeichen.");
                event.consume();
                return;
            }

            File rootDir = new File(Config.getImageRootPath());
            File gebietDir = new File(rootDir, gebiet);

            List<String> vorhandeneGebiete = loadGebiete();

            for (String g : vorhandeneGebiete) {
                if (g.equalsIgnoreCase(gebiet)) {
                    showAlert("Fehler", "Gebiet existiert bereits.");
                    event.consume();
                    return;
                }
            }

            if (!gebietDir.mkdir()) {
                showAlert("Fehler", "Gebiet konnte nicht angelegt werden.");
                event.consume();
                return;
            }

            invalidateGebieteCache();

            gebietListView.getItems().setAll(loadGebiete());
            gebietListView.getSelectionModel().select(gebiet);
            gebietListView.scrollTo(gebiet);
            gebietListView.requestFocus();

        });

        dialog.showAndWait();
    }

    private HBox createStatusBar() {
        statusLabel = new Label("0 Quellen geladen");
        statusLabel.setId("statusLabel");

        HBox statusBar = new HBox(statusLabel);
        statusBar.setStyle("""
            -fx-padding: 6 10 6 10;
            -fx-background-color: #eaeaea;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 1 0 0 0;
        """);
        return statusBar;
    }

    private TableView<HeftEintrag> createHeftEintraegeTable() {
        TableView<HeftEintrag> table = new TableView<>();

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPrefWidth(320);
        table.setPlaceholder(new Label("Keine HeftEinträge vorhanden"));

        table.setStyle("""
    -fx-font-size:12px;
    -fx-selection-bar:#2b579a;
    -fx-selection-bar-non-focused:#2b579a;
    """);

        table.setFixedCellSize(45);

        TableColumn<HeftEintrag, String> colNro = new TableColumn<>("Nro");

        colNro.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getNro() != null
                                ? cellData.getValue().getNro()
                                : ""
                )
        );

        colNro.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setTooltip(null);
                } else {
                    setText(item);
                    setTooltip(new Tooltip(item));
                }
            }
        });

        colNro.setPrefWidth(70);
        colNro.setSortable(false);

        TableColumn<HeftEintrag, String> colTyp = new TableColumn<>("Typ");

        colTyp.setCellValueFactory(cellData ->

                new SimpleStringProperty(

                        cellData.getValue().getTypBezeichnung() != null
                                ? cellData.getValue().getTypBezeichnung()
                                : ""

                )

        );

        colTyp.setPrefWidth(110);
        colTyp.setSortable(false);

        colTyp.setCellFactory(column -> new TableCell<>(){

            @Override
            protected void updateItem(String item, boolean empty){

                super.updateItem(item, empty);

                if(empty || item == null){

                    setText(null);
                    setStyle("");

                    return;
                }

                setText(item);

                switch(item){

                    case "Verordnung" ->
                            setStyle("-fx-text-fill: #1f4e79; -fx-font-weight: bold;");

                    case "Bekanntmachung" ->
                            setStyle("-fx-text-fill: #666666;");

                    case "Dienstanweisung" ->
                            setStyle("-fx-text-fill: #7a3e00;");

                    case "Tarif" ->
                            setStyle("-fx-text-fill: #006400;");

                    case "Kursänderung" ->
                            setStyle("-fx-text-fill: #8b0000;");

                    default ->
                            setStyle("-fx-text-fill: black;");
                }
            }
        });

        TableColumn<HeftEintrag, String> colTitel = new TableColumn<>("Titel");
        colTitel.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getTitel() != null
                                ? cellData.getValue().getTitel()
                                : ""
                )
        );
        colTitel.setPrefWidth(180);

        TableColumn<HeftEintrag, String> colDatum = new TableColumn<>("Datum");
        colDatum.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getDatum() != null
                                ? cellData.getValue().getDatum().toString()
                                : ""
                )
        );
        colDatum.setPrefWidth(90);
        colDatum.setSortable(false);

        TableColumn<HeftEintrag, String> colSeite = new TableColumn<>("Seite");
        colSeite.setCellValueFactory(cellData -> {
            HeftEintrag eintrag = cellData.getValue();

            String von = String.valueOf(eintrag.getSeiteVon());
            String bis = String.valueOf(eintrag.getSeiteBis());

            String seite;
            if (!von.isBlank() && !bis.isBlank()) {
                seite = von.equals(bis) ? von : von + "-" + bis;
            } else if (!von.isBlank()) {
                seite = von;
            } else {
                seite = "";
            }

            return new SimpleStringProperty(seite);
        });
        colSeite.setPrefWidth(80);
        colSeite.setSortable(false);

        table.getColumns().addAll(colNro, colTyp, colTitel, colDatum, colSeite);

        table.setRowFactory(tv -> {
            TableRow<HeftEintrag> row = new TableRow<>();

            row.itemProperty().addListener((obs, oldItem, newItem) -> {
                if (newItem == null) {
                    row.setTooltip(null);
                    return;
                }

                String notiz = newItem.getForschungsnotiz();

                if (notiz != null && !notiz.isBlank()) {
                    row.setTooltip(new Tooltip(notiz));
                } else {
                    row.setTooltip(null);
                }
            });

            return row;
        });

        return table;
    }

    private VBox createNavigationPane() {
        Label lblGebiete = new Label("Gebiete");
        Label lblBaende = new Label("Jahr / Band");

        Label lblHefte = new Label("Hefte");
        lblHefte.setStyle("-fx-font-weight: bold;");

        Label lblHeftEintraege = new Label("HeftEinträge");
        lblHeftEintraege.setStyle("-fx-font-weight: bold;");

        cmbTypFilter = new ComboBox<>();

        cmbTypFilter.getItems().add("Alle");

        for(HeftEintragTyp typ : heftEintragRepository.findAllTypen()){
            cmbTypFilter.getItems().add(typ.getBezeichnung());
        }

        cmbTypFilter.getSelectionModel().selectFirst();

        Label lblBetreffe = new Label("Inhalte des HeftEintrags");
        lblBetreffe.setStyle("-fx-font-weight: bold;");

        btnInhaltseinheiten = new Button("Inhaltseinträge");
        btnInhaltseinheiten.setMaxWidth(Double.MAX_VALUE);
        btnInhaltseinheiten.setDisable(true);

        btnInhaltseinheiten.setOnAction(e -> {
            HeftEintrag heftEintrag = tblHeftEintraege.getSelectionModel().getSelectedItem();

            if (heftEintrag == null) {
                showAlert("Hinweis", "Bitte zuerst einen HeftEintrag auswählen.");
                return;
            }

            InhaltseinheitenWindow.open(heftEintrag, () -> {
                ladeInhalteZuHeftEintrag(heftEintrag);
                updateOutputButtons();
            }, seite -> {
                aktuellerBildIndex = seite - 1;
                ladeAktuellesBild();
            });
        });
        VBox navigation = new VBox(8,
                lblGebiete,
                gebietListView,
                lblBaende,
                bandListView,
                lblHefte,
                heftListView,
                lblHeftEintraege,
                cmbTypFilter,
                tblHeftEintraege,
                lblBetreffe,
                lstInhalteDetail,
                btnInhaltseinheiten
        );

        navigation.setPrefWidth(380);
        navigation.setMinWidth(320);
        navigation.setMaxWidth(450);
        navigation.setStyle("""
            -fx-padding: 8;
            -fx-background-color: #f4f4f4;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 0 1 0 0;
        """);

        gebietListView.setPrefHeight(150);
        bandListView.setPrefHeight(250);
        heftListView.setPrefHeight(120);

        heftListView.setOnMouseClicked(e -> {

            aktuellesLevel = NavigationLevel.HEFT;

            Heft heft = heftListView.getSelectionModel().getSelectedItem();

            if (heft == null) {
                return;
            }

            tblHeftEintraege.getSelectionModel().clearSelection();
            lstInhalteDetail.getSelectionModel().clearSelection();
            lstInhalteDetail.getItems().clear();
            lblForschungsnotiz.setText("");
            lblAktuellerInhalt.setText("Kein HeftEintrag ausgewählt");

            updateNavigationState();
            updateOutputButtons();
        });

        VBox.setVgrow(lstInhalteDetail, Priority.NEVER);

        cmbTypFilter.setOnAction(e -> {

            String filter = cmbTypFilter.getValue();

            List<HeftEintrag> aktuelleListe =
                    new ArrayList<>(aktuelleHeftEintragListe);

            if (filter == null || filter.equals("Alle")) {
                tblHeftEintraege.getItems().setAll(aktuelleHeftEintragListe);
                return;
            }

            List<HeftEintrag> gefiltert =
                    aktuelleListe.stream()
                            .filter(h -> filter.equals(h.getTypBezeichnung()))
                            .toList();

            tblHeftEintraege.setItems(
                    FXCollections.observableArrayList(gefiltert)
            );
        });

        return navigation;
    }

    private void ladeInhalteZuHeftEintrag(HeftEintrag heftEintrag) {
        lstInhalteDetail.getItems().clear();

        if (heftEintrag == null) {
            return;
        }

        try {
            InhaltseinheitRepository repository = new InhaltseinheitRepository();

            List<InhaltTabellenEintrag> liste =
                    repository.findByHeftEintragId(heftEintrag.getHeftEintragID());

            lstInhalteDetail.getItems().addAll(liste);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void waehleHeftInListeAus(int heftId) {
        for (Heft heft : heftListView.getItems()) {
            if (heft.getHeftID() == heftId) {
                heftListView.getSelectionModel().select(heft);
                heftListView.scrollTo(heft);
                return;
            }
        }
    }

    private void waehleHeftEintragInTabelleAus(int heftEintragId) {
        for (HeftEintrag eintrag : tblHeftEintraege.getItems()) {
            if (eintrag.getHeftEintragID() == heftEintragId) {
                tblHeftEintraege.getSelectionModel().select(eintrag);
                tblHeftEintraege.scrollTo(eintrag);
                return;
            }
        }
    }

    private void waehleKontextUndTrefferAus(HeftEintrag heftEintrag) {
        if (heftEintrag == null) {
            return;
        }

        String zielGebiet = heftEintrag.getGebietAnzeige();
        String zielBand = heftEintrag.getBandJahrAnzeige();

        if (zielGebiet != null && !zielGebiet.isBlank()) {
            gebietListView.getSelectionModel().select(zielGebiet);
            gebietListView.scrollTo(zielGebiet);
        }

        if (zielBand != null && !zielBand.isBlank()) {
            bandListView.getSelectionModel().select(zielBand);
            bandListView.scrollTo(zielBand);
        }

        Platform.runLater(() -> {
            waehleHeftInListeAus(heftEintrag.getHeftID());

            Platform.runLater(() ->
                    waehleHeftEintragInTabelleAus(heftEintrag.getHeftEintragID())
            );
        });
    }

    private BorderPane createDocumentPane(VBox imageToolbar, HBox bildNavigation) {
        BorderPane documentPane = new BorderPane();
        documentPane.setStyle("""
            -fx-padding: 10;
            -fx-background-color: white;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 1;
        """);

        VBox documentHeader = createDocumentHeader(imageToolbar);
        documentPane.setTop(documentHeader);
        documentPane.setCenter(imageScrollPane);
        documentPane.setBottom(bildNavigation);

        return documentPane;
    }

    private VBox createDocumentHeader(VBox imageToolbar) {
        VBox documentHeader = new VBox(
                6,
                lblBandTitel,
                lblSeitenmarkierung,
                lblAktuellerInhalt,
                lblForschungsnotiz,
                imageToolbar
        );
        documentHeader.setStyle("""
            -fx-padding: 8;
            -fx-background-color: #f8f8f8;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 0 0 1 0;
        """);
        return documentHeader;
    }

    private VBox createImageToolbar() {
        Button btnZoomIn = new Button("+");
        Button btnZoomOut = new Button("-");
        Button btnFit = new Button("Anpassen");
        Button btnRotateLeft = new Button("⟲");
        Button btnRotateRight = new Button("⟳");

        btnInhaltPdf = new Button("Inhalt PDF");
        btnInhaltPdf.setTooltip(new Tooltip("Ausgewählten Inhalt als PDF speichern"));
        btnInhaltPrint = new Button("Inhalt drucken");
        btnInhaltPrint.setTooltip(new Tooltip("Ausgewählten Inhalt drucken"));

        Button btnStartSeitenmarkierung = new Button("Startseite markieren");
        Button btnEndeSeitenmarkierung = new Button("Endseite markieren");
        Button btnHeftErfassen = new Button("Heft erfassen");
        btnHeftErfassen.setOnAction(e -> oeffneHeftDialog());

        btnHeftAendern = new Button("Heft ändern");
        btnHeftAendern.setOnAction(e -> heftAendern());

        btnHeftLoeschen = new Button("Heft löschen");
        btnHeftLoeschen.setOnAction(e -> heftLoeschen());

        Button btnHeftEintragErfassen = new Button("Heft Eintrag erfassen");
        btnHeftEintragErfassen.setOnAction(e -> oeffneHeftEintragDialog());
        btnHeftEintragAendern = new Button("Heft Eintrag ändern");
        btnHeftEintragAendern.setOnAction(e -> heftEintragAendern());

        btnHeftDrucken = new Button("Heft drucken");
        btnHeftDrucken.setOnAction(e -> printSelectedHeft());
        btnHeftPdf = new Button("Heft PDF");
        btnHeftPdf.setOnAction(e -> exportSelectedHeftToPdf());
        btnHeftDrucken.setDisable(true);
        btnHeftPdf.setDisable(true);
        btnHeftAendern.setDisable(true);
        btnHeftLoeschen.setDisable(true);

        btnHeftEintragDrucken = new Button("HeftEintrag drucken");
        btnHeftEintragDrucken.setOnAction(e -> printSelectedHeftEintrag());
        btnHeftEintragPdf = new Button("HeftEintrag PDF");
        btnHeftEintragPdf.setOnAction(e -> exportSelectedHeftEintragToPdf());
        btnHeftEintragDrucken.setDisable(true);
        btnHeftEintragPdf.setDisable(true);
        btnHeftEintragAendern.setDisable(true);

        btnZoomIn.setOnAction(e -> {
            if (fitToWindow) {
                zoomFactor = calculateFitZoom();
                fitToWindow = false;
            }
            zoomFactor *= 1.25;
            updateImageView();
        });

        btnZoomOut.setOnAction(e -> {
            if (fitToWindow) {
                zoomFactor = calculateFitZoom();
                fitToWindow = false;
            }
            zoomFactor /= 1.25;
            updateImageView();
        });

        btnFit.setOnAction(e -> {
            fitToWindow = true;
            updateImageView();
        });

        btnRotateLeft.setOnAction(e -> {
            rotationAngle -= 90;
            updateImageView();
        });

        btnRotateRight.setOnAction(e -> {
            rotationAngle += 90;
            updateImageView();
        });

        btnStartSeitenmarkierung.setOnAction(e -> {
            if (aktuellerBildIndex < 0) return;
            markierteStartSeite = aktuellerBildIndex + 1;
            markierteEndeSeite = null;
            updateSeitenmarkierung();
        });

        btnEndeSeitenmarkierung.setOnAction(e -> {
            if (aktuellerBildIndex < 0) return;

            if (markierteStartSeite == null) {
                markierteStartSeite = aktuellerBildIndex + 1;
                markierteEndeSeite = aktuellerBildIndex + 1;
            } else {
                markierteEndeSeite = aktuellerBildIndex + 1;

                if (markierteEndeSeite < markierteStartSeite) {
                    int temp = markierteStartSeite;
                    markierteStartSeite = markierteEndeSeite;
                    markierteEndeSeite = temp;
                }
            }

            updateSeitenmarkierung();
        });

        btnInhaltPdf.setOnAction(e -> exportSelectedInhaltToPdf());
        btnInhaltPrint.setOnAction(e -> printSelectedInhalt());

        HBox row1 = new HBox(
                5,
                btnZoomIn,
                btnZoomOut,
                btnFit,
                btnRotateLeft,
                btnRotateRight,
                new Separator(),
                btnStartSeitenmarkierung,
                btnEndeSeitenmarkierung
        );

        HBox row2 = new HBox(
                5,
                btnHeftErfassen,
                btnHeftAendern,
                btnHeftLoeschen,
                new Separator(),
                btnHeftEintragErfassen,
                btnHeftEintragAendern,
                new Separator(),
                btnHeftEintragDrucken,
                btnHeftEintragPdf,
                new Separator(),
                btnHeftDrucken,
                btnHeftPdf,
                new Separator(),
                btnInhaltPrint,
                btnInhaltPdf
        );

        row1.setPadding(new Insets(2, 0, 2, 0));
        row2.setPadding(new Insets(2, 0, 2, 0));

        VBox imageToolbar = new VBox(4, row1, row2);

        imageToolbar.setPadding(new Insets(5));

        imageToolbar.setStyle("""
        -fx-padding: 5;
        -fx-background-color: #f0f0f0;
        -fx-border-color: #d0d0d0;
    """);

        return imageToolbar;
    }

    private void heftLoeschen() {

        Heft heft = heftListView.getSelectionModel().getSelectedItem();

        if (heft == null) {
            showAlert("Hinweis", "Bitte zuerst ein Heft auswählen.");
            return;
        }

        if (heftEintragRepository.hatEintraegeZuHeft(heft.getHeftID())) {
            showAlert("Fehler", "Heft kann nicht gelöscht werden, solange noch HeftEinträge vorhanden sind.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Heft löschen");
        confirm.setHeaderText(null);
        confirm.setContentText("Soll das ausgewählte Heft wirklich gelöscht werden?");

        Optional<ButtonType> result = confirm.showAndWait();

        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        try {
            heftRepository.delete(heft.getHeftID());

            int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);
            heftListView.getItems().setAll(heftRepository.findByBand(bandId));
            heftListView.getSelectionModel().clearSelection();

            tblHeftEintraege.getItems().clear();
            lstInhalteDetail.getItems().clear();

            resetSeitenmarkierung();

            showAlert("Erfolg", "Heft wurde gelöscht.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fehler", "Heft konnte nicht gelöscht werden.");
        }
    }

    private HBox createImageNavigationBar() {
        btnErsteSeite = new Button("⏮");
        btnZurueck = new Button("← Zurück");
        btnWeiter = new Button("Weiter →");
        btnLetzteSeite = new Button("⏭");

        lblSeitenstand = new Label("0 / 0");

        txtSeiteDirekt = new TextField();
        txtSeiteDirekt.setPrefWidth(60);
        txtSeiteDirekt.setVisible(false);
        txtSeiteDirekt.setManaged(false);

        lblSeitenstand.setOnMouseClicked(e -> {
            if (aktuelleBildliste.isEmpty()) return;

            txtSeiteDirekt.setText(String.valueOf(aktuellerBildIndex + 1));
            lblSeitenstand.setVisible(false);
            lblSeitenstand.setManaged(false);

            txtSeiteDirekt.setVisible(true);
            txtSeiteDirekt.setManaged(true);
            txtSeiteDirekt.requestFocus();
            txtSeiteDirekt.selectAll();
        });

        txtSeiteDirekt.setOnAction(e -> {
            String eingabe = txtSeiteDirekt.getText() != null
                    ? txtSeiteDirekt.getText().trim()
                    : "";

            boolean gesprungen = false;

            String dateiname = findeDateinameZuLogischerSeite(eingabe);

            if (dateiname != null && !dateiname.isBlank()) {
                Path bildPfad = findeBildPfadInAktuellerListe(dateiname);

                if (bildPfad != null) {
                    int index = aktuelleBildliste.indexOf(bildPfad);

                    if (index >= 0) {
                        wechsleAufBandEbeneFuerFreieNavigation();
                        aktuellerBildIndex = index;
                        ladeAktuellesBild();
                        gesprungen = true;
                    }
                }
            }

            // Zahlen-Fallback vorerst deaktiviert,
            // damit nur echte logische Seiten verwendet werden.

            if (!gesprungen && !eingabe.isBlank()) {
                showAlert("Hinweis", "Seite '" + eingabe + "' wurde nicht gefunden.");
            }

            txtSeiteDirekt.setVisible(false);
            txtSeiteDirekt.setManaged(false);
            lblSeitenstand.setVisible(true);
            lblSeitenstand.setManaged(true);
        });

        btnZurueck.setOnAction(e -> zeigeVorherigesBild());
        btnWeiter.setOnAction(e -> zeigeNaechstesBild());

        btnErsteSeite.setOnAction(e -> {
            zeigeSeitenstandLabel();

            if (!aktuelleBildliste.isEmpty()) {
                aktuellerBildIndex = 0;
                ladeAktuellesBild();
            }
        });

        btnLetzteSeite.setOnAction(e -> {
            zeigeSeitenstandLabel();

            if (!aktuelleBildliste.isEmpty()) {
                aktuellerBildIndex = aktuelleBildliste.size() - 1;
                ladeAktuellesBild();
            }
        });

        HBox bildNavigation = new HBox(
                20,
                btnErsteSeite,
                btnZurueck,
                lblSeitenstand,
                txtSeiteDirekt,
                btnWeiter,
                btnLetzteSeite
        );
        bildNavigation.setStyle("-fx-alignment: center;");

        return bildNavigation;
    }

    private ScrollPane createImageViewer() {
        imageView = new ImageView(currentImage);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
        imageView.setCursor(javafx.scene.Cursor.HAND);

        Tooltip imageTooltip = new Tooltip("Doppelklick öffnet die Detailansicht");
        imageTooltip.setShowDelay(javafx.util.Duration.millis(200));
        Tooltip.install(imageView, imageTooltip);

        imageView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                openDocumentWindow();
            }
        });

        ScrollPane scrollPane = new ScrollPane(imageView);
        scrollPane.setPannable(true);

        scrollPane.setOnScroll(event -> {
            if (event.getDeltaY() < 0) {
                zeigeNaechstesBild();
            } else if (event.getDeltaY() > 0) {
                zeigeVorherigesBild();
            }
        });

        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> updateImageView());

        return scrollPane;
    }

    private void configureSelectionListeners() {

        gebietListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {

            bandListView.getSelectionModel().clearSelection();
            heftListView.getSelectionModel().clearSelection();
            tblHeftEintraege.getSelectionModel().clearSelection();
            lstInhalteDetail.getSelectionModel().clearSelection();

            bandListView.getItems().clear();
            heftListView.getItems().clear();
            tblHeftEintraege.getItems().clear();
            lstInhalteDetail.getItems().clear();

            aktuellesGebiet = newValue;
            aktuellesBand = null;

            lblBandTitel.setText("Kein Band gewählt");
            lblAktuellerInhalt.setText("Kein Inhaltseintrag auf dieser Seite");
            resetSeitenmarkierung();

            if (newValue == null) {
                updateStatusLabel(0);
                return;
            }

            List<String> baende = loadBaende(newValue);
            bandListView.getItems().setAll(baende);
            updateStatusLabel(baende.size());

            if (!baende.isEmpty()) {
                bandListView.getSelectionModel().selectFirst();
                bandListView.scrollTo(0);
            }
        });

        bandListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue == null) return;

            String gebiet = gebietListView.getSelectionModel().getSelectedItem();
            String band = newValue;

            aktuellesGebiet = gebiet;
            aktuellesBand = band;
            lblBandTitel.setText(gebiet + " – " + band);
            resetSeitenmarkierung();
            updateInhaltListe();

            heftListView.getItems().clear();
            tblHeftEintraege.getItems().clear();
            lstInhalteDetail.getItems().clear();

            int bandId = ermittleBandId(gebiet, band);

            if (bandId > 0) {
                List<Heft> hefte = heftRepository.findByBand(bandId);

                heftListView.setCellFactory(param -> new ListCell<>() {
                    @Override
                    protected void updateItem(Heft heft, boolean empty) {
                        super.updateItem(heft, empty);

                        if (empty || heft == null) {
                            setText(null);
                        } else {

                            String text = "Heft " + heft.getHeftNummer();

                            if (heft.getSeiteVon() > 0) {

                                if (heft.getSeiteBis() > 0 &&
                                        heft.getSeiteVon() != heft.getSeiteBis()) {

                                    text += " (" + heft.getSeiteVon() +
                                            "–" + heft.getSeiteBis() + ")";

                                } else {

                                    text += " (" + heft.getSeiteVon() + ")";
                                }
                            }

                            setText(text);
                        }
                    }
                });

                heftListView.getItems().setAll(hefte);
            }

            List<File> bilder = loadBilder(gebiet, band);

            aktuelleBildliste.clear();
            aktuellerBildIndex = -1;

            if (!bilder.isEmpty()) {
                for (File bild : bilder) {
                    aktuelleBildliste.add(bild.toPath());
                }

                MappingStatus status = pruefeMappingStatusFuerAktuellesBand();

                if (status == MappingStatus.UNVOLLSTAENDIG) {
                    showAlert("Fehler",
                            "Das Seitenmapping ist unvollständig.\n" +
                                    "Bitte prüfen oder neu aufbauen.");

                    currentImage = null;
                    imageView.setImage(null);
                    updateNavigationState();
                    return;
                }

                if (status == MappingStatus.NICHT_VORHANDEN) {
                    starteInitialMappingImHintergrund(bandId, () -> {
                        aktuellerBildIndex = 0;
                        ladeAktuellesBild();
                    });
                    return;
                }

                aktuellerBildIndex = 0;
                ladeAktuellesBild();

            } else {
                currentImage = null;
                imageView.setImage(null);
                updateNavigationState();
            }
        });

        heftListView.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {

            aktuellesLevel = NavigationLevel.HEFT;

            if (newValue != null) {
                Integer seiteVon = newValue.getSeiteVon();

                if (seiteVon > 0 && seiteVon <= aktuelleBildliste.size()) {
                    aktuellerBildIndex = seiteVon - 1;
                    ladeAktuellesBild();
                }

                tblHeftEintraege.getItems().clear();
                lstInhalteDetail.getItems().clear();
                lblForschungsnotiz.setText("");

                try {
                    List<HeftEintrag> liste =
                            heftEintragRepository.findByHeft(newValue.getHeftID());

                    aktuelleHeftEintragListe.clear();
                    aktuelleHeftEintragListe.addAll(liste);

                    tblHeftEintraege.getItems().setAll(aktuelleHeftEintragListe);
                    tblHeftEintraege.getSelectionModel().clearSelection();
                    cmbTypFilter.getSelectionModel().selectFirst();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }

            updateOutputButtons();
        });

        tblHeftEintraege.getSelectionModel().selectedItemProperty().addListener((o, alt, neu) -> {

            aktuellesLevel = NavigationLevel.HEFTEINTRAG;

            if (neu != null) {
                aktuellerBildIndex = neu.getSeiteVon() - 1;
                ladeAktuellesBild();

                ladeInhalteZuHeftEintrag(neu);

                if (neu.getForschungsnotiz() != null && !neu.getForschungsnotiz().isBlank()) {
                    lblForschungsnotiz.setText("Notiz: " + neu.getForschungsnotiz());
                } else {
                    lblForschungsnotiz.setText("");
                }

                lblAktuellerInhalt.setText("HeftEintrag: " + neu.getTitel());

            } else {
                lblForschungsnotiz.setText("");
                lblAktuellerInhalt.setText("Kein HeftEintrag ausgewählt");
                lstInhalteDetail.getItems().clear();
            }

            updateOutputButtons();

        });
    }

      private void springeZuSeite(int seite) {
        if (aktuelleBildliste.isEmpty()) {
        //    System.out.println("DEBUG: springeZuSeite - Keine Bilder geladen!");
            return;
        }

        if (seite < 1 || seite > aktuelleBildliste.size()) {
            return;
        }

        aktuellerBildIndex = seite - 1;
        ladeAktuellesBild();
    }

    private void wechsleAufBandEbeneFuerFreieNavigation() {
        lstInhalteDetail.getSelectionModel().clearSelection();
        tblHeftEintraege.getSelectionModel().clearSelection();
        heftListView.getSelectionModel().clearSelection();

        lstInhalteDetail.getItems().clear();
        lblForschungsnotiz.setText("");
        lblAktuellerInhalt.setText("Kein HeftEintrag ausgewählt");

        aktuellesLevel = NavigationLevel.HEFT;
        updateOutputButtons();
    }

    private void waehleHeftEintragFuerSeite(int seite) {

        for (HeftEintrag eintrag : tblHeftEintraege.getItems()) {

            if (seite >= eintrag.getSeiteVon() &&
                    seite <= eintrag.getSeiteBis()) {

                if (tblHeftEintraege.getSelectionModel().getSelectedItem() != eintrag) {

                    tblHeftEintraege.getSelectionModel().select(eintrag);
                    tblHeftEintraege.scrollTo(eintrag);

                }

                return;
            }
        }

        tblHeftEintraege.getSelectionModel().clearSelection();
        lstInhalteDetail.getItems().clear();
        lblForschungsnotiz.setText("");
        lblAktuellerInhalt.setText("Kein HeftEintrag auf dieser Seite");
    }

    private int ermittleBandId(String gebiet, String band) {
        return quelleRepository.findBandId(gebiet, band);
    }

    private void configureKeyboardNavigation(BorderPane root) {
        root.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            switch (event.getCode()) {
                case LEFT -> {
                    zeigeVorherigesBild();
                    event.consume();
                }
                case RIGHT -> {
                    zeigeNaechstesBild();
                    event.consume();
                }
            }
        });
    }

    private void openDocumentWindow() {
        if (currentImage == null) {
            showAlert("Hinweis", "Es ist kein Dokumentbild geladen.");
            return;
        }

        String titel = "Postverordnung";
        if (aktuellesGebiet != null && aktuellesBand != null) {
            titel = aktuellesGebiet + " – " + aktuellesBand;
        }

        String quelle = aktuellesGebiet != null ? aktuellesGebiet : "";
        String jahr = aktuellesBand != null ? aktuellesBand : "";
        String typ = "Seite " + (aktuellerBildIndex >= 0 ? (aktuellerBildIndex + 1) : "-");

        DocumentViewerWindow.open(
                currentImage,
                titel,
                quelle,
                jahr,
                typ
        );
    }

    private void resetSeitenmarkierung(){
        markierteStartSeite = null;
        markierteEndeSeite = null;
        updateSeitenmarkierung();;
    }

    private void updateSeitenmarkierung(){
        if (lblSeitenmarkierung == null) return;

        if (markierteStartSeite == null && markierteEndeSeite == null) {
            lblSeitenmarkierung.setText("Keine Seitenmarkierung");
        } else if (markierteStartSeite != null && markierteEndeSeite == null) {
            lblSeitenmarkierung.setText("Seitenmarkierung: Startseite " + markierteStartSeite);
        } else if (markierteStartSeite != null && markierteEndeSeite != null && markierteStartSeite.equals(markierteEndeSeite)) {
            lblSeitenmarkierung.setText("Seitenmarkierung: Seite " + markierteStartSeite);
        } else {
            lblSeitenmarkierung.setText(
                    "Seitenmarkierung: Seite " + markierteStartSeite + " bis " + markierteEndeSeite
            );
        }
    }

    private void updateOutputButtons() {

        boolean inhaltSelected =
                lstInhalteDetail.getSelectionModel().getSelectedItem() != null;

        btnInhaltPrint.setDisable(!inhaltSelected);
        btnInhaltPdf.setDisable(!inhaltSelected);

        boolean heftEintragSelected =
                tblHeftEintraege.getSelectionModel().getSelectedItem() != null;

        btnHeftEintragDrucken.setDisable(!heftEintragSelected);
        btnHeftEintragPdf.setDisable(!heftEintragSelected);

        btnHeftEintragAendern.setDisable(!heftEintragSelected);
        btnInhaltseinheiten.setDisable(!heftEintragSelected);

        boolean heftSelected =
                heftListView.getSelectionModel().getSelectedItem() != null;

        btnHeftDrucken.setDisable(!heftSelected);
        btnHeftPdf.setDisable(!heftSelected);

        btnHeftAendern.setDisable(!heftSelected);
        btnHeftLoeschen.setDisable(!heftSelected);
    }

    private void showAlert(String titel, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titel);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

       private void backupErstellen() {

        try {

            Connection conn = DatabaseConnection.getConnection();

            String zeitstempel =
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
                    );

            File backupDir = new File(BACKUP_DIR);

            if(!backupDir.exists()){
                showAlert("Backup Fehler",
                        "Backup Ordner existiert nicht:\n" + BACKUP_DIR);
                return;
            }

            String backupPfad =
                    BACKUP_DIR +
                            "\\QuellenDB_" +
                            zeitstempel +
                            ".bak";

            String sql =
                    "BACKUP DATABASE QuellenDB TO DISK = '" +
                            backupPfad +
                            "' WITH INIT";

            Statement stmt = conn.createStatement();
            stmt.execute(sql);

            showAlert(
                    "Backup erfolgreich",
                    "Datenbank wurde gesichert:\n" + backupPfad
            );

        } catch (Exception ex) {
            ex.printStackTrace();

            showAlert(
                    "Backup Fehler",
                    "Backup konnte nicht erstellt werden:\n" + ex.getMessage()
            );
        }
    }

    private void zeigeVorherigesBild() {
        zeigeSeitenstandLabel();

        if (aktuelleBildliste.isEmpty()) return;

        int bereichVonIndex = getAktiverBereichVon() - 1;

        if (aktuellerBildIndex > bereichVonIndex) {
            aktuellerBildIndex--;
            ladeAktuellesBild();
        }
    }

    private void zeigeNaechstesBild() {
        zeigeSeitenstandLabel();

        if (aktuelleBildliste.isEmpty()) return;

        int bereichBisIndex = getAktiverBereichBis() - 1;

        if (aktuellerBildIndex < bereichBisIndex) {
            aktuellerBildIndex++;
            ladeAktuellesBild();
        }
    }

    private void ladeAktuellesBild() {
        Path bildPfad = getAktuellerBildPfadAusListe();

        if (bildPfad == null) {
            return;
        }

        currentImage = new Image(bildPfad.toUri().toString());
        imageView.setImage(currentImage);

        updateImageView();
        updateNavigationState();

        updateInhaltAnzeige(aktuellerBildIndex + 1);
    }

    private Path getAktuellerBildPfadAusListe() {
        if (aktuelleBildliste.isEmpty()) {
            return null;
        }

        if (aktuellerBildIndex < 0 || aktuellerBildIndex >= aktuelleBildliste.size()) {
            return null;
        }

        return aktuelleBildliste.get(aktuellerBildIndex);
    }

    private String ermittleLogischeSeiteZumAktuellenDateinamen() {

        String dateiname = getAktuellerDateinameAusListe();

        if (dateiname == null || dateiname.isBlank()) {
            return null;
        }

        return findeLogischeSeiteZuDateiname(dateiname);
    }

    private String findeDateinameZuLogischerSeite(String logischeSeite) {

        if (logischeSeite == null || logischeSeite.isBlank()) {
            return null;
        }

        if (aktuellesGebiet == null || aktuellesBand == null) {
            return null;
        }

        int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

        if (bandId <= 0) {
            return null;
        }

        return seitenMappingRepository.findDateinameByBandIdAndLogischeSeite(
                bandId,
                logischeSeite
        );
    }

    private void updateNavigationState() {
        boolean hatBilder = !aktuelleBildliste.isEmpty();

        int bereichVonIndex = getAktiverBereichVon() - 1;
        int bereichBisIndex = getAktiverBereichBis() - 1;

        boolean istErste = aktuellerBildIndex <= bereichVonIndex;
        boolean istLetzte = !hatBilder || aktuellerBildIndex >= bereichBisIndex;

        if (btnZurueck != null) btnZurueck.setDisable(!hatBilder || istErste);
        if (btnWeiter != null) btnWeiter.setDisable(!hatBilder || istLetzte);
        if (btnErsteSeite != null) btnErsteSeite.setDisable(!hatBilder || istErste);
        if (btnLetzteSeite != null) btnLetzteSeite.setDisable(!hatBilder || istLetzte);

        if (lblSeitenstand != null) {
            if (hatBilder && aktuellerBildIndex >= 0) {
                String text = (aktuellerBildIndex + 1) + " / " + aktuelleBildliste.size();

                String logischeSeite = ermittleLogischeSeiteZumAktuellenDateinamen();

                if (logischeSeite != null && !logischeSeite.isBlank()) {
                    text += " | logisch: " + logischeSeite;
                }

                String dateiname = getAktuellerDateinameAusListe();
                if (dateiname != null && !dateiname.isBlank()) {
                    text += " | " + dateiname;
                }

                lblSeitenstand.setText(text);
            } else {
                lblSeitenstand.setText("0 / 0");
            }
        }
    }

    private String getAktuellerDateinameAusListe() {
        Path bildPfad = getAktuellerBildPfadAusListe();

        if (bildPfad == null) {
            return null;
        }

        return bildPfad.getFileName().toString();
    }

    private Path findeBildPfadInAktuellerListe(String dateiname) {

        if (dateiname == null || dateiname.isBlank()) {
            return null;
        }

        for (Path path : aktuelleBildliste) {
            if (path != null && path.getFileName().toString().equalsIgnoreCase(dateiname)) {
                return path;
            }
        }

        return null;
    }

    private void updateImageView() {
        if (currentImage == null || imageView == null || imageScrollPane == null) {
            return;
        }

        imageView.setRotate(rotationAngle);

        if (fitToWindow) {
            double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
            double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

            if (viewportWidth <= 0 || viewportHeight <= 0) {
                return;
            }

            double imageWidth = currentImage.getWidth();
            double imageHeight = currentImage.getHeight();

            double scaleX = viewportWidth / imageWidth;
            double scaleY = viewportHeight / imageHeight;
            double scale = Math.min(scaleX, scaleY);

            imageView.setFitWidth(imageWidth * scale);
            imageView.setFitHeight(imageHeight * scale);
        } else {
            imageView.setFitWidth(currentImage.getWidth() * zoomFactor);
            imageView.setFitHeight(currentImage.getHeight() * zoomFactor);
        }
    }

    private List<String> loadGebiete() {

        if (gebieteCache == null) {
            gebieteCache = loadGebieteFromFilesystem();
        }

        return gebieteCache;
    }

    private List<String> loadGebieteFromFilesystem() {

        File rootDir = new File(Config.getImageRootPath());

        if (!rootDir.exists() || !rootDir.isDirectory()) {
            return List.of();
        }

        File[] dirs = rootDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return List.of();
        }

        return Arrays.stream(dirs)
                .map(File::getName)
                .sorted()
                .toList();
    }

    private void invalidateGebieteCache() {
        gebieteCache = null;
    }

    private List<String> loadBaende(String gebiet) {
        return quelleRepository.findBandTitelByGebiet(gebiet);
    }

    private List<File> loadBilder(String gebiet, String band) {
        File gebietDir = new File(Config.getImageRootPath(), gebiet);

        if (!gebietDir.exists() || !gebietDir.isDirectory()) {
            return List.of();
        }

        File bandDir = new File(gebietDir, band);

        File[] files = null;

        if (bandDir.exists() && bandDir.isDirectory()) {

            files = bandDir.listFiles(file ->
                    file.isFile() && (
                            file.getName().toLowerCase().endsWith(".jpg") ||
                                    file.getName().toLowerCase().endsWith(".jpeg") ||
                                    file.getName().toLowerCase().endsWith(".png") ||
                                    file.getName().toLowerCase().endsWith(".gif")
                    )
            );

            if (files != null && files.length > 0) {
                return Arrays.stream(files)
                        .sorted(Comparator.comparing(File::getName))
                        .toList();
            }
        }

        try {
            int jahr = Integer.parseInt(band);

            File[] unterordner = gebietDir.listFiles(File::isDirectory);

            if (unterordner != null) {
                for (File ordner : unterordner) {

                    String name = ordner.getName().trim();

                    if (name.matches("\\d{4}-\\d{4}")) {

                        String[] teile = name.split("-");

                        int jahrVon = Integer.parseInt(teile[0]);
                        int jahrBis = Integer.parseInt(teile[1]);

                        if (jahr >= jahrVon && jahr <= jahrBis) {

                            File[] zeitraumFiles = ordner.listFiles(file ->
                                    file.isFile() && (
                                            file.getName().toLowerCase().endsWith(".jpg") ||
                                                    file.getName().toLowerCase().endsWith(".jpeg") ||
                                                    file.getName().toLowerCase().endsWith(".png") ||
                                                    file.getName().toLowerCase().endsWith(".gif")
                                    )
                            );

                            if (zeitraumFiles != null && zeitraumFiles.length > 0) {
                                return Arrays.stream(zeitraumFiles)
                                        .sorted(Comparator.comparing(File::getName))
                                        .toList();
                            }
                        }
                    }
                }
            }

        } catch (NumberFormatException ex) {
            // Band ist kein Jahr → nichts tun
        }

        return List.of();
    }

    private Image loadImageForPage(String gebiet, String bandJahr, int seite) {

        List<File> bilder = loadBilder(gebiet, bandJahr);

        if (seite < 1 || seite > bilder.size()) {
            return null;
        }

        File bildDatei = bilder.get(seite - 1);
        return new Image(bildDatei.toURI().toString());
    }

    private PrintPdfService createPrintPdfService() {
        return new PrintPdfService(
                Config.getImageRootPath(),
                this::loadImageForPage
        );
    }

    private void exportSelectedInhaltToPdf() {

        InhaltTabellenEintrag selected =
                lstInhalteDetail.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst einen Inhalt auswählen.");
            return;
        }

        PrintPdfService service = createPrintPdfService();
        service.exportRangeToPdf(
                aktuellesGebiet,
                aktuellesBand,
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                selected.getTitel()
        );
    }

    private void exportSelectedHeftToPdf() {

        Heft selected =
                heftListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst ein Heft auswählen.");
            return;
        }

        PrintPdfService service = createPrintPdfService();

        service.exportRangeToPdf(
                aktuellesGebiet,
                aktuellesBand,
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                "Heft " + selected.getHeftNummer()
        );

    }

    private void exportSelectedHeftEintragToPdf() {

        HeftEintrag selected =
                tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst einen HeftEintrag auswählen.");
            return;
        }

        PrintPdfService service = createPrintPdfService();

        service.exportRangeToPdf(
                aktuellesGebiet,
                aktuellesBand,
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                selected.getTitel()
        );

    }

    private void printSelectedHeft() {

        Heft selected =
                heftListView.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst ein Heft auswählen.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Druckmodus wählen");
        alert.setHeaderText("Wie soll das Heft gedruckt werden?");

        ButtonType btOriginalDruck = new ButtonType("Original (Farbe)");
        ButtonType btSparmodus = new ButtonType("Sparmodus");
        ButtonType btAbbrechen =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(
                btOriginalDruck,
                btSparmodus,
                btAbbrechen
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == btAbbrechen) {
            return;
        }

        boolean sparmodus = result.get() == btSparmodus;

        PrintPdfService service = createPrintPdfService();

        service.printRange(
                aktuellesGebiet,
                aktuellesBand,
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                "Heft " + selected.getHeftNummer(),
                sparmodus
        );

    }

    private void printSelectedInhalt() {

        InhaltTabellenEintrag selected =
                lstInhalteDetail.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst einen Inhalt auswählen.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Druckmodus wählen");
        alert.setHeaderText("Wie soll der Inhalt gedruckt werden?");

        ButtonType btOriginalDruck = new ButtonType("Original (Farbe)");
        ButtonType btSparmodus = new ButtonType("Sparmodus");
        ButtonType btAbbrechen = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btOriginalDruck, btSparmodus, btAbbrechen);

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == btAbbrechen) {
            return;
        }

        boolean sparmodus = result.get() == btSparmodus;

        PrintPdfService service = createPrintPdfService();
        service.printRange(
                aktuellesGebiet,
                aktuellesBand,
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                selected.getTitel(),
                sparmodus
        );
    }

    private void printSelectedHeftEintrag() {

        HeftEintrag selected =
                tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst einen HeftEintrag auswählen.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Druckmodus wählen");
        alert.setHeaderText("Wie soll der HeftEintrag gedruckt werden?");

        ButtonType btOriginalDruck = new ButtonType("Original (Farbe)");
        ButtonType btSparmodus = new ButtonType("Sparmodus");
        ButtonType btAbbrechen =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(
                btOriginalDruck,
                btSparmodus,
                btAbbrechen
        );

        Optional<ButtonType> result = alert.showAndWait();

        if (result.isEmpty() || result.get() == btAbbrechen) {
            return;
        }

        boolean sparmodus = result.get() == btSparmodus;

        PrintPdfService service = createPrintPdfService();

        service.printRange(
                aktuellesGebiet,
                aktuellesBand,
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                selected.getTitel(),
                sparmodus
        );
    }

    private double calculateFitZoom() {
        if (currentImage == null || imageScrollPane == null) {
            return 1.0;
        }

        double viewportWidth = imageScrollPane.getViewportBounds().getWidth();
        double viewportHeight = imageScrollPane.getViewportBounds().getHeight();

        if (viewportWidth <= 0 || viewportHeight <= 0) {
            return 1.0;
        }

        double imageWidth = currentImage.getWidth();
        double imageHeight = currentImage.getHeight();

        double scaleX = viewportWidth / imageWidth;
        double scaleY = viewportHeight / imageHeight;

        return Math.min(scaleX, scaleY);
    }

    //  ********************************************************************************************
    //                             Hier sind alle Methoden für das Mapping
    //  ********************************************************************************************

    private MappingStatus pruefeMappingStatusFuerAktuellesBand() {

        if (aktuellesGebiet == null || aktuellesBand == null) {
            return MappingStatus.NICHT_VORHANDEN;
        }

        int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

        if (bandId <= 0) {
            return MappingStatus.NICHT_VORHANDEN;
        }

        int anzahlBilder = aktuelleBildliste.size();
        int anzahlMapping = seitenMappingRepository.countByBandId(bandId);

        if (anzahlMapping == 0) {
            return MappingStatus.NICHT_VORHANDEN;
        }

        if (anzahlMapping == anzahlBilder) {
            return MappingStatus.VOLLSTAENDIG;
        }

        return MappingStatus.UNVOLLSTAENDIG;
    }

    private void starteInitialMappingImHintergrund(int bandId, Runnable onSuccess) {

        if (statusLabel != null) {
            statusLabel.setText("Seitenmapping wird erstellt ...");
        }

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                List<String> dateinamen = aktuelleBildliste.stream()
                        .map(path -> path.getFileName().toString())
                        .toList();

                seitenMappingRepository.initialisiereGrundmappingFuerBand(
                        bandId,
                        dateinamen
                );
                return null;
            }
        };

        task.setOnSucceeded(event -> {
            if (statusLabel != null) {
                statusLabel.setText("Seitenmapping wurde erstellt.");
            }

            if (onSuccess != null) {
                onSuccess.run();
            }

            showAlert("Hinweis", "Grundmapping wurde automatisch erstellt.");

            updateStatusLabel(bandListView.getItems().size());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }

            if (statusLabel != null) {
                statusLabel.setText("Fehler beim Erstellen des Seitenmappings.");
            }

            showAlert("Fehler", "Grundmapping konnte nicht erstellt werden.");
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String ermittleLogischeSeiteZumAktuellenBild() {

        if (aktuellesGebiet == null || aktuellesBand == null) {
            return null;
        }

        if (aktuellerBildIndex < 0) {
            return null;
        }

        int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

        if (bandId <= 0) {
            return null;
        }

        return seitenMappingRepository.findLogischeSeiteByBandIdAndBildIndex(
                bandId,
                aktuellerBildIndex + 1
        );
    }

    private String findeLogischeSeiteZuDateiname(String dateiname) {

        if (dateiname == null || dateiname.isBlank()) {
            return null;
        }

        if (aktuellesGebiet == null || aktuellesBand == null) {
            return null;
        }

        int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

        if (bandId <= 0) {
            return null;
        }

        return seitenMappingRepository.findLogischeSeiteByBandIdAndDateiname(
                bandId,
                dateiname
        );
    }

    //  ****************************************************************************************

    private void oeffneSeitenmappingDialog() {

        if (aktuellesGebiet == null || aktuellesGebiet.isBlank()
                || aktuellesBand == null || aktuellesBand.isBlank()) {

            showAlert("Hinweis", "Bitte zuerst ein Gebiet und ein Band auswählen.");
            return;
        }

        int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

        if (bandId <= 0) {
            showAlert("Fehler", "BandID konnte nicht ermittelt werden.");
            return;
        }

        Stage ownerStage = null;

        if (gebietListView != null && gebietListView.getScene() != null) {
            ownerStage = (Stage) gebietListView.getScene().getWindow();
        }

        Stage stage = new Stage();
        stage.setTitle("Seitenmapping bearbeiten");
        stage.initModality(Modality.WINDOW_MODAL);

        if (ownerStage != null) {
            stage.initOwner(ownerStage);
        }

        Label lblTitel = new Label("Seitenmapping für das aktuell gewählte Band");
        lblTitel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        Label lblHinweis = new Label("Bereit");
        lblHinweis.setStyle("-fx-text-fill: #444444;");

        ImageView previewImageView = new ImageView();
        previewImageView.setPreserveRatio(true);
        previewImageView.setFitWidth(480);
        previewImageView.setFitHeight(620);
        previewImageView.setSmooth(true);

        ScrollPane previewScrollPane = new ScrollPane(previewImageView);
        previewScrollPane.setFitToWidth(true);
        previewScrollPane.setFitToHeight(true);
        previewScrollPane.setPrefWidth(500);
        previewScrollPane.setPrefHeight(600);

        Label lblVorschau = new Label("Vorschau");
        lblVorschau.setStyle("-fx-font-weight: bold;");

        Consumer<String> zeigeInfo = text -> {
            lblHinweis.setText(text);
            lblHinweis.setStyle("-fx-text-fill: #444444;");
        };

        Consumer<String> zeigeFehler = text -> {
            lblHinweis.setText(text);
            lblHinweis.setStyle("-fx-text-fill: #b00020; -fx-font-weight: bold;");
        };

        Runnable setzeHinweisZurueck = () -> {
            PauseTransition pause = new PauseTransition(Duration.seconds(3));
            pause.setOnFinished(e -> zeigeInfo.accept("Bereit"));
            pause.play();
        };

        TableView<SeitenMapping> table = new TableView<>();

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(SeitenMapping item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setStyle("");
                    return;
                }

                int aktuellerBildIndex1basiert = aktuellerBildIndex + 1;

                if (item.getBildIndex() == aktuellerBildIndex1basiert) {
                    setStyle("-fx-background-color: #fff3b0; -fx-text-fill: black;");
                } else {
                    setStyle("-fx-text-fill: black;");
                }
            }
        });

        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setEditable(true);

        TableColumn<SeitenMapping, String> colIndex = new TableColumn<>("BildIndex");

        colIndex.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getBildIndex() + ""
                )
        );

        TableColumn<SeitenMapping, String> colDateiname = new TableColumn<>("Dateiname");

        colDateiname.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getDateiname()
                )
        );

        setzeSchwarzeTabellenZellen(colIndex);
        setzeSchwarzeTabellenZellen(colDateiname);

        TableColumn<SeitenMapping, String> colLogisch = new TableColumn<>("Logische Seite");

        Label lblIndexHeader = new Label("BildIndex");
        lblIndexHeader.setMaxWidth(Double.MAX_VALUE);
        lblIndexHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblDateinameHeader = new Label("Dateiname");
        lblDateinameHeader.setMaxWidth(Double.MAX_VALUE);
        lblDateinameHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label lblLogischHeader = new Label("Logische Seite");
        lblLogischHeader.setMaxWidth(Double.MAX_VALUE);
        lblLogischHeader.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        colIndex.setText(null);
        colIndex.setGraphic(lblIndexHeader);

        colDateiname.setText(null);
        colDateiname.setGraphic(lblDateinameHeader);

        colLogisch.setText(null);
        colLogisch.setGraphic(lblLogischHeader);

        colLogisch.setCellValueFactory(data ->
                new SimpleStringProperty(
                        data.getValue().getLogischeSeite()
                )
        );

        colLogisch.setCellFactory(tc -> {
            TextFieldTableCell<SeitenMapping, String> cell =
                    new TextFieldTableCell<>(new DefaultStringConverter()) {

                        @Override
                        public void updateItem(String item, boolean empty) {
                            super.updateItem(item, empty);

                            setTextFill(javafx.scene.paint.Color.BLACK);

                            if (empty) {
                                setText(null);
                                setGraphic(null);
                                return;
                            }

                            if (isEditing()) {
                                if (getGraphic() instanceof TextField textField) {
                                    textField.setStyle("-fx-text-fill: black;");
                                }
                            } else {
                                setStyle("-fx-text-fill: black;");
                            }
                        }

                        @Override
                        public void startEdit() {
                            super.startEdit();

                            if (getGraphic() instanceof TextField textField) {
                                textField.setStyle("-fx-text-fill: black;");
                            }
                        }
                    };

            cell.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
                if (event.getCode() == KeyCode.ENTER) {
                    mappingEnterGedrueckt = true;
                    mappingZielZeile = cell.getIndex() + 1;

                    System.out.println("ENTER gedrückt in Zeile: " + cell.getIndex());
                }
            });

            return cell;
        });

        colLogisch.setOnEditCommit(event -> {

            System.out.println("EDIT COMMIT: Zeile "
                    + event.getTablePosition().getRow()
                    + " | alt=" + event.getOldValue()
                    + " | neu=" + event.getNewValue());

            SeitenMapping eintrag = event.getRowValue();
            String neuerWert = event.getNewValue() != null
                    ? event.getNewValue().trim()
                    : "";

            if (neuerWert.isBlank()) {
                zeigeFehler.accept("Logische Seite darf nicht leer sein");
                setzeHinweisZurueck.run();
                table.refresh();
                return;
            }

            if (hatDoppelteLogischeSeite(table, eintrag, neuerWert)) {
                zeigeFehler.accept("Diese logische Seite existiert bereits");
                setzeHinweisZurueck.run();
                table.refresh();
                return;
            }

            eintrag.setLogischeSeite(neuerWert);

            seitenMappingRepository.updateLogischeSeite(
                    bandId,
                    eintrag.getBildIndex(),
                    neuerWert
            );

            zeigeInfo.accept("Seite gespeichert: " + neuerWert);
            setzeHinweisZurueck.run();

            table.refresh();

            System.out.println("EDIT COMMIT: Zeile "
                    + event.getTablePosition().getRow()
                    + " | alt=" + event.getOldValue()
                    + " | neu=" + event.getNewValue());

            System.out.println("AUSGEWAEHLTE ZEILE: " + table.getSelectionModel().getSelectedIndex());

            if (mappingEnterGedrueckt) {
                int zielZeile = mappingZielZeile;

                mappingEnterGedrueckt = false;
                mappingZielZeile = -1;

                Platform.runLater(() -> {
                    if (zielZeile >= 0 && zielZeile < table.getItems().size()) {
                        mappingAutoWeiter = true;

                        table.getSelectionModel().clearSelection();
                        table.getFocusModel().focus(zielZeile, colLogisch);
                        table.edit(zielZeile, colLogisch);
                    }
                });
            }
        });

        table.getColumns().addAll(colIndex, colDateiname, colLogisch);

        colIndex.setPrefWidth(110);
        colDateiname.setPrefWidth(320);
        colLogisch.setPrefWidth(140);

        table.setPlaceholder(new Label("Noch keine Daten geladen"));
        table.setPrefHeight(400);
        table.setPrefWidth(500);
        table.setMaxWidth(500);

        List<SeitenMapping> mappingListe =
                seitenMappingRepository.findByBandId(bandId);

        table.getItems().addAll(mappingListe);
        table.setPlaceholder(new Label("Keine Mapping-Daten vorhanden"));

        if (aktuellerBildIndex >= 0) {
            int aktuellerBildIndex1basiert = aktuellerBildIndex + 1;

            for (SeitenMapping eintrag : table.getItems()) {
                if (eintrag.getBildIndex() == aktuellerBildIndex1basiert) {

                    String dateiname = eintrag.getDateiname();

                    if (dateiname != null && !dateiname.isBlank()) {
                        Path bildPfad = findeBildPfadInAktuellerListe(dateiname);

                        if (bildPfad != null) {
                            Image previewImage = new Image(bildPfad.toUri().toString());
                            previewImageView.setImage(previewImage);
                        } else {
                            previewImageView.setImage(null);
                        }
                    } else {
                        previewImageView.setImage(null);
                    }

                    table.scrollTo(eintrag);

                    Platform.runLater(() -> {
                        table.getSelectionModel().clearSelection();
                        table.refresh();
                    });

                    break;
                }
            }
        }

        table.getSelectionModel().selectedItemProperty().addListener((obs, alt, neu) -> {

            if (neu == null) {
                return;
            }

            String dateiname = neu.getDateiname();

            if (dateiname != null && !dateiname.isBlank()) {

                Path bildPfad = findeBildPfadInAktuellerListe(dateiname);

                if (bildPfad != null) {
                    Image previewImage = new Image(bildPfad.toUri().toString());
                    previewImageView.setImage(previewImage);
                } else {
                    previewImageView.setImage(null);
                }

            } else {
                previewImageView.setImage(null);
            }

            int zielSeite = neu.getBildIndex();

            if (zielSeite > 0 && zielSeite <= aktuelleBildliste.size()) {
                aktuellerBildIndex = zielSeite - 1;
                ladeAktuellesBild();
                table.refresh();

                if (mappingAutoWeiter) {
                    mappingAutoWeiter = false;
                } else {
                    Platform.runLater(() -> table.getSelectionModel().clearSelection());
                }
            }
        });

        Button btnSchliessen = new Button("Schließen");
        btnSchliessen.setOnAction(e -> stage.close());

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonBar = new HBox(10, spacer, btnSchliessen);
        buttonBar.setPadding(new Insets(0, 20, 20, 20));

        VBox leftContent = new VBox(10, lblTitel, table, lblHinweis);
        leftContent.setPadding(new Insets(20));

        VBox rightContent = new VBox(10, lblVorschau, previewScrollPane);
        rightContent.setPadding(new Insets(20, 20, 20, 0));

        BorderPane root = new BorderPane();
        HBox centerBox = new HBox(10, leftContent, rightContent);
        HBox.setHgrow(leftContent, Priority.ALWAYS);
        HBox.setHgrow(rightContent, Priority.NEVER);
        root.setCenter(centerBox);
        root.setBottom(buttonBar);

        Scene scene = new Scene(root, 1100, 720);
        stage.setScene(scene);

        zeigeInfo.accept("Bereit");

        stage.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private boolean hatDoppelteLogischeSeite(TableView<SeitenMapping> table,
                                             SeitenMapping aktuellerEintrag,
                                             String logischeSeite) {

        if (logischeSeite == null || logischeSeite.isBlank()) {
            return false;
        }

        for (SeitenMapping eintrag : table.getItems()) {
            if (eintrag == null) {
                continue;
            }

            if (eintrag == aktuellerEintrag) {
                continue;
            }

            String vorhanden = eintrag.getLogischeSeite();

            if (vorhanden != null && vorhanden.trim().equalsIgnoreCase(logischeSeite.trim())) {
                return true;
            }
        }

        return false;
    }

    private <T> void setzeSchwarzeTabellenZellen(TableColumn<SeitenMapping, T> column) {
        column.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);

                setTextFill(javafx.scene.paint.Color.BLACK);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.valueOf(item));
                }
            }
        });
    }

    private void updateInhaltAnzeige(String text) {
        lblAktuellerInhalt.setText(text);
    }

    private void updateInhaltAnzeige(int aktuelleSeite) {

        HeftEintrag eintrag =
                tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (eintrag != null) {

            String seiten;

            if (eintrag.getSeiteVon() == eintrag.getSeiteBis()) {
                seiten = "S. " + eintrag.getSeiteVon();
            } else {
                seiten = "S. " + eintrag.getSeiteVon() + "-" + eintrag.getSeiteBis();
            }

            String typText =
                    eintrag.getTypBezeichnung() != null
                            ? eintrag.getTypBezeichnung()
                            : "Eintrag";

            lblAktuellerInhalt.setText(
                    typText + ": " + eintrag.getTitel() + " (" + seiten + ")"
            );

        } else {

            lblAktuellerInhalt.setText("Kein HeftEintrag ausgewählt");

        }
    }

    private void updateStatusLabel(int anzahlQuellen) {
        if (statusLabel != null) {
            if (anzahlQuellen == 1) {
                statusLabel.setText("1 Quelle geladen");
            } else {
                statusLabel.setText(anzahlQuellen + " Quellen geladen");
            }
        }
    }

    private void updateInhaltListe() {

        try {

            lstInhalteDetail.getItems().clear();

            HeftEintrag aktuellerEintrag =
                    tblHeftEintraege.getSelectionModel().getSelectedItem();

            if (aktuellerEintrag == null) {
                return;
            }

            ladeInhalteZuHeftEintrag(aktuellerEintrag);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void oeffneHeftEintragDialog() {

        Heft aktuellesHeft = heftListView.getSelectionModel().getSelectedItem();

        if (aktuellesHeft == null) {
            showAlert("Hinweis", "Bitte zuerst ein Heft auswählen.");
            return;
        }

        if (markierteStartSeite == null || markierteEndeSeite == null) {
            showAlert("Hinweis", "Bitte zuerst einen Seitenbereich markieren.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Neuen HeftEintrag anlegen");
        dialog.setHeaderText("HeftEintrag aus markiertem Seitenbereich anlegen");

        ButtonType speichernButtonType = new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechenButtonType = new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(speichernButtonType, abbrechenButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");

        TextField txtNro = new TextField();
        TextField txtTitel = new TextField();

        ComboBox<HeftEintragTyp> cmbTyp = new ComboBox<>();

        List<HeftEintragTyp> typen =
                heftEintragRepository.findAllTypen();

        cmbTyp.getItems().addAll(typen);

        if(!typen.isEmpty()){
            cmbTyp.getSelectionModel().selectFirst();
        }

        DatePicker dpDatum = new DatePicker();

        TextField txtSeiteVon = new TextField(String.valueOf(markierteStartSeite));
        txtSeiteVon.setEditable(false);

        TextField txtSeiteBis = new TextField(String.valueOf(markierteEndeSeite));
        txtSeiteBis.setEditable(false);

        TextArea txtForschungsnotiz = new TextArea();
        txtForschungsnotiz.setPrefRowCount(4);
        txtForschungsnotiz.setWrapText(true);

        grid.add(new Label("Nro:"), 0, 0);
        grid.add(txtNro, 1, 0);

        grid.add(new Label("Titel:"), 0, 1);
        grid.add(txtTitel, 1, 1);

        grid.add(new Label("Typ:"), 0, 2);
        grid.add(cmbTyp, 1, 2);

        grid.add(new Label("Datum:"), 0, 3);
        grid.add(dpDatum, 1, 3);

        grid.add(new Label("Seite von:"), 0, 4);
        grid.add(txtSeiteVon, 1, 4);

        grid.add(new Label("Seite bis:"), 0, 5);
        grid.add(txtSeiteBis, 1, 5);

        grid.add(new Label("Forschungsnotiz:"), 0, 6);
        grid.add(txtForschungsnotiz, 1, 6);
        txtForschungsnotiz.setPrefHeight(100);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != speichernButtonType) {
            return;
        }

        String nro = txtNro.getText() != null ? txtNro.getText().trim() : "";
        String titel = txtTitel.getText() != null ? txtTitel.getText().trim() : "";

        String forschungsnotiz =
                txtForschungsnotiz.getText() != null
                        ? txtForschungsnotiz.getText().trim()
                        : "";

        if (titel.isEmpty()) {
            showAlert("Hinweis", "Bitte einen Titel eingeben.");
            return;
        }

        try {
            HeftEintrag eintrag = new HeftEintrag();

            // Diese Setter bitte ggf. an deine echte HeftEintrag-Klasse anpassen
            eintrag.setHeftID(aktuellesHeft.getHeftID());

            HeftEintragTyp typ = cmbTyp.getValue();

            int typID = typ != null
                    ? typ.getHeftEintragTypID()
                    : 1;

            eintrag.setHeftEintragTypID(typID);
            eintrag.setNro(nro);
            eintrag.setTitel(titel);
            eintrag.setDatum(dpDatum.getValue());
            eintrag.setSeiteVon(markierteStartSeite);
            eintrag.setSeiteBis(markierteEndeSeite);
            eintrag.setForschungsnotiz(forschungsnotiz);

            heftEintragRepository.insert(eintrag);

            List<HeftEintrag> liste =
                    heftEintragRepository.findByHeft(aktuellesHeft.getHeftID());

            aktuelleHeftEintragListe.clear();
            aktuelleHeftEintragListe.addAll(liste);

            tblHeftEintraege.getItems().setAll(liste);

            for (HeftEintrag h : liste) {
                if (h.getHeftEintragID() == eintrag.getHeftEintragID()) {
                    tblHeftEintraege.getSelectionModel().select(h);
                    tblHeftEintraege.scrollTo(h);
                    break;
                }
            }

            markierteStartSeite = null;
            markierteEndeSeite = null;
            updateSeitenmarkierung();

            updateOutputButtons();

            showAlert("Erfolg", "HeftEintrag wurde angelegt.");

        } catch (Exception ex) {
            ex.printStackTrace();
            showAlert("Fehler", "HeftEintrag konnte nicht gespeichert werden:\n" + ex.getMessage());
        }
    }

    private void oeffneHeftEintragDialog(HeftEintrag eintrag) {

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("HeftEintrag ändern");
        dialog.setHeaderText("HeftEintrag bearbeiten");

        ButtonType speichernButtonType =
                new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);

        ButtonType abbrechenButtonType =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
                speichernButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setStyle("-fx-padding: 10;");

        TextField txtNro = new TextField(eintrag.getNro());
        TextField txtTitel = new TextField(eintrag.getTitel());

        ComboBox<HeftEintragTyp> cmbTyp = new ComboBox<>();

        List<HeftEintragTyp> typen =
                heftEintragRepository.findAllTypen();

        cmbTyp.getItems().addAll(typen);

        for(HeftEintragTyp typ : typen){

            if(typ.getHeftEintragTypID()
                    == eintrag.getHeftEintragTypID()){

                cmbTyp.getSelectionModel().select(typ);
                break;
            }
        }

        DatePicker dpDatum =
                new DatePicker(eintrag.getDatum());

        TextField txtSeiteVon =
                new TextField(String.valueOf(eintrag.getSeiteVon()));

        TextField txtSeiteBis =
                new TextField(String.valueOf(eintrag.getSeiteBis()));

        txtSeiteVon.setEditable(true);
        txtSeiteBis.setEditable(true);

        TextArea txtForschungsnotiz = new TextArea();
        txtForschungsnotiz.setPrefRowCount(4);
        txtForschungsnotiz.setWrapText(true);
        txtForschungsnotiz.setPrefHeight(100);

        if (eintrag.getForschungsnotiz() != null) {
            txtForschungsnotiz.setText(eintrag.getForschungsnotiz());
        }

        grid.add(new Label("Nro:"),0,0);
        grid.add(txtNro,1,0);

        grid.add(new Label("Titel:"),0,1);
        grid.add(txtTitel,1,1);

        grid.add(new Label("Typ:"),0,2);
        grid.add(cmbTyp,1,2);

        grid.add(new Label("Datum:"),0,3);
        grid.add(dpDatum,1,3);

        grid.add(new Label("Seite von:"),0,4);
        grid.add(txtSeiteVon,1,4);

        grid.add(new Label("Seite bis:"),0,5);
        grid.add(txtSeiteBis,1,5);

        grid.add(new Label("Forschungsnotiz:"), 0, 6);
        grid.add(txtForschungsnotiz, 1, 6);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != speichernButtonType) {
            return;
        }

        String nro = txtNro.getText().trim();
        String titel = txtTitel.getText().trim();

        String seiteVonText = txtSeiteVon.getText().trim();
        String seiteBisText = txtSeiteBis.getText().trim();

        if (titel.isEmpty()) {
            showAlert("Hinweis","Titel darf nicht leer sein.");
            return;
        }

        if (seiteVonText.isEmpty() || seiteBisText.isEmpty()) {
            showAlert("Hinweis", "Seite von und Seite bis dürfen nicht leer sein.");
            return;
        }

        int seiteVonNeu;
        int seiteBisNeu;

        try {
            seiteVonNeu = Integer.parseInt(seiteVonText);
            seiteBisNeu = Integer.parseInt(seiteBisText);
        } catch (NumberFormatException ex) {
            showAlert("Hinweis", "Seite von und Seite bis müssen Zahlen sein.");
            return;
        }

        if(seiteBisNeu < seiteVonNeu){

            showAlert(
                    "Hinweis",
                    "Seite bis darf nicht kleiner als Seite von sein."
            );

            return;
        }

        HeftEintragTyp typ = cmbTyp.getValue();

        int typID = typ != null
                ? typ.getHeftEintragTypID()
                : 1;

        eintrag.setNro(nro);
        eintrag.setTitel(titel);
        eintrag.setDatum(dpDatum.getValue());
        eintrag.setForschungsnotiz(txtForschungsnotiz.getText());
        eintrag.setHeftEintragTypID(typID);

        eintrag.setSeiteVon(seiteVonNeu);
        eintrag.setSeiteBis(seiteBisNeu);

        updateHeftEintrag(eintrag);

    }

    private void updateHeftEintrag(HeftEintrag eintrag){

        try{

            heftEintragRepository.update(eintrag);

            Heft heft =
                    heftListView.getSelectionModel().getSelectedItem();

            List<HeftEintrag> liste =
                    heftEintragRepository.findByHeft(heft.getHeftID());

            tblHeftEintraege.getItems().setAll(liste);

            for(HeftEintrag h : liste){

                if(h.getHeftEintragID() ==
                        eintrag.getHeftEintragID()){

                    tblHeftEintraege
                            .getSelectionModel()
                            .select(h);

                    break;
                }
            }

            showAlert("Erfolg",
                    "HeftEintrag wurde geändert.");

        }
        catch(Exception e){

            e.printStackTrace();

            showAlert("Fehler",
                    "HeftEintrag konnte nicht geändert werden.");
        }
    }

    private void oeffneHeftDialog() {
        oeffneHeftDialog(null);
    }

       private void oeffneHeftDialog(HeftEingabe eingabe) {

        boolean istAendern = eingabe != null;

        if (!istAendern && (markierteStartSeite == null || markierteEndeSeite == null)) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Keine Seitenmarkierung");
            alert.setHeaderText(null);
            alert.setContentText("Bitte zuerst einen Seitenbereich markieren.");
            alert.showAndWait();
            return;
        }

        Dialog<HeftEingabe> dialog = new Dialog<>();

        if (istAendern) {
            dialog.setTitle("Heft ändern");
            dialog.setHeaderText("Vorhandenes Heft bearbeiten");
        } else {
            dialog.setTitle("Heft erfassen");
            dialog.setHeaderText("Heft aus markiertem Seitenbereich anlegen (" +
                    markierteStartSeite + " bis " + markierteEndeSeite + ")");
        }

        ButtonType btnOKType     = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnCancelType = ButtonType.CANCEL;

        dialog.getDialogPane().getButtonTypes().addAll(btnOKType, btnCancelType);

        // Buttons als Button holen (wichtig für setDefaultButton / setCancelButton)
        Button btnOk     = (Button) dialog.getDialogPane().lookupButton(btnOKType);
        Button btnCancel = (Button) dialog.getDialogPane().lookupButton(btnCancelType);

        // Default- & Cancel-Verhalten abschalten → Enter triggert NICHT mehr automatisch OK/Cancel
        btnOk.setDefaultButton(false);
        btnCancel.setCancelButton(false);

        btnOk.setDisable(true);  // anfangs deaktiviert

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        TextField txtHeftNummer = new TextField(eingabe != null ? eingabe.nummer() : "");
        txtHeftNummer.setPromptText("z. B. I, IV, XII");

        DatePicker dpDatum = new DatePicker(eingabe != null ? eingabe.datum() : null);
        dpDatum.setPromptText("Ausgabedatum");

        TextField txtOrt = new TextField(eingabe != null ? eingabe.ort() : "");
        txtOrt.setPromptText("z. B. Berlin, Wien, ...");

        String initialSeiteVon = "";
        String initialSeiteBis = "";

        if (eingabe != null) {
            if (eingabe.seiteVon() != null) {
                initialSeiteVon = String.valueOf(eingabe.seiteVon());
            }

            if (eingabe.seiteBis() != null) {
                initialSeiteBis = String.valueOf(eingabe.seiteBis());
            }
        }
        else if (markierteStartSeite != null && markierteEndeSeite != null) {
            initialSeiteVon = String.valueOf(markierteStartSeite);
            initialSeiteBis = String.valueOf(markierteEndeSeite);
        }

        TextField txtSeiteVon = new TextField(initialSeiteVon);
        TextField txtSeiteBis = new TextField(initialSeiteBis);

        txtSeiteVon.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        txtSeiteBis.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        txtSeiteVon.setOnAction(e -> txtSeiteBis.requestFocus());

        txtSeiteBis.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                btnOk.requestFocus();
                event.consume();
            }
        });

        txtSeiteVon.setPrefWidth(80);
        txtSeiteBis.setPrefWidth(80);

        btnOk.addEventFilter(ActionEvent.ACTION, event -> {
            String nummer = txtHeftNummer.getText().trim();
            String seiteVonText = txtSeiteVon.getText().trim();
            String seiteBisText = txtSeiteBis.getText().trim();

            if (nummer.isEmpty()) {
                showAlert("Fehler", "Bitte eine Heftnummer eingeben.");
                event.consume();
                return;
            }

            if (seiteVonText.isEmpty() || seiteBisText.isEmpty()) {
                showAlert("Fehler", "Bitte Seite von und Seite bis eingeben.");
                event.consume();
                return;
            }

            try {
                int seiteVon = Integer.parseInt(seiteVonText);
                int seiteBis = Integer.parseInt(seiteBisText);

                if (seiteBis < seiteVon) {
                    showAlert("Fehler", "Seite bis darf nicht kleiner als Seite von sein.");
                    event.consume();
                }

                int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

                Integer aktuelleHeftId = null;

                if (istAendern) {
                    Heft aktuellesHeft = heftListView.getSelectionModel().getSelectedItem();
                    if (aktuellesHeft != null) {
                        aktuelleHeftId = aktuellesHeft.getHeftID();
                    }
                }

                if (hatUeberschneidungMitVorhandenemHeft(
                        bandId,
                        seiteVon,
                        seiteBis,
                        aktuelleHeftId
                )) {
                    showAlert("Fehler", "Der Seitenbereich überschneidet sich mit einem vorhandenen Heft.");
                    event.consume();
                }

                List<File> bilder = loadBilder(aktuellesGebiet, aktuellesBand);
                int maxSeiten = bilder.size();

                if (seiteVon < 1 || seiteBis > maxSeiten) {
                    showAlert("Fehler",
                            "Der Seitenbereich muss zwischen 1 und " + maxSeiten + " liegen.");
                    event.consume();
                    return;
                }

            } catch (NumberFormatException ex) {
                showAlert("Fehler", "Seite von und Seite bis müssen Zahlen sein.");
                event.consume();
            }
        });

        // Live-Validierung: OK-Button nur aktiv wenn Heftnummer gefüllt
        txtHeftNummer.textProperty().addListener((obs, old, neu) ->
                btnOk.setDisable(neu == null || neu.trim().isEmpty()));
        btnOk.setDisable(txtHeftNummer.getText() == null || txtHeftNummer.getText().trim().isEmpty());

        // Enter in Heftnummer → zu Datum
        txtHeftNummer.setOnAction(e -> dpDatum.requestFocus());

        // Enter / Return im DatePicker-Editor → zu Ort (wichtig: EventFilter!)
        TextField dateEditor = (TextField) dpDatum.getEditor();
        dateEditor.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtOrt.requestFocus();
                event.consume();
            }
        });

        txtOrt.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                txtSeiteVon.requestFocus();
                event.consume();
            }
        });

        grid.add(new Label("Heftnummer (römisch):"), 0, 0);
        grid.add(txtHeftNummer, 1, 0);

        grid.add(new Label("Datum:"), 0, 1);
        grid.add(dpDatum, 1, 1);

        grid.add(new Label("Ort:"), 0, 2);
        grid.add(txtOrt, 1, 2);

        grid.add(new Label("Seite von:"), 0, 3);
        grid.add(txtSeiteVon, 1, 3);

        grid.add(new Label("Seite bis:"), 0, 4);
        grid.add(txtSeiteBis, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // ResultConverter für Klick auf OK-Button
        dialog.setResultConverter(dialogBtn -> {
            if (dialogBtn == btnOKType) {
                String nummer = txtHeftNummer.getText().trim();
                LocalDate datum = dpDatum.getValue();
                String ort = txtOrt.getText().trim();

                if (nummer.isEmpty()) return null;

                if (txtSeiteVon.getText().trim().isEmpty() || txtSeiteBis.getText().trim().isEmpty()) {
                    showAlert("Fehler", "Seite von und Seite bis müssen angegeben werden.");
                    return null;
                }

                Integer seiteVon = Integer.parseInt(txtSeiteVon.getText().trim());
                Integer seiteBis = Integer.parseInt(txtSeiteBis.getText().trim());

                return new HeftEingabe(nummer, datum, ort, seiteVon, seiteBis);
            }
            return null;
        });

        Platform.runLater(() -> txtHeftNummer.requestFocus());

        dialog.showAndWait().ifPresent(result -> {

            if (eingabe == null) {
                speichereHeft(result);
            } else {
                updateHeft(result);
            }

        });
    }

    private record HeftEingabe(String nummer, LocalDate datum, String ort, Integer seiteVon, Integer seiteBis) {
        @Override
        public String toString() {
            return "Heft " + nummer + " | " + datum + " | " + ort;
        }
    }

    private void speichereHeft(HeftEingabe eingabe) {

        if (eingabe == null) {
            return;
        }

        Heft heft = new Heft();

        int bandId = ermittleBandId(aktuellesGebiet, aktuellesBand);

        if (bandId <= 0) {
            showAlert("Fehler", "BandID konnte nicht ermittelt werden.");
            return;
        }

        heft.setBandID(bandId);
        heft.setHeftNummer(eingabe.nummer());
        heft.setAusgabeDatum(eingabe.datum());
        heft.setOrt(eingabe.ort());
        heft.setSeiteVon(eingabe.seiteVon());
        heft.setSeiteBis(eingabe.seiteBis());

        heft.setIstAktiv(true);
        heft.setSortierung(0);

        try {
            if (hatUeberschneidungMitVorhandenemHeft(
                    heft.getBandID(),
                    heft.getSeiteVon(),
                    heft.getSeiteBis(),
                    null
            )) {
                showAlert("Fehler", "Der Seitenbereich überschneidet sich mit einem vorhandenen Heft.");
                return;
            }

            heftRepository.insert(heft);

            int bandIdReload = ermittleBandId(aktuellesGebiet, aktuellesBand);

            heftListView.getItems().setAll(
                    heftRepository.findByBand(bandIdReload)
            );
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateHeft(HeftEingabe eingabe) {

        Heft heft = heftListView.getSelectionModel().getSelectedItem();

        if (heft == null) {
            return;
        }

        if (eingabe.seiteVon() > eingabe.seiteBis()) {
            showAlert("Fehler", "Seite bis darf nicht kleiner als Seite von sein.");
            return;
        }

        heft.setHeftNummer(eingabe.nummer());
        heft.setAusgabeDatum(eingabe.datum());
        heft.setOrt(eingabe.ort());

        heft.setSeiteVon(eingabe.seiteVon());
        heft.setSeiteBis(eingabe.seiteBis());

        try {
            if (hatUeberschneidungMitVorhandenemHeft(
                    heft.getBandID(),
                    heft.getSeiteVon(),
                    heft.getSeiteBis(),
                    heft.getHeftID()
            )) {
                showAlert("Fehler", "Der Seitenbereich überschneidet sich mit einem vorhandenen Heft.");
                return;
            }

            heftRepository.update(heft);

            List<Heft> liste = heftRepository.findByBand(heft.getBandID());
            heftListView.getItems().setAll(liste);

            for (Heft h : liste) {
                if (h.getHeftID() == heft.getHeftID()) {
                    heftListView.getSelectionModel().select(h);
                    break;
                }
            }

            showAlert("Erfolg", "Heft wurde geändert.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fehler", "Heft konnte nicht geändert werden.");
        }
    }

    private void heftAendern() {

        Heft heft = heftListView.getSelectionModel().getSelectedItem();

        if (heft == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Kein Heft gewählt");
            alert.setHeaderText(null);
            alert.setContentText("Bitte zuerst ein Heft auswählen.");
            alert.showAndWait();
            return;
        }
        HeftEingabe eingabe = new HeftEingabe(
                heft.getHeftNummer(),
                heft.getAusgabeDatum(),
                heft.getOrt(),
                heft.getSeiteVon(),
                heft.getSeiteBis()
        );

        oeffneHeftDialog(eingabe);
    }

    private void heftEintragAendern() {

        HeftEintrag eintrag =
                tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (eintrag == null) {
            showAlert("Hinweis", "Bitte zuerst einen HeftEintrag auswählen.");
            return;
        }

        oeffneHeftEintragDialog(eintrag);
    }

    private void zeigeSeitenstandLabel() {
        if (txtSeiteDirekt != null) {
            txtSeiteDirekt.setVisible(false);
            txtSeiteDirekt.setManaged(false);
        }

        if (lblSeitenstand != null) {
            lblSeitenstand.setVisible(true);
            lblSeitenstand.setManaged(true);
        }
    }

    private int getAktiverBereichVon() {

        InhaltTabellenEintrag inhalt =
                lstInhalteDetail.getSelectionModel().getSelectedItem();

        if (inhalt != null && inhalt.getSeiteVon() > 0) {
            return inhalt.getSeiteVon();
        }

        HeftEintrag eintrag = tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (eintrag != null) {
            return eintrag.getSeiteVon();
        }

        Heft heft = heftListView.getSelectionModel().getSelectedItem();

        if (heft != null && heft.getSeiteVon() > 0) {
            return heft.getSeiteVon();
        }

        return 1;
    }

    private int getAktiverBereichBis() {

        InhaltTabellenEintrag inhalt =
                lstInhalteDetail.getSelectionModel().getSelectedItem();

        if (inhalt != null && inhalt.getSeiteBis() > 0) {
            return inhalt.getSeiteBis();
        }

        HeftEintrag eintrag = tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (eintrag != null && eintrag.getSeiteBis() > 0) {
            return eintrag.getSeiteBis();
        }

        Heft heft = heftListView.getSelectionModel().getSelectedItem();

        if (heft != null && heft.getSeiteBis() > 0) {
            return heft.getSeiteBis();
        }

        return aktuelleBildliste.size();

    }

    private boolean hatUeberschneidungMitVorhandenemHeft(int bandId, int seiteVon, int seiteBis, Integer aktuelleHeftId) {
        List<Heft> vorhandeneHefte = heftRepository.findByBand(bandId);

        for (Heft vorhandenesHeft : vorhandeneHefte) {

            if (aktuelleHeftId != null && vorhandenesHeft.getHeftID() == aktuelleHeftId) {
                continue;
            }

            if (seiteVon <= vorhandenesHeft.getSeiteBis() && seiteBis >= vorhandenesHeft.getSeiteVon()) {
                return true;
            }
        }

        return false;
    }

    private void oeffneBandJahrDialog() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Band/Jahr anlegen");
        dialog.setHeaderText("Neue Stammdaten für Band/Jahr erfassen");

        ButtonType speichernButtonType =
                new ButtonType("Speichern", ButtonBar.ButtonData.OK_DONE);
        ButtonType abbrechenButtonType =
                new ButtonType("Abbrechen", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(
                speichernButtonType,
                abbrechenButtonType
        );

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> cmbGebiet = new ComboBox<>();
        cmbGebiet.getItems().addAll(loadGebiete());
        cmbGebiet.setPrefWidth(220);

        TextField txtJahr = new TextField();
        txtJahr.setPromptText("z. B. 1846");

        txtJahr.setTextFormatter(new TextFormatter<>(change ->
                change.getControlNewText().matches("\\d*") ? change : null));

        grid.add(new Label("Gebiet:"), 0, 0);
        grid.add(cmbGebiet, 1, 0);

        grid.add(new Label("Jahr/Band:"), 0, 1);
        grid.add(txtJahr, 1, 1);

        dialog.getDialogPane().setContent(grid);

        Button btnSpeichern =
                (Button) dialog.getDialogPane().lookupButton(speichernButtonType);

        btnSpeichern.addEventFilter(ActionEvent.ACTION, event -> {
            String gebiet = cmbGebiet.getValue();
            String jahr = txtJahr.getText().trim();

            if (gebiet == null || gebiet.isBlank()) {
                showAlert("Fehler", "Bitte ein Gebiet auswählen.");
                event.consume();
                return;
            }

            if (jahr.isBlank()) {
                showAlert("Fehler", "Bitte ein Jahr/Band eingeben.");
                event.consume();
                return;
            }

            int jahrInt = Integer.parseInt(jahr);

            if (quelleRepository.bandExistiert(gebiet, jahrInt)) {
                showAlert("Fehler", "Dieses Band/Jahr existiert bereits.");
                event.consume();
            }
        });

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != speichernButtonType) {
            return;
        }

        String gebiet = cmbGebiet.getValue();
        String jahr = txtJahr.getText().trim();

        try {
            int jahrInt = Integer.parseInt(jahr);

            if (quelleRepository.bandExistiert(gebiet, jahrInt)) {
                showAlert("Fehler", "Dieses Band/Jahr existiert bereits.");
                return;
            }

            quelleRepository.insertBand(gebiet, jahrInt);

            int bandId = ermittleBandId(gebiet, String.valueOf(jahrInt));

            File rootDir = new File(Config.getImageRootPath());
            File gebietDir = new File(rootDir, gebiet);
            File bandDir = new File(gebietDir, String.valueOf(jahrInt));

            if (!gebietDir.exists()) {
                try {
                    quelleRepository.deleteBand(bandId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                showAlert("Fehler", "Gebietsordner existiert nicht.");
                return;
            }

            if (!bandDir.exists() && !bandDir.mkdir()) {
                try {
                    quelleRepository.deleteBand(bandId);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                showAlert("Fehler", "Band/Jahr-Ordner konnte nicht angelegt werden.");
                return;
            }

            invalidateGebieteCache();

            // Bandliste neu laden
            gebietListView.getSelectionModel().select(gebiet);

            List<String> baende = loadBaende(gebiet);
            bandListView.getItems().setAll(baende);
            updateStatusLabel(baende.size());

            bandListView.getSelectionModel().select(String.valueOf(jahrInt));
            bandListView.scrollTo(String.valueOf(jahrInt));
            bandListView.requestFocus();

            showAlert("Erfolg", "Band/Jahr wurde angelegt.");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fehler", "Band/Jahr konnte nicht angelegt werden.");
        }
    }
}