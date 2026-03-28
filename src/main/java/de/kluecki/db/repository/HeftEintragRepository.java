/*
 * Neue Zielstruktur / Repository
 *
 * Zweck:
 * Zugriff auf HeftEintrag Daten.
 *
 * Rolle im Projekt:
 * Zentrale Datenstruktur der neuen Architektur.
 *
 * Später:
 * Soll die alte Veröffentlichungslogik ablösen.
 */

package de.kluecki.db.repository;

import de.kluecki.db.model.HeftEintrag;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HeftEintragRepository {

    private final Connection connection;

    public HeftEintragRepository(Connection connection) {
        this.connection = connection;
    }

    public List<HeftEintrag> findByHeft(int heftID) {

        List<HeftEintrag> liste = new ArrayList<>();

        String sql =
                "SELECT * " +
                        "FROM HeftEintrag " +
                        "WHERE HeftID = ? " +
                        "ORDER BY SeiteVon";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, heftID);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                HeftEintrag he = new HeftEintrag();

                he.setHeftEintragID(rs.getInt("HeftEintragID"));
                he.setHeftID(rs.getInt("HeftID"));
                he.setHeftEintragTypID(rs.getInt("HeftEintragTypID"));

                he.setNro(rs.getString("Nro"));
                he.setTitel(rs.getString("Titel"));

                Date d = rs.getDate("Datum");

                if (d != null) {
                    he.setDatum(d.toLocalDate());
                }

                he.setSeiteVon(rs.getInt("SeiteVon"));

                int seiteBis = rs.getInt("SeiteBis");

                if (!rs.wasNull()) {
                    he.setSeiteBis(seiteBis);
                }

                he.setSortierung(rs.getInt("Sortierung"));

                he.setBemerkung(rs.getString("Bemerkung"));

                he.setIstAktiv(rs.getBoolean("IstAktiv"));

                liste.add(he);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return liste;
    }

    public void insert(HeftEintrag eintrag) throws Exception {

        String sql = """
    INSERT INTO HeftEintrag
    (HeftID, HeftEintragTypID, Nro, Titel, Datum, SeiteVon, SeiteBis)
    VALUES (?, ?, ?, ?, ?, ?, ?)
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, eintrag.getHeftID());
            stmt.setInt(2, eintrag.getHeftEintragTypID());
            stmt.setString(3, eintrag.getNro());
            stmt.setString(4, eintrag.getTitel());

            if (eintrag.getDatum() != null) {
                stmt.setDate(5, java.sql.Date.valueOf(eintrag.getDatum()));
            } else {
                stmt.setNull(5, java.sql.Types.DATE);
            }

            stmt.setInt(6, eintrag.getSeiteVon());
            stmt.setInt(7, eintrag.getSeiteBis());

            stmt.executeUpdate();
        }
    }

    public void update(HeftEintrag eintrag) {

        String sql = """
        UPDATE dbo.HeftEintrag
        SET HeftID = ?,
            HeftEintragTypID = ?,
            Nro = ?,
            Titel = ?,
            Datum = ?,
            SeiteVon = ?,
            SeiteBis = ?
        WHERE HeftEintragID = ?
        """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, eintrag.getHeftID());
            stmt.setInt(2, eintrag.getHeftEintragTypID());

            stmt.setString(3, eintrag.getNro());
            stmt.setString(4, eintrag.getTitel());

            if (eintrag.getDatum() != null) {
                stmt.setDate(5, Date.valueOf(eintrag.getDatum()));
            } else {
                stmt.setNull(5, Types.DATE);
            }

            stmt.setInt(6, eintrag.getSeiteVon());
            stmt.setInt(7, eintrag.getSeiteBis());

            stmt.setInt(8, eintrag.getHeftEintragID());

            stmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Fehler beim Aktualisieren des HeftEintrags.", e);
        }
    }

    public boolean hatEintraegeZuHeft(int heftId) {
        String sql = """
        SELECT COUNT(*)
        FROM dbo.HeftEintrag
        WHERE HeftID = ?
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, heftId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    public List<HeftEintrag> findByTitelContains(String suchtext) throws SQLException {
        List<HeftEintrag> liste = new ArrayList<>();

        String sql = """
              SELECT HeftEintragID, HeftID, HeftEintragTypID, Nro, Titel, Datum,
                     SeiteVon, SeiteBis, Sortierung, Bemerkung, IstAktiv
              FROM dbo.HeftEintrag
              WHERE Titel IS NOT NULL
              AND LOWER(Titel) LIKE LOWER(?)
              ORDER BY Titel
              """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + suchtext + "%");

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HeftEintrag eintrag = new HeftEintrag();

                    eintrag.setHeftEintragID(rs.getInt("HeftEintragID"));
                    eintrag.setHeftID(rs.getInt("HeftID"));
                    eintrag.setHeftEintragTypID(rs.getInt("HeftEintragTypID"));
                    eintrag.setNro(rs.getString("Nro"));
                    eintrag.setTitel(rs.getString("Titel"));

                    if (rs.getDate("Datum") != null) {
                        eintrag.setDatum(rs.getDate("Datum").toLocalDate());
                    }

                    eintrag.setSeiteVon(rs.getInt("SeiteVon"));
                    eintrag.setSeiteBis(rs.getInt("SeiteBis"));
                    eintrag.setSortierung(rs.getInt("Sortierung"));
                    eintrag.setBemerkung(rs.getString("Bemerkung"));
                    eintrag.setIstAktiv(rs.getBoolean("IstAktiv"));

                    liste.add(eintrag);
                }
            }
        }

        return liste;
    }

    public List<HeftEintrag> findByTitelContainsAndBandId(String suchtext, int bandId) throws SQLException {
        List<HeftEintrag> liste = new ArrayList<>();

        String sql = """
    SELECT HeftEintragID, HeftID, HeftEintragTypID, Nro, Titel, Datum,
           SeiteVon, SeiteBis, Sortierung, Bemerkung, IstAktiv
    FROM dbo.HeftEintrag
    WHERE Titel IS NOT NULL
      AND LOWER(Titel) LIKE LOWER(?)
      AND HeftID IN (
          SELECT HeftID
          FROM dbo.Heft
          WHERE BandID = ?
      )
    ORDER BY Titel
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + suchtext + "%");
            stmt.setInt(2, bandId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HeftEintrag eintrag = new HeftEintrag();

                    eintrag.setHeftEintragID(rs.getInt("HeftEintragID"));
                    eintrag.setHeftID(rs.getInt("HeftID"));
                    eintrag.setHeftEintragTypID(rs.getInt("HeftEintragTypID"));
                    eintrag.setNro(rs.getString("Nro"));
                    eintrag.setTitel(rs.getString("Titel"));

                    if (rs.getDate("Datum") != null) {
                        eintrag.setDatum(rs.getDate("Datum").toLocalDate());
                    }

                    eintrag.setSeiteVon(rs.getInt("SeiteVon"));
                    eintrag.setSeiteBis(rs.getInt("SeiteBis"));
                    eintrag.setSortierung(rs.getInt("Sortierung"));
                    eintrag.setBemerkung(rs.getString("Bemerkung"));
                    eintrag.setIstAktiv(rs.getBoolean("IstAktiv"));

                    liste.add(eintrag);
                }
            }
        }

        return liste;
    }
}