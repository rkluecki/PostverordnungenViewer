package de.kluecki.db;

import java.util.List;

public class InhaltTypen {

    public static List<String> getStandardTypen() {
        return List.of(
                "Abschnitt",
                "Verordnung",
                "Bekanntmachung",
                "Dienstanweisung",
                "Formular",
                "Tarif",
                "Kursänderung",
                "Personalnachricht",
                "Liste",
                "Ortsverzeichniseintrag",
                "Sachverzeichnis",
                "Glossar",
                "Sonstiges"
        );
    }

    public static int getTypId(String typ){

        return switch(typ){

            case "Verordnung" -> 1;
            case "Bekanntmachung" -> 2;
            case "Tabelle" -> 3;
            case "Taxe" -> 4;
            case "Kursänderung" -> 5;
            case "Personalie" -> 6;
            case "Sonstiges" -> 7;

            default -> 7;
        };
    }
}