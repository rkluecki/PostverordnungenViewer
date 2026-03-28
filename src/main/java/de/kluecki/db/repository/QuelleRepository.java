package de.kluecki.db.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.Quelle;

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

    public List<String> findBandTitelByGebiet(String gebiet) {
        List<String> liste = new ArrayList<>();

        String sql = """
        SELECT Jahr
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND Land = ?
          AND Jahr IS NOT NULL
        ORDER BY Jahr
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gebiet);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(String.valueOf(rs.getInt("Jahr")));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public void insertBand(String gebiet, int jahr) {
        String sql = """
        INSERT INTO Quelle (QuelleTypID, StatusID, EbeneTyp, Jahr, Titel, Land)
        VALUES (1, 1, 'BAND', ?, ?, ?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, jahr);
            stmt.setString(2, String.valueOf(jahr));
            stmt.setString(3, gebiet);

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Band/Jahr konnte nicht gespeichert werden.", e);
        }
    }

    public boolean bandExistiert(String gebiet, int jahr) {

        String sql = """
        SELECT COUNT(*)
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
        AND Land = ?
        AND Jahr = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gebiet);
            stmt.setInt(2, jahr);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public boolean hatBaenderZuGebiet(String gebiet) {

        String sql = """
            SELECT COUNT(*)
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND Land = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gebiet);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    public void deleteBand(int bandID) throws SQLException {

        String sql = """
        DELETE FROM dbo.Quelle
        WHERE QuelleID = ?
        AND EbeneTyp = 'BAND'
    """;

        try (Connection conn = DatabaseConnection.getConnection();
        PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandID);

            stmt.executeUpdate();
        }
    }
}