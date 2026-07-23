package de.kluecki.db.repository;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.Quelle;
import de.kluecki.db.model.BandNavigationEintrag;

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

        String sql;
        boolean istZeitraum = band != null && band.matches("\\d{4}-\\d{4}");

        if (istZeitraum) {
            sql = """
        SELECT TOP 1 QuelleID
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND Land = ?
          AND JahrVon = ?
          AND JahrBis = ?
        ORDER BY QuelleID
        """;
        } else {
            sql = """
        SELECT TOP 1 QuelleID
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND Land = ?
          AND Jahr = ?
        ORDER BY QuelleID
        """;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, gebiet);

            if (istZeitraum) {
                String[] teile = band.split("-");
                ps.setInt(2, Integer.parseInt(teile[0]));
                ps.setInt(3, Integer.parseInt(teile[1]));
            } else {
                ps.setInt(2, Integer.parseInt(band));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("QuelleID");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    public int findHauptbandId(String gebiet, String band) {

        String sql;
        boolean istZeitraum =
                band != null && band.matches("\\d{4}-\\d{4}");

        if (istZeitraum) {
            sql = """
            SELECT TOP 1 QuelleID
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND ParentQuelleID IS NULL
              AND Land = ?
              AND JahrVon = ?
              AND JahrBis = ?
            ORDER BY QuelleID
            """;
        } else {
            sql = """
            SELECT TOP 1 QuelleID
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND ParentQuelleID IS NULL
              AND Land = ?
              AND Jahr = ?
            ORDER BY QuelleID
            """;
        }

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, gebiet);

            if (istZeitraum) {
                String[] teile = band.split("-");

                ps.setInt(2, Integer.parseInt(teile[0]));
                ps.setInt(3, Integer.parseInt(teile[1]));
            } else {
                ps.setInt(2, Integer.parseInt(band));
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("QuelleID");
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return 0;
    }

    public List<String> findBandTitelByGebiet(String gebiet) {
        List<String> liste = new ArrayList<>();

        String sql = """
            SELECT Jahr, JahrVon, JahrBis
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND ParentQuelleID IS NULL
              AND Land = ?
              AND Jahr IS NOT NULL
            ORDER BY Jahr
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gebiet);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    int jahr = rs.getInt("Jahr");

                    Integer jahrVon = (Integer) rs.getObject("JahrVon");
                    Integer jahrBis = (Integer) rs.getObject("JahrBis");

                    String anzeige;

                    if (jahrVon != null && jahrBis != null && !jahrVon.equals(jahrBis)) {
                        anzeige = jahrVon + "-" + jahrBis;
                    } else {
                        anzeige = String.valueOf(jahr);
                    }

                    liste.add(anzeige);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<BandNavigationEintrag> findBandNavigationByGebiet(
            String gebiet) {

        List<BandNavigationEintrag> liste = new ArrayList<>();

        String sql = """
        SELECT
            QuelleID,
            ParentQuelleID,
            Jahr,
            Titel,
            BandOrdner
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND Land = ?
          AND Jahr IS NOT NULL
        ORDER BY
            Jahr,
            CASE
                WHEN ParentQuelleID IS NULL THEN 0
                ELSE 1
            END,
            Titel,
            QuelleID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gebiet);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    int quelleId = rs.getInt("QuelleID");

                    Integer parentQuelleId =
                            (Integer) rs.getObject("ParentQuelleID");

                    int jahr = rs.getInt("Jahr");
                    String titel = rs.getString("Titel");
                    String bandOrdner = rs.getString("BandOrdner");

                    BandNavigationEintrag eintrag =
                            new BandNavigationEintrag(
                                    quelleId,
                                    parentQuelleId,
                                    jahr,
                                    titel,
                                    bandOrdner
                            );

                    liste.add(eintrag);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<BandNavigationEintrag> findHauptbaendeByGebiet(String gebiet) {

        List<BandNavigationEintrag> liste = new ArrayList<>();

        String sql = """
            SELECT
                QuelleID,
                ParentQuelleID,
                Jahr,
                Titel,
                BandOrdner
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND ParentQuelleID IS NULL
              AND Land = ?
              AND Jahr IS NOT NULL
            ORDER BY Jahr, QuelleID
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, gebiet);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    int quelleId = rs.getInt("QuelleID");
                    Integer parentQuelleId =
                            (Integer) rs.getObject("ParentQuelleID");
                    int jahr = rs.getInt("Jahr");
                    String titel = rs.getString("Titel");
                    String bandOrdner = rs.getString("BandOrdner");

                    BandNavigationEintrag eintrag =
                            new BandNavigationEintrag(
                                    quelleId,
                                    parentQuelleId,
                                    jahr,
                                    titel,
                                    bandOrdner
                            );

                    liste.add(eintrag);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<BandNavigationEintrag> findUnterbaendeByParentId(int parentQuelleId) {

        List<BandNavigationEintrag> liste = new ArrayList<>();

        String sql = """
        SELECT
            QuelleID,
            ParentQuelleID,
            Jahr,
            Titel,
            BandOrdner
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND ParentQuelleID = ?
        ORDER BY Titel, QuelleID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parentQuelleId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {

                    int quelleId = rs.getInt("QuelleID");
                    Integer parentQuelleIdWert =
                            (Integer) rs.getObject("ParentQuelleID");
                    int jahr = rs.getInt("Jahr");
                    String titel = rs.getString("Titel");
                    String bandOrdner = rs.getString("BandOrdner");

                    BandNavigationEintrag eintrag =
                            new BandNavigationEintrag(
                                    quelleId,
                                    parentQuelleIdWert,
                                    jahr,
                                    titel,
                                    bandOrdner
                            );

                    liste.add(eintrag);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<String> findUnterbandTitelByParentId(int parentQuelleId) {

        List<String> liste = new ArrayList<>();

        String sql = """
            SELECT Titel
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND ParentQuelleID = ?
            ORDER BY Titel
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parentQuelleId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    liste.add(rs.getString("Titel"));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public boolean hatUnterbaende(int parentQuelleId) {

        String sql = """
            SELECT COUNT(*)
            FROM Quelle
            WHERE EbeneTyp = 'BAND'
              AND ParentQuelleID = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parentQuelleId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Unterbände konnten nicht geprüft werden.",
                    e
            );
        }

        return false;
    }

    public boolean unterbandExistiert(
            int parentQuelleId,
            String titel) {

        String sql = """
        SELECT COUNT(*)
        FROM Quelle
        WHERE EbeneTyp = 'BAND'
          AND ParentQuelleID = ?
          AND LOWER(Titel) = LOWER(?)
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parentQuelleId);
            stmt.setString(2, titel);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Unterband konnte nicht auf Dubletten geprüft werden.",
                    e
            );
        }

        return false;
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

    public int insertUnterband(
            int parentQuelleId,
            String gebiet,
            int jahr,
            String titel,
            String bandOrdner) {

        String sql = """
            INSERT INTO Quelle
            (
                ParentQuelleID,
                QuelleTypID,
                StatusID,
                EbeneTyp,
                Jahr,
                Titel,
                Land,
                BandOrdner
            )
            OUTPUT INSERTED.QuelleID
            VALUES
            (
                ?,
                1,
                1,
                'BAND',
                ?,
                ?,
                ?,
                ?
            )
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, parentQuelleId);
            stmt.setInt(2, jahr);
            stmt.setString(3, titel);
            stmt.setString(4, gebiet);
            stmt.setString(5, bandOrdner);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("QuelleID");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();

            throw new RuntimeException(
                    "Unterband konnte nicht gespeichert werden.",
                    e
            );
        }

        return 0;
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