package de.kluecki.db.repository;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.InhaltTabellenEintrag;
import de.kluecki.db.model.Inhaltstyp;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class InhaltseinheitRepository {

    public void insert(
            int heftEintragId,
            int lfdNr,
            String ueberschrift,
            int inhaltstypId,
            int seiteVon,
            Integer seiteBis,
            String bemerkung
    ) {
        String sql = """
            INSERT INTO Inhaltseinheit
            (HeftEintragID, LfdNr, Ueberschrift, InhaltstypID, SeiteVon, SeiteBis, Bemerkung, IstUnsicher, ErstelltAm)
            VALUES (?, ?, ?, ?, ?, ?, ?, 0, SYSDATETIME())
        """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, heftEintragId);
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

    public List<InhaltTabellenEintrag> findByHeftEintragId(int heftEintragId) {

        List<InhaltTabellenEintrag> liste = new ArrayList<>();

        String sql = """
           SELECT ie.LfdNr,
                  ie.Ueberschrift,
                  ie.SeiteVon,
                  ie.SeiteBis,
                  ie.InhaltstypID,
                  it.Bezeichnung AS Typ,
                  ie.Bemerkung,
                  qBand.Region AS Gebiet,
                  qBand.Jahr AS BandJahr
         FROM Inhaltseinheit ie
         INNER JOIN HeftEintrag he ON ie.HeftEintragID = he.HeftEintragID
         INNER JOIN Heft h ON he.HeftID = h.HeftID
         INNER JOIN Quelle qBand ON h.BandID = qBand.QuelleID
         LEFT JOIN Inhaltstyp it ON ie.InhaltstypID = it.InhaltstypID
         WHERE ie.HeftEintragID = ?
         ORDER BY ie.LfdNr
""";

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, heftEintragId);

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

                int inhaltstypId = rs.getInt("InhaltstypID");
                String typ = rs.getString("Typ");
                String beschreibung = rs.getString("Bemerkung");
                String gebiet = rs.getString("Gebiet");
                String bandJahr = String.valueOf(rs.getInt("BandJahr"));

                Inhaltstyp typObj = new Inhaltstyp(inhaltstypId, typ);

                liste.add(new InhaltTabellenEintrag(
                        nr,
                        titel,
                        seite,
                        typObj,
                        beschreibung,
                        gebiet,
                        bandJahr,
                        seiteVon,
                        seiteBis
                ));
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return liste;
    }

    public void deleteByHeftEintragIdUndLfdNr(int heftEintragId, int lfdNr) {

        String sql = """
        DELETE FROM Inhaltseinheit
        WHERE HeftEintragID = ? AND LfdNr = ?
    """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setInt(1, heftEintragId);
            ps.setInt(2, lfdNr);

            ps.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void update(
            int heftEintragId,
            int lfdNr,
            String ueberschrift,
            int inhaltstypId,
            int seiteVon,
            Integer seiteBis,
            String bemerkung
         ) {

        String sql = """
        UPDATE Inhaltseinheit
        SET Ueberschrift = ?,
            InhaltstypID = ?,
            SeiteVon = ?,
            SeiteBis = ?,
            Bemerkung = ?
        WHERE HeftEintragID = ?
        AND LfdNr = ?
    """;

        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, ueberschrift);
            ps.setInt(2, inhaltstypId);
            ps.setInt(3, seiteVon);

            if (seiteBis == null) {
                ps.setNull(4, java.sql.Types.INTEGER);
            } else {
                ps.setInt(4, seiteBis);
            }

            ps.setString(5, bemerkung);

            ps.setInt(6, heftEintragId);
            ps.setInt(7, lfdNr);

            ps.executeUpdate();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}