package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.model.HilfeHinweis;
import de.kluecki.db.model.HilfeSchritt;
import de.kluecki.db.model.HilfeThema;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class HilfeRepository {


    public List<HilfeThema> findAllThemen() {
        List<HilfeThema> liste = new ArrayList<>();

        String sql = """
        SELECT HilfeThemaID, Titel, Sortierung, IstAktiv
        FROM dbo.HilfeThema
        WHERE IstAktiv = 1
        ORDER BY Sortierung, Titel
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                HilfeThema t = new HilfeThema();
                t.setHilfeThemaID(rs.getInt("HilfeThemaID"));
                t.setTitel(rs.getString("Titel"));
                t.setSortierung(rs.getInt("Sortierung"));
                t.setIstAktiv(rs.getBoolean("IstAktiv"));
                liste.add(t);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<HilfeSchritt> findSchritteByThemaId(int themaId) {
        List<HilfeSchritt> liste = new ArrayList<>();

        String sql = """
        SELECT HilfeSchrittID, HilfeThemaID, Reihenfolge, Text, IstAktiv
        FROM dbo.HilfeSchritt
        WHERE HilfeThemaID = ?
          AND IstAktiv = 1
        ORDER BY Reihenfolge, HilfeSchrittID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, themaId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HilfeSchritt s = new HilfeSchritt();
                    s.setHilfeSchrittID(rs.getInt("HilfeSchrittID"));
                    s.setHilfeThemaID(rs.getInt("HilfeThemaID"));
                    s.setReihenfolge(rs.getInt("Reihenfolge"));
                    s.setText(rs.getString("Text"));
                    s.setIstAktiv(rs.getBoolean("IstAktiv"));
                    liste.add(s);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<HilfeHinweis> findHinweiseBySchrittId(int schrittId) {
        List<HilfeHinweis> liste = new ArrayList<>();

        String sql = """
        SELECT HilfeHinweisID, HilfeSchrittID, Typ, Text, IstAktiv
        FROM dbo.HilfeHinweis
        WHERE HilfeSchrittID = ?
          AND IstAktiv = 1
        ORDER BY HilfeHinweisID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, schrittId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HilfeHinweis h = new HilfeHinweis();
                    h.setHilfeHinweisID(rs.getInt("HilfeHinweisID"));
                    h.setHilfeSchrittID(rs.getInt("HilfeSchrittID"));
                    h.setTyp(rs.getString("Typ"));
                    h.setText(rs.getString("Text"));
                    h.setIstAktiv(rs.getBoolean("IstAktiv"));
                    liste.add(h);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<HilfeThema> findAlleThemen() {
        List<HilfeThema> liste = new ArrayList<>();

        String sql = """
            SELECT HilfeThemaID, Titel, Sortierung, IstAktiv
            FROM HilfeThema
            WHERE IstAktiv = 1
            ORDER BY Sortierung, Titel
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                HilfeThema thema = new HilfeThema();
                thema.setHilfeThemaID(rs.getInt("HilfeThemaID"));
                thema.setTitel(rs.getString("Titel"));
                thema.setSortierung(rs.getInt("Sortierung"));
                thema.setIstAktiv(rs.getBoolean("IstAktiv"));
                liste.add(thema);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public List<HilfeSchritt> findSchritteZuThema(int hilfeThemaId) {
        List<HilfeSchritt> liste = new ArrayList<>();

        String sql = """
            SELECT HilfeSchrittID, HilfeThemaID, Reihenfolge, Text, IstAktiv
            FROM HilfeSchritt
            WHERE HilfeThemaID = ?
              AND IstAktiv = 1
            ORDER BY Reihenfolge
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, hilfeThemaId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    HilfeSchritt schritt = new HilfeSchritt();
                    schritt.setHilfeSchrittID(rs.getInt("HilfeSchrittID"));
                    schritt.setHilfeThemaID(rs.getInt("HilfeThemaID"));
                    schritt.setReihenfolge(rs.getInt("Reihenfolge"));
                    schritt.setText(rs.getString("Text"));
                    schritt.setIstAktiv(rs.getBoolean("IstAktiv"));
                    liste.add(schritt);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public void insertThema(HilfeThema thema) {

        String sql = """
            INSERT INTO HilfeThema (Titel, Sortierung, IstAktiv)
            VALUES (?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, thema.getTitel());
            stmt.setInt(2, thema.getSortierung());
            stmt.setBoolean(3, thema.isIstAktiv());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void insertSchritt(HilfeSchritt schritt) {

        String sql = """
            INSERT INTO HilfeSchritt (HilfeThemaID, Reihenfolge, Text, IstAktiv)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, schritt.getHilfeThemaID());
            stmt.setInt(2, schritt.getReihenfolge());
            stmt.setString(3, schritt.getText());
            stmt.setBoolean(4, schritt.isIstAktiv());

            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteSchrittById(int hilfeSchrittId) {

        String sql = "DELETE FROM HilfeSchritt WHERE HilfeSchrittID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, hilfeSchrittId);
            stmt.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deleteThemaById(int themaId) {

        String sql = "DELETE FROM HilfeThema WHERE HilfeThemaID = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, themaId);
            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateSchritt(HilfeSchritt schritt) {

        String sql = """
    UPDATE HilfeSchritt
    SET Reihenfolge = ?, Text = ?
    WHERE HilfeSchrittID = ?
    """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, schritt.getReihenfolge());
            ps.setString(2, schritt.getText());
            ps.setInt(3, schritt.getHilfeSchrittID());

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
