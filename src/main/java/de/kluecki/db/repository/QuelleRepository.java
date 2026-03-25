package de.kluecki.db.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.Quelle;
import java.sql.PreparedStatement;

public class QuelleRepository {

    public List<Quelle> findAll() {
        List<Quelle> quellen = new ArrayList<>();

        String sql = """
        SELECT QuelleID, ParentQuelleID, QuelleTypID, EbeneTyp, Titel, SeiteVon, SeiteBis
        FROM Quelle
        ORDER BY QuelleID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int quelleID = rs.getInt("QuelleID");
                Integer parentQuelleID = (Integer) rs.getObject("ParentQuelleID");
                int quelleTypID = rs.getInt("QuelleTypID");
                String ebeneTyp = rs.getString("EbeneTyp");
                String titel = rs.getString("Titel");
                Integer seiteVon = (Integer) rs.getObject("SeiteVon");
                Integer seiteBis = (Integer) rs.getObject("SeiteBis");

                Quelle quelle = new Quelle(
                        quelleID,
                        parentQuelleID,
                        quelleTypID,
                        ebeneTyp,
                        titel,
                        seiteVon,
                        seiteBis
                );
                quellen.add(quelle);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return quellen;
    }

    public int findBandId(String gebiet, String band) {

        System.out.println("findBandId Gebiet: [" + gebiet + "]");
        System.out.println("findBandId Band: [" + band + "]");

        String sql = """
        SELECT TOP 1 QuelleID
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND Land = ?
          AND Jahr = ?
        ORDER BY QuelleID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, gebiet);
            ps.setInt(2, Integer.parseInt(band));

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("QuelleID");
                }
            }

            System.out.println("findBandId -> kein Treffer");

        } catch (NumberFormatException ex) {
            System.out.println("Band/Jahr ist keine Zahl: " + band);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }
}