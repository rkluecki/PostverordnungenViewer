package de.kluecki.db;

import de.kluecki.db.model.VerordnungBetreff;
import de.kluecki.db.print.PrintPdfService;
import de.kluecki.db.repository.VerordnungBetreffRepository;
import javafx.application.Application;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.*;

public class PostverordnungenApp extends Application {

    private static final String ROOT_PATH = "D:\\Postgeschichte_PC\\Postverordnungen";

    private ListView<String> gebietListView;
    private ListView<String> bandListView;

    private ImageView imageView;
    private ScrollPane imageScrollPane;
    private Image currentImage;

    private final List<Path> aktuelleBildliste = new ArrayList<>();
    private int aktuellerBildIndex = -1;

    private double zoomFactor = 1.0;
    private boolean fitToWindow = true;
    private double rotationAngle = 0;

    private Button btnErsteSeite;
    private Button btnZurueck;
    private Button btnWeiter;
    private Button btnLetzteSeite;

    private Label lblSeitenstand;
    private TextField txtSeiteDirekt;

    private Label lblBandTitel;
    private Label lblVerordnungsMarkierung;

    private Integer markierteStartSeite = null;
    private Integer markierteEndeSeite = null;
    private Label lblAktuellerBetreff;
    private VerordnungBetreffRepository betreffRepository;
    private String aktuellesGebiet;
    private String aktuellesBand;
    private ListView<VerordnungBetreff> lstBetreffe;


