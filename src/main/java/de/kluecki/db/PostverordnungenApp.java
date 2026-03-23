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
import de.kluecki.db.UI.VerordnungsSucheDialog;
import de.kluecki.db.model.HeftEintrag;
import de.kluecki.db.model.VerordnungBetreff;
import de.kluecki.db.print.PrintPdfService;
import de.kluecki.db.repository.HeftEintragRepository;
import de.kluecki.db.repository.QuelleRepository;
import de.kluecki.db.repository.VerordnungBetreffRepository;
import javafx.application.Application;
import javafx.geometry.Insets;
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
import java.time.LocalDate;
import java.util.*;
import javafx.beans.property.SimpleStringProperty;
import de.kluecki.db.model.Heft;
import de.kluecki.db.repository.HeftRepository;
import javafx.scene.layout.GridPane;
import javafx.scene.control.DatePicker;
import de.kluecki.db.InhaltTabellenEintrag;
import de.kluecki.db.repository.InhaltseinheitRepository;

public class PostverordnungenApp extends Application {

    // Konfiguration
    private static final String ROOT_PATH = "D:\\Postgeschichte_PC\\Postverordnungen";

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

    // Aktuelle fachliche Auswahl (Navigation)
    private String aktuellesGebiet;
    private String aktuellesBand;
    private Integer markierteStartSeite = null;
    private Integer markierteEndeSeite = null;

    // Tabellen / Detailansichten
    // Neue Zielstruktur
    private TableView<HeftEintrag> tblHeftEintraege; // neue Zielstruktur

    private ListView<InhaltTabellenEintrag> lstInhalteDetail;

    // Repository Zugriff (Datenbank)
    private VerordnungBetreffRepository betreffRepository; // Alt / Übergang
    private HeftEintragRepository heftEintragRepository; // neue Struktur
    private HeftRepository heftRepository;
    private QuelleRepository quelleRepository;  // Basisstruktur



    @Override
    public void start(Stage stage) {

        try {
            betreffRepository = new VerordnungBetreffRepository(DatabaseConnection.getConnection());
            heftEintragRepository = new HeftEintragRepository(DatabaseConnection.getConnection());
            heftRepository = new HeftRepository(DatabaseConnection.getConnection());

            //var liste = heftEintragRepository.findByHeft(1);
            //System.out.println("HeftEinträge: " + liste.size());
            //for (HeftEintrag he : liste) {
            //    System.out.println(he.getHeftEintragID() + " | " + he.getTitel() + " | " + he.getNro());
            //}

        } catch (Exception e) {
            e.printStackTrace();
        }

        quelleRepository = new QuelleRepository();

        BorderPane root = new BorderPane();

        root.setTop(createMenuBar());
        root.setBottom(createStatusBar());

        gebietListView = new ListView<>();
        gebietListView.getItems().addAll(loadGebiete());

        bandListView = new ListView<>();
        heftListView = new ListView<>();

        HBox imageToolbar = createImageToolbar();
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
        Menu menuHilfe = new Menu("Hilfe");
        MenuItem menuSucheVerordnungen = new MenuItem("Verordnungen suchen");
        menuBearbeiten.getItems().add(menuSucheVerordnungen);

        menuSucheVerordnungen.setOnAction(e -> {

         VerordnungsSucheDialog dialog =
                 new VerordnungsSucheDialog(
                            betreffRepository,
                            loadGebiete(),
                            selected -> springeZuVerordnung(selected)
                 );

         dialog.open();
        });

        menuBar.getMenus().addAll(menuDatei, menuBearbeiten, menuAnsicht, menuHilfe);
        return menuBar;
    }

    private HBox createStatusBar() {
        Label statusLabel = new Label("0 Quellen geladen");
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
        colNro.setPrefWidth(70);
        colNro.setSortable(false);

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

        table.getColumns().addAll(colNro, colTitel, colDatum, colSeite);

        return table;
    }

