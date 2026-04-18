package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.SeitenMapping;
import de.kluecki.db.model.SeitenMappingEintrag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SeitenMappingRepository {

    public List<SeitenMapping> findByBandId(int bandId) {
        List<SeitenMapping> liste = new ArrayList<>();

        String sql = """
                SELECT MappingID, bandId, BildIndex, Dateiname, LogischeSeite
                FROM SeitenMapping
                WHERE BandID = ?
                ORDER BY BildIndex
                """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    SeitenMapping mapping = new SeitenMapping();
                    mapping.setMappingID(rs.getInt("MappingID"));
                    mapping.setBandID(rs.getInt("BandID"));
                    mapping.setBildIndex(rs.getInt("BildIndex"));
                    mapping.setDateiname(rs.getString("Dateiname"));
                    mapping.setLogischeSeite(rs.getString("LogischeSeite"));

                    liste.add(mapping);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public String findLogischeSeiteByBandIdAndBildIndex(int bandId, int bildIndex) {

        String sql = """
            SELECT LogischeSeite
            FROM SeitenMapping
            WHERE bandId = ? AND BildIndex = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);
            stmt.setInt(2, bildIndex);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("LogischeSeite");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public int countByBandId(int bandId) {

        String sql = """
            SELECT COUNT(*)
            FROM SeitenMapping
            WHERE BandID = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public boolean hatMappingZuBand(int bandId) {
        return countByBandId(bandId) > 0;
    }

    public void insert(SeitenMapping mapping) {

        String sql = """
                INSERT INTO SeitenMapping (BandID, BildIndex, Dateiname, LogischeSeite)
                VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, mapping.getBandID());
            stmt.setInt(2, mapping.getBildIndex());
            stmt.setString(3, mapping.getDateiname());
            stmt.setString(4, mapping.getLogischeSeite());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void initialisiereGrundmappingFuerBand(int bandId, List<String> dateinamen) {

        if (hatMappingZuBand(bandId)) {
            return;
        }

        if (dateinamen == null || dateinamen.isEmpty()) {
            return;
        }

        for (int i = 0; i < dateinamen.size(); i++) {
            SeitenMapping mapping = new SeitenMapping();
            mapping.setBandID(bandId);
            mapping.setBildIndex(i + 1);
            mapping.setDateiname(dateinamen.get(i));
            mapping.setLogischeSeite(String.valueOf(i + 1));

            insert(mapping);
        }
    }

    public void updateLogischeSeite(int bandId, int bildIndex, String logischeSeite) {

        String sql = """
        UPDATE SeitenMapping
        SET LogischeSeite = ?
        WHERE BandID = ? AND BildIndex = ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, logischeSeite);
            stmt.setInt(2, bandId);
            stmt.setInt(3, bildIndex);

            int rows = stmt.executeUpdate();

            if (rows == 0) {
                System.out.println("WARNUNG: Kein Mapping-Eintrag gefunden für Update!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String findLogischeSeiteByBandIdAndDateiname(int bandId, String dateiname) {

        String sql = """
        SELECT LogischeSeite
        FROM SeitenMapping
        WHERE BandID = ?
          AND Dateiname = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);
            stmt.setString(2, dateiname);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("LogischeSeite");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public String findDateinameByBandIdAndLogischeSeite(int bandId, String logischeSeite) {

        String sql = """
        SELECT Dateiname
        FROM SeitenMapping
        WHERE BandID = ?
          AND LogischeSeite = ?
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, bandId);
            stmt.setString(2, logischeSeite);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("Dateiname");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}