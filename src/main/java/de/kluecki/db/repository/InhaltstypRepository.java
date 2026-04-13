package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import de.kluecki.db.model.Inhaltstyp;

public class InhaltstypRepository {

    public List<Inhaltstyp> findAllInhaltstypen() {
        List<Inhaltstyp> liste = new ArrayList<>();

        String sql = "SELECT InhaltstypID, Bezeichnung FROM dbo.Inhaltstyp ORDER BY InhaltstypID";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int id = rs.getInt("InhaltstypID");
                String bez = rs.getString("Bezeichnung");

                liste.add(new Inhaltstyp(id, bez));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }
}