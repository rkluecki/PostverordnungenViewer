package de.kluecki.db;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import de.kluecki.db.print.PrintPdfService;


public class DocumentViewerWindow {

    public static void open(Image image, String titel, String quelle, String jahr, String typ) {

        Stage stage = new Stage();
        stage.setTitle("Dokumentansicht");

        ImageView imageView = new ImageView(image);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Group imageGroup = new Group(imageView);

        StackPane imageContainer = new StackPane(imageGroup);
        imageContainer.setAlignment(Pos.CENTER);

        ScrollPane scrollPane = new ScrollPane(imageContainer);
        scrollPane.setPannable(true);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        final double[] zoomFactor = {1.0};
        final boolean[] fitToWindow = {true};
        final double[] rotationAngle = {0};

        Button btnZoomIn = new Button("+");
        Button btnZoomOut = new Button("-");
        Button btnFit = new Button("Anpassen");
        Button btnOriginal = new Button("100%");
        Button btnRotateLeft = new Button("⟲");
        Button btnRotateRight = new Button("⟳");
        Button btnReset = new Button("Reset");
        Label lblZoom = new Label("100%");
        HBox zoomBox = new HBox(lblZoom);
        zoomBox.setAlignment(Pos.CENTER);
        zoomBox.setPrefWidth(90);

        zoomBox.setStyle("""
    -fx-border-color: #c0c0c0;
    -fx-border-radius: 3;
    -fx-background-radius: 3;
    -fx-padding: 3 8 3 8;
    -fx-background-color: #fafafa;
""");
        lblZoom.setStyle("-fx-font-weight: bold;");
        btnZoomIn.setTooltip(new Tooltip("Vergrößern"));
        btnZoomOut.setTooltip(new Tooltip("Verkleinern"));
        btnFit.setTooltip(new Tooltip("Bild ins Fenster einpassen"));
        btnRotateLeft.setTooltip(new Tooltip("90° nach links drehen"));
        btnRotateRight.setTooltip(new Tooltip("90° nach rechts drehen"));
        btnReset.setTooltip(new Tooltip("Rotation zurücksetzen und neu anpassen"));
        btnOriginal.setTooltip(new Tooltip("Originalgröße anzeigen (100%)"));
        Button btnPrint = new Button("Drucken");
        btnPrint.setTooltip(new Tooltip("Seite drucken"));
        Button btnPdf = new Button("PDF");
        btnPdf.setTooltip(new Tooltip("Seite als PDF speichern"));

        HBox toolbar = new HBox(10, btnZoomIn, btnZoomOut, btnFit, btnOriginal, btnRotateLeft, btnRotateRight, btnReset, zoomBox, btnPrint, btnPdf);
        toolbar.setStyle("""
            -fx-padding: 5;
            -fx-background-color: #f0f0f0;
            -fx-border-color: #d0d0d0;
        """);

        VBox infoPanel = new VBox(10);
        infoPanel.setPrefWidth(260);
        infoPanel.setMinWidth(220);
        infoPanel.setStyle("""
            -fx-padding: 10;
            -fx-background-color: #f7f7f7;
            -fx-border-color: #d0d0d0;
            -fx-border-width: 0 1 0 0;
        """);

        Label lblInfoTitel = new Label("Dokument-Info");
        lblInfoTitel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label lblTitel = new Label("Titel: " + titel);
        Label lblQuelle = new Label("Quelle: " + quelle);
        Label lblJahr = new Label("Jahr: " + jahr);
        Label lblTyp = new Label("Typ: " + typ);
        Label lblSeite = new Label("Seite: 1");
        Label lblHinweis = new Label("Hinweis: Platzhalter für spätere Detaildaten");

        infoPanel.getChildren().addAll(
                lblInfoTitel,
                lblTitel,
                lblQuelle,
                lblJahr,
                lblTyp,
                lblSeite,
                lblHinweis
        );

        Runnable updateImageView = () -> {
            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();

            boolean rotated = Math.abs(rotationAngle[0] % 180) == 90;

            double layoutWidth = rotated ? imageHeight : imageWidth;
            double layoutHeight = rotated ? imageWidth : imageHeight;

            if (fitToWindow[0]) {
                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();

                if (viewportWidth <= 0 || viewportHeight <= 0) {
                    return;
                }

                double scaleX = viewportWidth / layoutWidth;
                double scaleY = viewportHeight / layoutHeight;
                double scale = Math.min(scaleX, scaleY);

                imageView.setFitWidth(imageWidth * scale);
                imageView.setFitHeight(imageHeight * scale);

                imageGroup.setRotate(rotationAngle[0]);

                imageContainer.setPrefWidth(layoutWidth * scale);
                imageContainer.setPrefHeight(layoutHeight * scale);
                imageContainer.setMinWidth(layoutWidth * scale);
                imageContainer.setMinHeight(layoutHeight * scale);
                lblZoom.setText((int)(scale * 100) + " %");
            } else {
                imageView.setFitWidth(imageWidth * zoomFactor[0]);
                imageView.setFitHeight(imageHeight * zoomFactor[0]);

                imageGroup.setRotate(rotationAngle[0]);

                double scaledImageWidth = imageWidth * zoomFactor[0];
                double scaledImageHeight = imageHeight * zoomFactor[0];

                double containerWidth = rotated ? scaledImageHeight : scaledImageWidth;
                double containerHeight = rotated ? scaledImageWidth : scaledImageHeight;

                imageContainer.setPrefWidth(containerWidth);
                imageContainer.setPrefHeight(containerHeight);
                imageContainer.setMinWidth(containerWidth);
                imageContainer.setMinHeight(containerHeight);
                lblZoom.setText((int)(zoomFactor[0] * 100) + " %");
            }
        };

        Runnable applyFitZoomAsBase = () -> {
            double viewportWidth = scrollPane.getViewportBounds().getWidth();
            double viewportHeight = scrollPane.getViewportBounds().getHeight();

            if (viewportWidth <= 0 || viewportHeight <= 0) {
                zoomFactor[0] = 1.0;
                return;
            }

            double imageWidth = image.getWidth();
            double imageHeight = image.getHeight();

            boolean rotated = Math.abs(rotationAngle[0] % 180) == 90;

            double layoutWidth = rotated ? imageHeight : imageWidth;
            double layoutHeight = rotated ? imageWidth : imageHeight;

            double scaleX = viewportWidth / layoutWidth;
            double scaleY = viewportHeight / layoutHeight;
            zoomFactor[0] = Math.min(scaleX, scaleY);
        };

        imageGroup.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                fitToWindow[0] = true;
                updateImageView.run();
            }
        });

        btnZoomIn.setOnAction(e -> {
            if (fitToWindow[0]) {
                applyFitZoomAsBase.run();
                fitToWindow[0] = false;
            }

            zoomFactor[0] *= 1.25;
            updateImageView.run();
        });

        btnZoomOut.setOnAction(e -> {
            if (fitToWindow[0]) {
                applyFitZoomAsBase.run();
                fitToWindow[0] = false;
            }

            zoomFactor[0] /= 1.25;
            updateImageView.run();
        });

        btnFit.setOnAction(e -> {
            fitToWindow[0] = true;
            updateImageView.run();
        });

        btnOriginal.setOnAction(e -> {
            fitToWindow[0] = false;
            zoomFactor[0] = 1.0;
            updateImageView.run();
        });

        btnRotateLeft.setOnAction(e -> {
            rotationAngle[0] -= 90;
            updateImageView.run();
        });

        btnRotateRight.setOnAction(e -> {
            rotationAngle[0] += 90;
            updateImageView.run();
        });

        btnReset.setOnAction(e -> {
            rotationAngle[0] = 0;
            fitToWindow[0] = true;
            updateImageView.run();
        });

        btnPrint.setOnAction(e -> {
            PrintPdfService service = new PrintPdfService(
                    "D:\\Postgeschichte_PC\\Postverordnungen",
                    (gebiet, bandJahr, seite) -> null
            );
            service.printSinglePage(image);
        });

        btnPdf.setOnAction(e -> {
            PrintPdfService service = new PrintPdfService(
                    "D:\\Postgeschichte_PC\\Postverordnungen",
                    (gebiet, bandJahr, seite) -> null
            );
            service.exportSinglePageToPdf(image, titel);
        });

        scrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> {
            if (fitToWindow[0]) {
                updateImageView.run();
            }
        });

        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (!event.isControlDown()) {
                return;
            }

            if (fitToWindow[0]) {
                applyFitZoomAsBase.run();
                fitToWindow[0] = false;
            }

            if (event.getDeltaY() > 0) {
                zoomFactor[0] *= 1.15;
            } else if (event.getDeltaY() < 0) {
                zoomFactor[0] /= 1.15;
            }

            updateImageView.run();
            event.consume();
        });

        BorderPane contentPane = new BorderPane();
        contentPane.setLeft(infoPanel);
        contentPane.setCenter(scrollPane);

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(contentPane);

        Scene scene = new Scene(root, 1400, 950);
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        stage.show();
        updateImageView.run();
    }
}