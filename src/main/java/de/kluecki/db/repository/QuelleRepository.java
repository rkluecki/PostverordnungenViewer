package de.kluecki.db.repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import de.kluecki.db.DatabaseConnection;
import de.kluecki.db.Quelle;
import de.kluecki.db.model.Veroeffentlichung;
import java.sql.PreparedStatement;
import java.sql.Date;

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

    public List<Veroeffentlichung> findVeroeffentlichungenByBand(String gebiet, String band) {
        List<Veroeffentlichung> liste = new ArrayList<>();

        String sql = """
        SELECT
            QuelleID,
            Nummer,
            Titel,
            DatumVon,
            Jahr,
            SeiteVon,
            SeiteBis,
            StatusID,
            EbeneSortierung
        FROM Quelle
        WHERE EbeneTyp = 'VEROEFFENTLICHUNG'
          AND Land = ?
          AND Jahr = ?
        ORDER BY
            CASE WHEN EbeneSortierung IS NULL THEN 1 ELSE 0 END,
            EbeneSortierung,
            CASE WHEN SeiteVon IS NULL THEN 1 ELSE 0 END,
            SeiteVon,
            QuelleID
        """;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, gebiet);
            ps.setInt(2, Integer.parseInt(band));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Veroeffentlichung v = new Veroeffentlichung();

                    v.setQuelleId(rs.getInt("QuelleID"));
                    v.setNummer(rs.getString("Nummer"));
                    v.setTitel(rs.getString("Titel"));

                    Date datumVon = rs.getDate("DatumVon");
                    if (datumVon != null) {
                        v.setDatum(datumVon.toString());
                    } else {
                        int jahr = rs.getInt("Jahr");
                        v.setDatum(jahr > 0 ? String.valueOf(jahr) : "");
                    }

                    Integer seiteVon = (Integer) rs.getObject("SeiteVon");
                    Integer seiteBis = (Integer) rs.getObject("SeiteBis");

                    v.setSeiteVon(seiteVon != null ? seiteVon : 0);
                    v.setSeiteBis(seiteBis != null ? seiteBis : 0);

                    Object statusObj = rs.getObject("StatusID");
                    v.setStatus(statusObj != null ? String.valueOf(statusObj) : "");

                    Integer ebeneSortierung = (Integer) rs.getObject("EbeneSortierung");
                    v.setEbeneSortierung(ebeneSortierung != null ? ebeneSortierung : 0);

                    liste.add(v);
                }
            }

        } catch (NumberFormatException ex) {
            System.out.println("Band/Jahr ist keine Zahl: " + band);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return liste;
    }
}