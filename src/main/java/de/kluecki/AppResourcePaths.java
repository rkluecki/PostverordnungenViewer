package de.kluecki;

import java.nio.file.Files;
import java.nio.file.Path;

public final class AppResourcePaths {

    private AppResourcePaths() {
        // Hilfsklasse, keine Instanzen
    }

    public static Path getSuiteResourcesFolder() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();

        while (current != null) {
            Path candidate = current.resolve("resources");

            if (Files.isDirectory(candidate)
                    && Files.exists(candidate.resolve("icon.png"))
                    && Files.exists(candidate.resolve("startbild.png"))) {
                return candidate;
            }

            current = current.getParent();
        }

        throw new IllegalStateException(
                "Der Suite-Ressourcenordner für Ordinata wurde nicht gefunden. " +
                        "Erwartet wird ein Ordner 'resources' mit icon.png und startbild.png."
        );
    }

    public static Path getIconPngPath() {
        return getSuiteResourcesFolder().resolve("icon.png");
    }

    public static Path getStartbildPngPath() {
        return getSuiteResourcesFolder().resolve("startbild.png");
    }

    public static Path getBeschreibungPath() {
        return getSuiteResourcesFolder().resolve("beschreibung.txt");
    }
}