/*
 * Übergangsstruktur / Repository
 *
 * Zweck:
 * Lädt VerordnungBetreff Daten aus der Datenbank.
 *
 * Rolle im Projekt:
 * Wird noch von der bestehenden UI verwendet.
 *
 * Später:
 * Funktion wird vermutlich durch HeftEintrag ersetzt.
 */

package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.VerordnungBetreff;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VerordnungBetreffRepository {

    private final Connection connection;

    public VerordnungBetreffRepository(Connection connection) {
        this.connection = connection;
    }

    public List<VerordnungBetreff> findByBand(String gebiet, String bandJahr) throws SQLException {

        String sql = """
            SELECT *
            FROM VerordnungBetreff
            WHERE Gebiet = ?
            AND BandJahr = ?
            ORDER BY SeiteVon
            """;

        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setString(1, gebiet);
        stmt.setString(2, bandJahr);

        ResultSet rs = stmt.executeQuery();

        List<VerordnungBetreff> liste = new ArrayList<>();

        while (rs.next()) {

            VerordnungBetreff v = new VerordnungBetreff();

            v.setVerordnungBetreffID(rs.getInt("VerordnungBetreffID"));
            v.setGebiet(rs.getString("Gebiet"));
            v.setBandJahr(rs.getString("BandJahr"));

            v.setSeiteVon(rs.getInt("SeiteVon"));
            v.setSeiteBis(rs.getInt("SeiteBis"));

            v.setTitel(rs.getString("Titel"));
            v.setBemerkung(rs.getString("Bemerkung"));

            liste.add(v);
        }

        return liste;
    }

    public VerordnungBetreff findForPage(String gebiet, String bandJahr, int seite) throws SQLException {

        String sql = """
            SELECT TOP 1 *
            FROM VerordnungBetreff
            WHERE Gebiet = ?
            AND BandJahr = ?
            AND ? BETWEEN SeiteVon AND SeiteBis
            ORDER BY SeiteVon
            """;

        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setString(1, gebiet);
        stmt.setString(2, bandJahr);
        stmt.setInt(3, seite);

        ResultSet rs = stmt.executeQuery();

        if (rs.next()) {
            VerordnungBetreff v = new VerordnungBetreff();

            v.setVerordnungBetreffID(rs.getInt("VerordnungBetreffID"));
            v.setGebiet(rs.getString("Gebiet"));
            v.setBandJahr(rs.getString("BandJahr"));

            v.setSeiteVon(rs.getInt("SeiteVon"));
            v.setSeiteBis(rs.getInt("SeiteBis"));

            v.setTitel(rs.getString("Titel"));
            v.setBemerkung(rs.getString("Bemerkung"));

            Object heftEintragObj = rs.getObject("HeftEintragID");

            if (heftEintragObj != null) {
                v.setHeftEintragID((Integer) heftEintragObj);
            }

            return v;
        }

        return null;
    }

    public void insert(VerordnungBetreff betreff) throws SQLException {

        String sql = """
            
            INSERT INTO VerordnungBetreff
               (
                   Gebiet,
                   BandJahr,
                   SeiteVon,
                   SeiteBis,
                   Titel,
                   Bemerkung,
                   QuelleID,
                   HeftEintragID
               )
                   VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        PreparedStatement stmt = connection.prepareStatement(sql);

        stmt.setString(1, betreff.getGebiet());
        stmt.setString(2, betreff.getBandJahr());
        stmt.setInt(3, betreff.getSeiteVon());
        stmt.setInt(4, betreff.getSeiteBis());
        stmt.setString(5, betreff.getTitel());
        stmt.setString(6, betreff.getBemerkung());
        stmt.setInt(7, betreff.getQuelleID());

        if (betreff.getHeftEintragID() != null) {
            stmt.setInt(8, betreff.getHeftEintragID());
        } else {
            stmt.setNull(8, Types.INTEGER);
        }

        stmt.executeUpdate();
    }

    public void update(VerordnungBetreff betreff) throws SQLException {
        String sql = """
            UPDATE VerordnungBetreff
            SET Titel = ?, Bemerkung = ?
            WHERE VerordnungBetreffID = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, betreff.getTitel());
            ps.setString(2, betreff.getBemerkung());
            ps.setInt(3, betreff.getVerordnungBetreffID());

            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {

        String sql = """
            DELETE FROM VerordnungBetreff
            WHERE VerordnungBetreffID = ?
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);

            ps.executeUpdate();
        }
    }

    public List<VerordnungBetreff> search(String gebiet,
                                          String bandJahrSuchbegriff,
                                          String titelSuchbegriff) throws SQLException {

        StringBuilder sql = new StringBuilder("""
        SELECT *
        FROM VerordnungBetreff
        WHERE 1 = 1
        """);

        List<Object> parameter = new ArrayList<>();

        if (gebiet != null && !gebiet.isBlank() && !gebiet.equalsIgnoreCase("Alle")) {
            sql.append(" AND Gebiet = ? ");
            parameter.add(gebiet);
        }

        if (bandJahrSuchbegriff != null && !bandJahrSuchbegriff.isBlank()) {
            String suchmuster = bandJahrSuchbegriff.trim()
                    .replace("*", "%")
                    .replace("?", "_");

            sql.append(" AND BandJahr LIKE ? ");
            parameter.add(suchmuster);
        }

        if (titelSuchbegriff != null && !titelSuchbegriff.isBlank()) {
            String suchmuster = titelSuchbegriff.trim()
                    .replace("*", "%")
                    .replace("?", "_");

            sql.append(" AND Titel LIKE ? ");
            parameter.add(suchmuster);
        }

        sql.append("""
        ORDER BY Gebiet, BandJahr, SeiteVon
        """);

        PreparedStatement stmt = connection.prepareStatement(sql.toString());

        for (int i = 0; i < parameter.size(); i++) {
            stmt.setObject(i + 1, parameter.get(i));
        }

        ResultSet rs = stmt.executeQuery();

        List<VerordnungBetreff> liste = new ArrayList<>();

        while (rs.next()) {
            VerordnungBetreff v = new VerordnungBetreff();

            v.setVerordnungBetreffID(rs.getInt("VerordnungBetreffID"));
            v.setGebiet(rs.getString("Gebiet"));
            v.setBandJahr(rs.getString("BandJahr"));
            v.setSeiteVon(rs.getInt("SeiteVon"));
            v.setSeiteBis(rs.getInt("SeiteBis"));
            v.setTitel(rs.getString("Titel"));
            v.setBemerkung(rs.getString("Bemerkung"));

            liste.add(v);
        }

        return liste;
    }

    public List<VerordnungBetreff> findByQuelleId(int quelleId) throws SQLException {

        String sql = """
        SELECT *
        FROM VerordnungBetreff
        WHERE QuelleID = ?
        ORDER BY SeiteVon
        """;

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, quelleId);

        ResultSet rs = stmt.executeQuery();

        List<VerordnungBetreff> liste = new ArrayList<>();

        while (rs.next()) {

            VerordnungBetreff v = new VerordnungBetreff();

            v.setVerordnungBetreffID(rs.getInt("VerordnungBetreffID"));
            v.setGebiet(rs.getString("Gebiet"));
            v.setBandJahr(rs.getString("BandJahr"));
            v.setSeiteVon(rs.getInt("SeiteVon"));
            v.setSeiteBis(rs.getInt("SeiteBis"));
            v.setTitel(rs.getString("Titel"));
            v.setBemerkung(rs.getString("Bemerkung"));

            Object heftEintragObj = rs.getObject("HeftEintragID");

            if (heftEintragObj != null) {
                v.setHeftEintragID((Integer) heftEintragObj);
            }

            Object quelleIdObj = rs.getObject("QuelleID");
            if (quelleIdObj != null) {
                v.setQuelleID((Integer) quelleIdObj);
            }

            liste.add(v);
        }

        return liste;
    }

    public List<VerordnungBetreff> findByHeftEintragId(int heftEintragId) throws SQLException {

        String sql = """
        SELECT *
        FROM VerordnungBetreff
        WHERE HeftEintragID = ?
        ORDER BY SeiteVon
        """;

        PreparedStatement stmt = connection.prepareStatement(sql);
        stmt.setInt(1, heftEintragId);

        ResultSet rs = stmt.executeQuery();

        List<VerordnungBetreff> liste = new ArrayList<>();

        while (rs.next()) {

            VerordnungBetreff v = new VerordnungBetreff();

            v.setVerordnungBetreffID(rs.getInt("VerordnungBetreffID"));
            v.setGebiet(rs.getString("Gebiet"));
            v.setBandJahr(rs.getString("BandJahr"));
            v.setSeiteVon(rs.getInt("SeiteVon"));
            v.setSeiteBis(rs.getInt("SeiteBis"));
            v.setTitel(rs.getString("Titel"));
            v.setBemerkung(rs.getString("Bemerkung"));

            Object heftEintragObj = rs.getObject("HeftEintragID");
            if (heftEintragObj != null) {
                v.setHeftEintragID((Integer) heftEintragObj);
            }

            Object quelleIdObj = rs.getObject("QuelleID");
            if (quelleIdObj != null) {
                v.setQuelleID((Integer) quelleIdObj);
            }

            liste.add(v);
        }

        return liste;
    }
}