    @Override
    public void start(Stage stage) {

        try {
            betreffRepository = new VerordnungBetreffRepository(DatabaseConnection.getConnection());
        } catch (Exception e) {
            e.printStackTrace();
        }

        BorderPane root = new BorderPane();

        root.setTop(createMenuBar());
        root.setBottom(createStatusBar());

        gebietListView = new ListView<>();
        gebietListView.getItems().addAll(loadGebiete());

        bandListView = new ListView<>();

        HBox imageToolbar = createImageToolbar();
        HBox bildNavigation = createImageNavigationBar();

        lblBandTitel = new Label("Kein Band gewählt");

        lblVerordnungsMarkierung = new Label("Keine Verordnung markiert");
        lblVerordnungsMarkierung.setStyle("""
            -fx-padding: 4 0 4 0;
            -fx-font-style: italic;
            -fx-text-fill: #444444;
        """);

        lblAktuellerBetreff = new Label("Kein Betreff auf dieser Seite");
        lblAktuellerBetreff.setStyle("""
            -fx-padding: 4 0 4 0;
            -fx-font-style: italic;
            -fx-text-fill: #444444;
""");

        lstBetreffe = new ListView<>();
        lstBetreffe.setPrefWidth(280);

        lstBetreffe.setFixedCellSize(45);
        lstBetreffe.setStyle("-fx-font-size: 12px;");

        lstBetreffe.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(VerordnungBetreff item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getSeiteVon() + "–" + item.getSeiteBis() + "\n" + item.getTitel());
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

    private VBox createNavigationPane() {
        Label lblGebiete = new Label("Gebiete");
        Label lblBaende = new Label("Jahr / Band");
        Label lblBetreffe = new Label("Verordnungen");
        lblBetreffe.setStyle("-fx-font-weight: bold;");

        VBox navigation = new VBox(8, lblGebiete, gebietListView, lblBaende, bandListView, lblBetreffe, lstBetreffe);
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

        VBox.setVgrow(lstBetreffe, javafx.scene.layout.Priority.ALWAYS);

        lstBetreffe.setOnMouseClicked(e -> {

            VerordnungBetreff betreff =
                    lstBetreffe.getSelectionModel().getSelectedItem();

            if (betreff == null) return;

            if (e.getClickCount() == 1) {

                aktuellerBildIndex = betreff.getSeiteVon() - 1;
                ladeAktuellesBild();

            }

            if (e.getClickCount() == 2) {

                TextInputDialog dialog = new TextInputDialog(betreff.getTitel());
                dialog.setTitle("Betreff ändern");
                dialog.setHeaderText("Betreff bearbeiten");
                dialog.setContentText("Betreff:");

                Optional<String> result = dialog.showAndWait();

                result.ifPresent(neuerTitel -> {

                    if (neuerTitel.trim().isEmpty()) {
                        showAlert("Hinweis", "Titel darf nicht leer sein.");
                        return;
                    }

                    betreff.setTitel(neuerTitel.trim());

                    try {
                        betreffRepository.update(betreff);
                        updateBetreffListe();
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }

                });
            }

        });

        return navigation;
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
        VBox documentHeader = new VBox(6, lblBandTitel, lblVerordnungsMarkierung, lblAktuellerBetreff, imageToolbar);
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
        Button btnBetreffAnpassen = new Button("Betreff ändern");
        Button btnBetreffLoeschen = new Button("Betreff löschen");
        Button btnVerordnungPdf = new Button("Verordnung PDF");
        btnVerordnungPdf.setTooltip(new Tooltip("Gesamte Verordnung als PDF speichern"));
        Button btnVerordnungPrint = new Button("Verordnung drucken");
        btnVerordnungPrint.setTooltip(new Tooltip("Gesamte Verordnung drucken"));

        Button btnStartVerordnung = new Button("Start Verordnung");
        Button btnEndeVerordnung = new Button("Ende Verordnung");

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

        btnBetreffAnpassen.setOnAction(e -> {

            VerordnungBetreff selected =
                    lstBetreffe.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showAlert("Hinweis", "Bitte zuerst einen Betreff auswählen.");
                return;
            }

            TextInputDialog dialog = new TextInputDialog(selected.getTitel());
            dialog.setTitle("Betreff ändern");
            dialog.setHeaderText("Betreff bearbeiten");
            dialog.setContentText("Betreff:");

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(neuerTitel -> {

                if (neuerTitel.trim().isEmpty()) {
                    showAlert("Hinweis", "Titel darf nicht leer sein.");
                    return;
                }

                selected.setTitel(neuerTitel.trim());

                try {
                    betreffRepository.update(selected);
                    updateBetreffListe();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
        });

        btnBetreffLoeschen.setOnAction(e -> {

            VerordnungBetreff selected =
                    lstBetreffe.getSelectionModel().getSelectedItem();

            if (selected == null) {
                showAlert("Hinweis", "Bitte zuerst einen Betreff auswählen.");
                return;
            }

            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Betreff löschen");
            confirm.setHeaderText("Betreff wirklich löschen?");
            confirm.setContentText(selected.getTitel());

            Optional<ButtonType> result = confirm.showAndWait();

            if (result.isEmpty() || result.get() != ButtonType.OK) {
                return;
            }
            try {
                betreffRepository.delete(selected.getVerordnungBetreffID());
                updateBetreffListe();
                updateBetreffAnzeige(aktuellerBildIndex + 1);
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        });

        btnStartVerordnung.setOnAction(e -> {
            if (aktuellerBildIndex < 0) return;
            markierteStartSeite = aktuellerBildIndex + 1;
            markierteEndeSeite = null;
            updateVerordnungsMarkierung();
        });

        btnEndeVerordnung.setOnAction(e -> {
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

            updateVerordnungsMarkierung();

            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Verordnung speichern");
            dialog.setHeaderText("Betreff der Verordnung eingeben");
            dialog.setContentText("Betreff:");

            Optional<String> result = dialog.showAndWait();

            result.ifPresent(this::speichereVerordnungBetreff);
        });

        btnVerordnungPdf.setOnAction(e -> exportSelectedVerordnungToPdf());
        btnVerordnungPrint.setOnAction(e -> printSelectedVerordnung());

        HBox imageToolbar = new HBox(
                5,
                btnZoomIn,
                btnZoomOut,
                btnFit,
                btnRotateLeft,
                btnRotateRight,
                new Separator(),
                btnStartVerordnung,
                btnEndeVerordnung,
                btnBetreffAnpassen,
                btnBetreffLoeschen,
                btnVerordnungPrint,
                btnVerordnungPdf
        );

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
            if (!aktuelleBildliste.isEmpty()) {
                aktuellerBildIndex = 0;
                ladeAktuellesBild();
            }
        });

        btnLetzteSeite.setOnAction(e -> {
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
            resetVerordnungsMarkierung();
            updateBetreffListe();

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

    private void resetVerordnungsMarkierung() {
        markierteStartSeite = null;
        markierteEndeSeite = null;
        updateVerordnungsMarkierung();
    }

    private void updateVerordnungsMarkierung() {
        if (lblVerordnungsMarkierung == null) return;

        if (markierteStartSeite == null && markierteEndeSeite == null) {
            lblVerordnungsMarkierung.setText("Keine Verordnung markiert");
        } else if (markierteStartSeite != null && markierteEndeSeite == null) {
            lblVerordnungsMarkierung.setText("Verordnung: Start Seite " + markierteStartSeite);
        } else if (markierteStartSeite != null && markierteEndeSeite != null && markierteStartSeite.equals(markierteEndeSeite)) {
            lblVerordnungsMarkierung.setText("Verordnung: Seite " + markierteStartSeite);
        } else {
            lblVerordnungsMarkierung.setText(
                    "Verordnung: Seite " + markierteStartSeite + " bis " + markierteEndeSeite
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
        if (aktuelleBildliste.isEmpty()) return;

        if (aktuellerBildIndex > 0) {
            aktuellerBildIndex--;
            ladeAktuellesBild();
        }
    }

    private void zeigeNaechstesBild() {
        if (aktuelleBildliste.isEmpty()) return;

        if (aktuellerBildIndex < aktuelleBildliste.size() - 1) {
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
        updateBetreffAnzeige(aktuellerBildIndex + 1);
    }

    private void updateNavigationState() {
        boolean hatBilder = !aktuelleBildliste.isEmpty();
        boolean istErste = aktuellerBildIndex <= 0;
        boolean istLetzte = !hatBilder || aktuellerBildIndex >= aktuelleBildliste.size() - 1;

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

    private void exportSelectedVerordnungToPdf() {

        VerordnungBetreff selected = lstBetreffe.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst eine Verordnung auswählen.");
            return;
        }

        PrintPdfService service = createPrintPdfService();
        service.exportRangeToPdf(
                selected.getGebiet(),
                selected.getBandJahr(),
                selected.getSeiteVon(),
                selected.getSeiteBis(),
                selected.getTitel()
        );
    }

    private void printSelectedVerordnung() {

        VerordnungBetreff selected = lstBetreffe.getSelectionModel().getSelectedItem();

        if (selected == null) {
            showAlert("Hinweis", "Bitte zuerst eine Verordnung auswählen.");
            return;
        }

        PrintPdfService service = createPrintPdfService();

        service.printRange(selected);
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

    private void updateBetreffAnzeige(String text) {
        lblAktuellerBetreff.setText(text);
    }

    private void updateBetreffAnzeige(int aktuelleSeite) {

        try {

            VerordnungBetreff betreff =
                    betreffRepository.findForPage(aktuellesGebiet, aktuellesBand, aktuelleSeite);

            if (betreff != null) {

                lblAktuellerBetreff.setText(
                        "Betreff: " + betreff.getTitel() +
                                " (S. " + betreff.getSeiteVon() +
                                "–" + betreff.getSeiteBis() + ")"
                );

                for (VerordnungBetreff eintrag : lstBetreffe.getItems()) {
                    if (eintrag.getVerordnungBetreffID() == betreff.getVerordnungBetreffID()) {
                        lstBetreffe.getSelectionModel().select(eintrag);
                        lstBetreffe.scrollTo(eintrag);
                        break;
                    }
                }

            } else {

                lblAktuellerBetreff.setText("Kein Betreff auf dieser Seite");
                lstBetreffe.getSelectionModel().clearSelection();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private VerordnungBetreff hatUeberschneidungMitBestehendemBereich(int neueSeiteVon, int neueSeiteBis) {

        for (VerordnungBetreff vorhanden : lstBetreffe.getItems()) {

            if (!java.util.Objects.equals(vorhanden.getGebiet(), aktuellesGebiet)) {
                continue;
            }

            if (!java.util.Objects.equals(vorhanden.getBandJahr(), aktuellesBand)) {
                continue;
            }

            int vorhandenVon = vorhanden.getSeiteVon();
            int vorhandenBis = vorhanden.getSeiteBis();

            boolean ueberschneidetSich =
                    neueSeiteVon <= vorhandenBis && neueSeiteBis >= vorhandenVon;

            if (ueberschneidetSich) {
                return vorhanden;
            }
        }

        return null;
    }

    private void speichereVerordnungBetreff(String titel) {

        if (markierteStartSeite == null || markierteEndeSeite == null) {
            System.out.println("Keine vollständige Markierung vorhanden");
            return;
        }

        if (titel == null || titel.trim().isEmpty()) {
            showAlert("Hinweis", "Bitte einen Betreff eingeben.");
            return;
        }

        try {

            VerordnungBetreff betreff = new VerordnungBetreff();

            betreff.setGebiet(aktuellesGebiet);
            betreff.setBandJahr(aktuellesBand);

            int vonSeite = Math.min(markierteStartSeite, markierteEndeSeite);
            int bisSeite = Math.max(markierteStartSeite, markierteEndeSeite);

            VerordnungBetreff konflikt =
                    hatUeberschneidungMitBestehendemBereich(vonSeite, bisSeite);

            if (konflikt != null) {
                showAlert("Hinweis",
                        "Überschneidung mit:\n" +
                                konflikt.getTitel() +
                                " (Seiten " +
                                konflikt.getSeiteVon() +
                                "–" +
                                konflikt.getSeiteBis() +
                                ")");
                return;
            }

            betreff.setSeiteVon(vonSeite);
            betreff.setSeiteBis(bisSeite);
            betreff.setTitel(titel.trim());
            betreff.setBemerkung(null);

            betreffRepository.insert(betreff);
            updateBetreffListe();
            updateBetreffAnzeige(aktuellerBildIndex + 1);
            markierteStartSeite = null;
            markierteEndeSeite = null;
            updateVerordnungsMarkierung();

            System.out.println("Betreff gespeichert");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void updateBetreffListe() {

        try {

            if (aktuellesGebiet == null || aktuellesBand == null) {
                return;
            }

            lstBetreffe.getItems().clear();

            List<VerordnungBetreff> liste =
                    betreffRepository.findByBand(aktuellesGebiet, aktuellesBand);

            lstBetreffe.getItems().addAll(liste);

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
}