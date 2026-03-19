package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.InhaltTabellenEintrag;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InhaltseinheitRepository {

    public void insert(
            int verordnungBetreffId,
            int lfdNr,
            String ueberschrift,
            int inhaltstypId,
            int seiteVon,
            Integer seiteBis,
            String bemerkung
    ) {
        String sql = """
            INSERT INTO Inhaltseinheit
            (VerordnungBetreffID, LfdNr, Ueberschrift, InhaltstypID, SeiteVon, SeiteBis, Bemerkung, IstUnsicher, ErstelltAm)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, SYSDATETIME())
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, verordnungBetreffId);
            ps.setInt(2, lfdNr);
            ps.setString(3, ueberschrift);
            ps.setInt(4, inhaltstypId);
            ps.setInt(5, seiteVon);

            if (seiteBis == null) {
                ps.setNull(6, java.sql.Types.INTEGER);
            } else {
                ps.setInt(6, seiteBis);
            }

            ps.setString(7, bemerkung);

            ps.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Integer findInhaltstypIdByName(String name) {

        String sql = "SELECT InhaltstypID FROM Inhaltstyp WHERE Bezeichnung = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return rs.getInt("InhaltstypID");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return null;
    }

    public java.util.List<InhaltTabellenEintrag> findByVerordnungBetreffId(int verordnungBetreffId) {

        java.util.List<InhaltTabellenEintrag> liste = new java.util.ArrayList<>();

        String sql = """
        SELECT ie.LfdNr,
               ie.Ueberschrift,
               ie.SeiteVon,
               ie.SeiteBis,
               it.Bezeichnung AS Typ,
               ie.Bemerkung
        FROM Inhaltseinheit ie
        LEFT JOIN Inhaltstyp it ON ie.InhaltstypID = it.InhaltstypID
        WHERE ie.VerordnungBetreffID = ?
        ORDER BY ie.LfdNr
    """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, verordnungBetreffId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String nr = String.valueOf(rs.getInt("LfdNr"));
                String titel = rs.getString("Ueberschrift");

                int seiteVon = rs.getInt("SeiteVon");
                Integer seiteBis = (Integer) rs.getObject("SeiteBis");

                String seite;
                if (seiteBis == null || seiteBis == seiteVon) {
                    seite = String.valueOf(seiteVon);
                } else {
                    seite = seiteVon + "-" + seiteBis;
                }

                String typ = rs.getString("Typ");
                String beschreibung = rs.getString("Bemerkung");

                liste.add(new InhaltTabellenEintrag(nr, titel, seite, typ, beschreibung));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return liste;
    }

    public List<InhaltTabellenEintrag> findByQuelleId(int quelleId) {
        return new ArrayList<>();
    }
}