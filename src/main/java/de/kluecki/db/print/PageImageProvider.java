package de.kluecki.db.print;

import javafx.scene.image.Image;

public interface PageImageProvider {

    Image loadPageImage(String gebiet, String bandJahr, int seite);

}