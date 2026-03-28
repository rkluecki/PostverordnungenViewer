package de.kluecki.db.repository;

import de.kluecki.db.model.Heft;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class HeftRepository {

    private final Connection connection;

    public HeftRepository(Connection connection) {
        this.connection = connection;
    }

    public List<Heft> findByBand(int bandID) {
        List<Heft> liste = new ArrayList<>();

        String sql = """
        SELECT HeftID, BandID, HeftNummer, Titel, AusgabeDatum,
               SeiteVon, SeiteBis, Ort, Bemerkung, Sortierung, IstAktiv
        FROM dbo.Heft
        WHERE BandID = ?
        ORDER BY Sortierung, HeftNummer
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bandID);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Heft h = new Heft();

                    h.setHeftID(rs.getInt("HeftID"));
                    h.setBandID(rs.getInt("BandID"));
                    h.setHeftNummer(rs.getString("HeftNummer"));
                    h.setTitel(rs.getString("Titel"));

                    if (rs.getDate("AusgabeDatum") != null) {
                        h.setAusgabeDatum(rs.getDate("AusgabeDatum").toLocalDate());
                    }

                    h.setSeiteVon(rs.getInt("SeiteVon"));
                    h.setSeiteBis(rs.getInt("SeiteBis"));
                    h.setOrt(rs.getString("Ort"));
                    h.setBemerkung(rs.getString("Bemerkung"));
                    h.setSortierung(rs.getInt("Sortierung"));
                    h.setIstAktiv(rs.getBoolean("IstAktiv"));

                    liste.add(h);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public void insert(Heft heft) {
        String sql = """
            INSERT INTO dbo.Heft
                (BandID, HeftNummer, Titel, AusgabeDatum,
                 SeiteVon, SeiteBis, Ort,Bemerkung, Sortierung, IstAktiv)
            VALUES
                (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, heft.getBandID());
            stmt.setString(2, heft.getHeftNummer());
            stmt.setString(3, heft.getTitel());

            if (heft.getAusgabeDatum() != null) {
                stmt.setDate(4, java.sql.Date.valueOf(heft.getAusgabeDatum()));
            } else {
                stmt.setDate(4, null);
            }

            stmt.setInt(5, heft.getSeiteVon());
            stmt.setInt(6, heft.getSeiteBis());
            stmt.setString(7, heft.getOrt());
            stmt.setString(8, heft.getBemerkung());
            stmt.setInt(9, heft.getSortierung());
            stmt.setBoolean(10, heft.isIstAktiv());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void update(Heft heft) {

        String sql = """
           UPDATE dbo.Heft
           SET HeftNummer = ?,
               AusgabeDatum = ?,
               SeiteVon = ?,
               SeiteBis = ?,
               Ort = ?,
               Sortierung = ?,
               IstAktiv = ?
           WHERE HeftID = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setString(1, heft.getHeftNummer());

            if (heft.getAusgabeDatum() != null) {
                stmt.setDate(2, java.sql.Date.valueOf(heft.getAusgabeDatum()));
            } else {
                stmt.setNull(2, java.sql.Types.DATE);
            }

            stmt.setInt(3, heft.getSeiteVon());
            stmt.setInt(4, heft.getSeiteBis());
            stmt.setString(5, heft.getOrt());
            stmt.setInt(6, heft.getSortierung());

            stmt.setBoolean(7, heft.isIstAktiv());
            stmt.setInt(8, heft.getHeftID());
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Aktualisieren des Hefts.", e);
        }
    }

    public boolean hatHefteZuBand(int bandId) {

        String sql = """
        SELECT COUNT(*)
        FROM dbo.Heft
        WHERE BandID = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, bandId);

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

    public void delete(int heftId) throws SQLException {
        String sql = """
        DELETE FROM dbo.Heft
        WHERE HeftID = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, heftId);
            stmt.executeUpdate();
        }
    }

    public Heft findById(int heftId) throws Exception {

        String sql = "SELECT * FROM Heft WHERE HeftID = ?";

        try (var ps = connection.prepareStatement(sql)) {

            ps.setInt(1, heftId);

            try (var rs = ps.executeQuery()) {

                if (rs.next()) {

                    Heft heft = new Heft();

                    heft.setHeftID(rs.getInt("HeftID"));
                    heft.setBandID(rs.getInt("BandID"));
                    heft.setHeftNummer(rs.getString("HeftNummer"));
                    heft.setAusgabeDatum(rs.getDate("AusgabeDatum") != null
                            ? rs.getDate("AusgabeDatum").toLocalDate()
                            : null);
                    heft.setOrt(rs.getString("Ort"));
                    heft.setSeiteVon(rs.getInt("SeiteVon"));
                    heft.setSeiteBis(rs.getInt("SeiteBis"));

                    return heft;
                }
            }
        }

        return null;
    }
}