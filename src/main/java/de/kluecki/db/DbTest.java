package de.kluecki.db;

import de.kluecki.db.repository.QuelleRepository;

import java.util.List;

public class DbTest {

    public static void main(String[] args) {

        QuelleRepository repo = new QuelleRepository();

        List<Quelle> quellen = repo.findAll();

        System.out.println("Gefundene Quellen:");
        System.out.println();

        for (Quelle q : quellen) {
            System.out.println(q);
        }
    }
}