    private VBox createNavigationPane() {
        Label lblGebiete = new Label("Gebiete");
        Label lblBaende = new Label("Jahr / Band");

        Label lblHefte = new Label("Hefte");
        lblHefte.setStyle("-fx-font-weight: bold;");

        Label lblHeftEintraege = new Label("HeftEinträge");
        lblHeftEintraege.setStyle("-fx-font-weight: bold;");

        Label lblBetreffe = new Label("Inhalte des HeftEintrags");
        lblBetreffe.setStyle("-fx-font-weight: bold;");

        Button btnInhaltseinheiten = new Button("Inhaltseinträge");
        btnInhaltseinheiten.setMaxWidth(Double.MAX_VALUE);

        btnInhaltseinheiten.setOnAction(e -> {
            HeftEintrag heftEintrag = tblHeftEintraege.getSelectionModel().getSelectedItem();

            if (heftEintrag == null) {
                showAlert("Hinweis", "Bitte zuerst einen HeftEintrag auswählen.");
                return;
            }

            InhaltseinheitenWindow.open(heftEintrag);
        });

        VBox navigation = new VBox(8,
                lblGebiete,
                gebietListView,
                lblBaende,
                bandListView,
                lblHefte,
                heftListView,
                lblHeftEintraege,
                tblHeftEintraege,
                lblBetreffe,
                lstInhalteDetail,
                btnInhaltseinheiten
        );

        navigation.setPrefWidth(300);
        navigation.setMinWidth(260);
        navigation.setMaxWidth(360);
        navigation.setStyle("""
            -fx-padding: 8;
            -fx-background-color: #f4f4f4;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 0 1 0 0;
        """);

        gebietListView.setPrefHeight(150);
        bandListView.setPrefHeight(250);
        heftListView.setPrefHeight(120);

        VBox.setVgrow(lstInhalteDetail, Priority.NEVER);

        tblHeftEintraege.setOnMouseClicked(e -> {

            HeftEintrag eintrag =
                    tblHeftEintraege.getSelectionModel().getSelectedItem();

            if (eintrag == null) return;

            if (e.getClickCount() == 1) {

                aktuellerBildIndex = eintrag.getSeiteVon() - 1;
                ladeAktuellesBild();
                ladeInhalteZuHeftEintrag(eintrag);
            }
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

    private BorderPane createDocumentPane(HBox imageToolbar, HBox bildNavigation) {
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

    private VBox createDocumentHeader(HBox imageToolbar) {
        VBox documentHeader = new VBox(6, lblBandTitel, lblSeitenmarkierung, lblAktuellerInhalt, imageToolbar);
        documentHeader.setStyle("""
            -fx-padding: 8;
            -fx-background-color: #f8f8f8;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 0 0 1 0;
        """);
        return documentHeader;
    }

    private HBox createImageToolbar() {
        Button btnZoomIn = new Button("+");
        Button btnZoomOut = new Button("-");
        Button btnFit = new Button("Anpassen");
        Button btnRotateLeft = new Button("⟲");
        Button btnRotateRight = new Button("⟳");

        Button btnInhaltPdf = new Button("Inhalt PDF");
        btnInhaltPdf.setTooltip(new Tooltip("Ausgewählten Inhalt als PDF speichern"));
        Button btnInhaltPrint = new Button("Inhalt drucken");
        btnInhaltPrint.setTooltip(new Tooltip("Ausgewählten Inhalt drucken"));

        Button btnStartSeitenmarkierung = new Button("Startseite markieren");
        Button btnEndeSeitenmarkierung = new Button("Endseite markieren");
        Button btnHeftErfassen = new Button("Heft erfassen");
        btnHeftErfassen.setOnAction(e -> oeffneHeftDialog());

        Button btnHeftAendern = new Button("Heft ändern");
        btnHeftAendern.setOnAction(e -> heftAendern());

        Button btnHeftEintragErfassen = new Button("HeftEintrag erfassen");
        btnHeftEintragErfassen.setOnAction(e -> oeffneHeftEintragDialog());
        Button btnHeftEintragAendern = new Button("HeftEintrag ändern");
        btnHeftEintragAendern.setOnAction(e -> heftEintragAendern());

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
            updateSeitenmarkierung();;
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

            updateSeitenmarkierung();;
        });
        btnInhaltPdf.setOnAction(e -> exportSelectedInhaltToPdf());
        btnInhaltPrint.setOnAction(e -> printSelectedInhalt());

        HBox imageToolbar = new HBox(
                5,

                // Bildsteuerung
                btnZoomIn,
                btnZoomOut,
                btnFit,
                btnRotateLeft,
                btnRotateRight,

                new Separator(),

                // Seitenmarkierung
                btnStartSeitenmarkierung,
                btnEndeSeitenmarkierung,

                new Separator(),

                // Heft
                btnHeftErfassen,
                btnHeftAendern,

                new Separator(),

                // Heftinhalt
                btnHeftEintragErfassen,
                btnHeftEintragAendern,

                new Separator(),

                // Ausgabe
                btnInhaltPrint,
                btnInhaltPdf
        );

        imageToolbar.setPadding(new Insets(5));

        imageToolbar.setStyle("""
            -fx-padding: 5;
            -fx-background-color: #f0f0f0;
            -fx-border-color: #d0d0d0;
        """);

        return imageToolbar;
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
            try {
                int seite = Integer.parseInt(txtSeiteDirekt.getText().trim());
                if (seite >= 1 && seite <= aktuelleBildliste.size()) {
                    aktuellerBildIndex = seite - 1;
                    ladeAktuellesBild();
                }
            } catch (NumberFormatException ex) {
                // ungültige Eingabe ignorieren
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
            if (newValue == null) return;
            bandListView.getItems().setAll(loadBaende(newValue));
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
                heftListView.getItems().setAll(heftRepository.findByBand(bandId));
            }

            List<File> bilder = loadBilder(gebiet, band);

            aktuelleBildliste.clear();
            aktuellerBildIndex = -1;

            if (!bilder.isEmpty()) {
                for (File bild : bilder) {
                    aktuelleBildliste.add(bild.toPath());
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
            if (newValue == null) return;

            Integer seiteVon = newValue.getSeiteVon();

            if (seiteVon > 0 && seiteVon <= aktuelleBildliste.size()) {
                aktuellerBildIndex = seiteVon - 1;
                ladeAktuellesBild();
            }

            tblHeftEintraege.getItems().clear();
            lstInhalteDetail.getItems().clear();

            try {
                tblHeftEintraege.getItems().setAll(
                        heftEintragRepository.findByHeft(newValue.getHeftID())
                );
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private int ermittleBandId(String gebiet, String band) {
        System.out.println("ermittleBandId -> Gebiet: [" + gebiet + "], Band: [" + band + "]");
        int bandId = quelleRepository.findBandId(gebiet, band);
        System.out.println("ermittleBandId -> Ergebnis BandID: " + bandId);
        return bandId;
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

        VerordnungBetreff betreff = null;

        try {
            if (betreffRepository != null && aktuellesGebiet != null && aktuellesBand != null && aktuellerBildIndex >= 0) {
                betreff = betreffRepository.findForPage(
                        aktuellesGebiet,
                        aktuellesBand,
                        aktuellerBildIndex + 1
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

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

    private void showAlert(String titel, String text) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titel);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
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
        if (aktuelleBildliste.isEmpty()) return;
        if (aktuellerBildIndex < 0 || aktuellerBildIndex >= aktuelleBildliste.size()) return;

        Path bildPfad = aktuelleBildliste.get(aktuellerBildIndex);
        currentImage = new Image(bildPfad.toUri().toString());
        imageView.setImage(currentImage);

        updateImageView();
        updateNavigationState();
        updateInhaltAnzeige(aktuellerBildIndex + 1);
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
                lblSeitenstand.setText((aktuellerBildIndex + 1) + " / " + aktuelleBildliste.size());
            } else {
                lblSeitenstand.setText("0 / 0");
            }
        }
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
        File rootDir = new File(ROOT_PATH);

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

    private List<String> loadBaende(String gebiet) {
        File gebietDir = new File(ROOT_PATH, gebiet);

        if (!gebietDir.exists() || !gebietDir.isDirectory()) {
            return List.of();
        }

        File[] dirs = gebietDir.listFiles(File::isDirectory);
        if (dirs == null) {
            return List.of();
        }

        return Arrays.stream(dirs)
                .map(File::getName)
                .sorted()
                .toList();
    }

    private List<File> loadBilder(String gebiet, String band) {
        File bandDir = new File(new File(ROOT_PATH, gebiet), band);

        if (!bandDir.exists() || !bandDir.isDirectory()) {
            return List.of();
        }

        File[] files = bandDir.listFiles(file ->
                file.isFile() && (
                        file.getName().toLowerCase().endsWith(".jpg") ||
                                file.getName().toLowerCase().endsWith(".jpeg") ||
                                file.getName().toLowerCase().endsWith(".png") ||
                                file.getName().toLowerCase().endsWith(".gif")
                )
        );

        if (files == null) {
            return List.of();
        }

        return Arrays.stream(files)
                .sorted(Comparator.comparing(File::getName))
                .toList();
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
                ROOT_PATH,
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

    public static void main(String[] args) {
        launch(args);
    }

    private void updateInhaltAnzeige(String text) {
        lblAktuellerInhalt.setText(text);
    }

    private void updateInhaltAnzeige(int aktuelleSeite) {

        try {

            VerordnungBetreff betreff =
                    betreffRepository.findForPage(aktuellesGebiet, aktuellesBand, aktuelleSeite);

            if (betreff != null) {

                lblAktuellerInhalt.setText(
                        "Inhalt: " + betreff.getTitel() +
                                " (S. " + betreff.getSeiteVon() +
                                "–" + betreff.getSeiteBis() + ")"
                );

            } else {

                lblAktuellerInhalt.setText("Kein Inhaltseintrag auf dieser Seite");
                lstInhalteDetail.getSelectionModel().clearSelection();

            }

        } catch (Exception e) {
            e.printStackTrace();
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

    private void springeZuVerordnung(VerordnungBetreff betreff) {

        try {

            String gebiet = betreff.getGebiet();
            String band = betreff.getBandJahr();

            gebietListView.getSelectionModel().select(gebiet);

            bandListView.getSelectionModel().select(band);

            int seite = betreff.getSeiteVon();

            if (seite > 0 && seite <= aktuelleBildliste.size()) {

                aktuellerBildIndex = seite - 1;

                ladeAktuellesBild();

            }

        }
        catch (Exception e) {
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

        DatePicker dpDatum = new DatePicker();

        TextField txtSeiteVon = new TextField(String.valueOf(markierteStartSeite));
        txtSeiteVon.setEditable(false);

        TextField txtSeiteBis = new TextField(String.valueOf(markierteEndeSeite));
        txtSeiteBis.setEditable(false);

        grid.add(new Label("Nro:"), 0, 0);
        grid.add(txtNro, 1, 0);

        grid.add(new Label("Titel:"), 0, 1);
        grid.add(txtTitel, 1, 1);

        grid.add(new Label("Datum:"), 0, 2);
        grid.add(dpDatum, 1, 2);

        grid.add(new Label("Seite von:"), 0, 3);
        grid.add(txtSeiteVon, 1, 3);

        grid.add(new Label("Seite bis:"), 0, 4);
        grid.add(txtSeiteBis, 1, 4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != speichernButtonType) {
            return;
        }

        String nro = txtNro.getText() != null ? txtNro.getText().trim() : "";
        String titel = txtTitel.getText() != null ? txtTitel.getText().trim() : "";

        if (titel.isEmpty()) {
            showAlert("Hinweis", "Bitte einen Titel eingeben.");
            return;
        }

        try {
            HeftEintrag eintrag = new HeftEintrag();

            // Diese Setter bitte ggf. an deine echte HeftEintrag-Klasse anpassen
            eintrag.setHeftID(aktuellesHeft.getHeftID());
            eintrag.setHeftEintragTypID(1);
            eintrag.setNro(nro);
            eintrag.setTitel(titel);
            eintrag.setDatum(dpDatum.getValue());
            eintrag.setSeiteVon(markierteStartSeite);
            eintrag.setSeiteBis(markierteEndeSeite);

            // Diesen Methoden-Namen bitte ggf. an dein Repository anpassen
            heftEintragRepository.insert(eintrag);

            tblHeftEintraege.getItems().setAll(
                    heftEintragRepository.findByHeft(aktuellesHeft.getHeftID())
            );

            tblHeftEintraege.getSelectionModel().select(eintrag);
            lstInhalteDetail.getItems().clear();

            markierteStartSeite = null;
            markierteEndeSeite = null;
            updateSeitenmarkierung();;

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

        DatePicker dpDatum =
                new DatePicker(eintrag.getDatum());

        TextField txtSeiteVon =
                new TextField(String.valueOf(eintrag.getSeiteVon()));

        TextField txtSeiteBis =
                new TextField(String.valueOf(eintrag.getSeiteBis()));

        txtSeiteVon.setEditable(false);
        txtSeiteBis.setEditable(false);

        grid.add(new Label("Nro:"),0,0);
        grid.add(txtNro,1,0);

        grid.add(new Label("Titel:"),0,1);
        grid.add(txtTitel,1,1);

        grid.add(new Label("Datum:"),0,2);
        grid.add(dpDatum,1,2);

        grid.add(new Label("Seite von:"),0,3);
        grid.add(txtSeiteVon,1,3);

        grid.add(new Label("Seite bis:"),0,4);
        grid.add(txtSeiteBis,1,4);

        dialog.getDialogPane().setContent(grid);

        Optional<ButtonType> result = dialog.showAndWait();

        if (result.isEmpty() || result.get() != speichernButtonType) {
            return;
        }

        String nro = txtNro.getText().trim();
        String titel = txtTitel.getText().trim();

        if (titel.isEmpty()) {
            showAlert("Hinweis","Titel darf nicht leer sein.");
            return;
        }

        eintrag.setNro(nro);
        eintrag.setTitel(titel);
        eintrag.setDatum(dpDatum.getValue());

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

        // Enter NUR im letzten Feld (Ort) → Dialog bestätigen
        txtOrt.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (!btnOk.isDisabled()) {
                    dialog.setResult(new HeftEingabe(
                            txtHeftNummer.getText().trim(),
                            dpDatum.getValue(),
                            txtOrt.getText().trim()
                    ));
                    dialog.close();
                }
                event.consume();
            }
        });

        grid.add(new Label("Heftnummer (römisch):"), 0, 0);
        grid.add(txtHeftNummer, 1, 0);

        grid.add(new Label("Datum:"), 0, 1);
        grid.add(dpDatum, 1, 1);

        grid.add(new Label("Ort:"), 0, 2);
        grid.add(txtOrt, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // ResultConverter für Klick auf OK-Button
        dialog.setResultConverter(dialogBtn -> {
            if (dialogBtn == btnOKType) {
                String nummer = txtHeftNummer.getText().trim();
                LocalDate datum = dpDatum.getValue();
                String ort = txtOrt.getText().trim();

                if (nummer.isEmpty()) return null;

                return new HeftEingabe(nummer, datum, ort);
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {

            if (eingabe == null) {
                speichereHeft(result);
            } else {
                updateHeft(result);
            }

        });
    }

    private record HeftEingabe(String nummer, LocalDate datum, String ort) {
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
        heft.setSeiteVon(markierteStartSeite);
        heft.setSeiteBis(markierteEndeSeite);
        heft.setIstAktiv(true);
        heft.setSortierung(0);

        System.out.println("Heft erzeugt:");
        System.out.println("BandID: " + heft.getBandID());
        System.out.println("Nummer: " + heft.getHeftNummer());
        System.out.println("Seiten: " + heft.getSeiteVon() + " - " + heft.getSeiteBis());

        try {
            heftRepository.insert(heft);

            heftListView.getItems().setAll(
                    heftRepository.findByBand(heft.getBandID())
            );

            System.out.println("Heft gespeichert");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateHeft(HeftEingabe eingabe) {

        Heft heft = heftListView.getSelectionModel().getSelectedItem();

        if (heft == null) {
            return;
        }

        heft.setHeftNummer(eingabe.nummer());
        heft.setAusgabeDatum(eingabe.datum());

        try {

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
                ""
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

        HeftEintrag eintrag = tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (eintrag != null) {
            return eintrag.getSeiteVon();
        }

        return 1;
    }

    private int getAktiverBereichBis() {

        HeftEintrag eintrag = tblHeftEintraege.getSelectionModel().getSelectedItem();

        if (eintrag != null && eintrag.getSeiteBis() != null) {
            return eintrag.getSeiteBis();
        }

        return aktuelleBildliste.size();
    }
}