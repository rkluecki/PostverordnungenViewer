package de.kluecki.db.UI;

import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.SVGPath;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

public final class OrdinataGebietsWappenFactory {

    private OrdinataGebietsWappenFactory() {
    }

    public static Node erstelle(
            String gebiet) {

        if ("Baden".equalsIgnoreCase(gebiet)) {
            return erstelleBadenWappen();
        }

        if ("Bayern".equalsIgnoreCase(gebiet)) {
            return erstelleBayernWappen();
        }

        if ("Preussen".equalsIgnoreCase(gebiet)) {
            return erstellePreussenWappen();
        }

        if ("Sachsen".equalsIgnoreCase(gebiet)) {
            return erstelleSachsenWappen();
        }

        if ("Württemberg".equalsIgnoreCase(gebiet)) {
            return erstelleWuerttembergWappen();
        }

        if ("NPD".equalsIgnoreCase(gebiet)) {
            return erstelleNpdWappen();
        }

        if ("Reichspost".equalsIgnoreCase(gebiet)) {
            return erstelleReichspostWappen();
        }

        return null;
    }

    private static Node erstelleBadenWappen() {

        String schildForm =
                "M 1 1 "
                        + "L 17 1 "
                        + "L 17 11 "
                        + "C 17 16 13 19 9 21 "
                        + "C 5 19 1 16 1 11 "
                        + "Z";

        SVGPath schildFlaeche = new SVGPath();
        schildFlaeche.setContent(schildForm);
        schildFlaeche.setFill(
                Color.web("#f2d36b")
        );

        Line schraegbalken =
                new Line(
                        -2,
                        18,
                        20,
                        3
                );

        schraegbalken.setStroke(
                Color.web("#c9363e")
        );

        schraegbalken.setStrokeWidth(4.2);

        SVGPath ausschnitt = new SVGPath();
        ausschnitt.setContent(schildForm);

        schraegbalken.setClip(ausschnitt);

        SVGPath schildRand = new SVGPath();
        schildRand.setContent(schildForm);
        schildRand.setFill(Color.TRANSPARENT);
        schildRand.setStroke(
                Color.web("#756d59")
        );
        schildRand.setStrokeWidth(0.8);

        Pane wappen = new Pane(
                schildFlaeche,
                schraegbalken,
                schildRand
        );

        wappen.setMinSize(18, 22);
        wappen.setPrefSize(18, 22);
        wappen.setMaxSize(18, 22);

        return wappen;
    }

    private static Node erstelleBayernWappen() {

        var bildUrl =
                OrdinataGebietsWappenFactory.class.getResource(
                        "/images/gebiete/bayern_wappen.png"
                );

        if (bildUrl == null) {
            System.err.println(
                    "Bayern-Wappen wurde nicht gefunden."
            );
            return null;
        }

        Image bild = new Image(
                bildUrl.toExternalForm()
        );

        ImageView wappen = new ImageView(bild);

        wappen.setFitWidth(18);
        wappen.setFitHeight(22);
        wappen.setPreserveRatio(true);
        wappen.setSmooth(true);

        return wappen;
    }

    private static Node erstellePreussenWappen() {

        var bildUrl =
                OrdinataGebietsWappenFactory.class.getResource(
                        "/images/gebiete/preussen_wappen.png"
                );

        if (bildUrl == null) {
            System.err.println(
                    "Preußen-Wappen wurde nicht gefunden."
            );
            return null;
        }

        Image bild = new Image(
                bildUrl.toExternalForm()
        );

        ImageView wappen = new ImageView(bild);

        wappen.setFitWidth(18);
        wappen.setFitHeight(22);
        wappen.setPreserveRatio(true);
        wappen.setSmooth(true);

        return wappen;
    }

    private static Node erstelleSachsenWappen() {

        var bildUrl =
                OrdinataGebietsWappenFactory.class.getResource(
                        "/images/gebiete/sachsen_wappen.png"
                );

        if (bildUrl == null) {
            System.err.println(
                    "Sachsen-Wappen wurde nicht gefunden."
            );
            return null;
        }

        Image bild = new Image(
                bildUrl.toExternalForm()
        );

        ImageView wappen = new ImageView(bild);

        wappen.setFitWidth(18);
        wappen.setFitHeight(22);
        wappen.setPreserveRatio(true);
        wappen.setSmooth(true);

        return wappen;
    }

    private static Node erstelleWuerttembergWappen() {

        var bildUrl =
                OrdinataGebietsWappenFactory.class.getResource(
                        "/images/gebiete/wuerttemberg_wappen.png"
                );

        if (bildUrl == null) {
            System.err.println(
                    "Württemberg-Wappen wurde nicht gefunden."
            );
            return null;
        }

        Image bild = new Image(
                bildUrl.toExternalForm()
        );

        ImageView wappen = new ImageView(bild);

        wappen.setFitWidth(18);
        wappen.setFitHeight(22);
        wappen.setPreserveRatio(true);
        wappen.setSmooth(true);

        return wappen;
    }

    private static Node erstelleNpdWappen() {

        var bildUrl =
                OrdinataGebietsWappenFactory.class.getResource(
                        "/images/gebiete/npd_wappen.png"
                );

        if (bildUrl == null) {
            System.err.println(
                    "Wappen des Norddeutschen Postbezirks wurde nicht gefunden."
            );
            return null;
        }

        Image bild = new Image(
                bildUrl.toExternalForm()
        );

        ImageView wappen = new ImageView(bild);

        wappen.setFitWidth(18);
        wappen.setFitHeight(22);
        wappen.setPreserveRatio(true);
        wappen.setSmooth(true);

        return wappen;
    }

    private static Node erstelleReichspostWappen() {

        var bildUrl =
                OrdinataGebietsWappenFactory.class.getResource(
                        "/images/gebiete/reichspost_wappen.png"
                );

        if (bildUrl == null) {
            System.err.println(
                    "Reichspost-Wappen wurde nicht gefunden."
            );
            return null;
        }

        Image bild = new Image(
                bildUrl.toExternalForm()
        );

        ImageView wappen = new ImageView(bild);

        wappen.setFitWidth(18);
        wappen.setFitHeight(22);
        wappen.setPreserveRatio(true);
        wappen.setSmooth(true);

        return wappen;
    }
}