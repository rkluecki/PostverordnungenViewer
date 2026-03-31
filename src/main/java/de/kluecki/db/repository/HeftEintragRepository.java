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
import de.kluecki.db.model.HeftEintragTyp;

public class HeftEintragRepository {

    private final Connection connection;

    public HeftEintragRepository(Connection connection) {
        this.connection = connection;
    }

    public List<HeftEintrag> findByHeft(int heftID) {

        List<HeftEintrag> liste = new ArrayList<>();

        String sql = """
    SELECT he.HeftEintragID,
           he.HeftID,
           he.HeftEintragTypID,
           het.Bezeichnung AS TypBezeichnung,
           he.Nro,
           he.Titel,
           he.Datum,
           he.SeiteVon,
           he.SeiteBis,
           he.Sortierung,
           he.Bemerkung,
           he.Forschungsnotiz,
           he.IstAktiv
    FROM dbo.HeftEintrag he
    LEFT JOIN dbo.HeftEintragTyp het
           ON he.HeftEintragTypID = het.HeftEintragTypID
    WHERE he.HeftID = ?
    ORDER BY he.SeiteVon
    """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setInt(1, heftID);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                HeftEintrag he = new HeftEintrag();

                he.setHeftEintragID(rs.getInt("HeftEintragID"));
                he.setHeftID(rs.getInt("HeftID"));
                he.setHeftEintragTypID(rs.getInt("HeftEintragTypID"));
                he.setTypBezeichnung(rs.getString("TypBezeichnung"));

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

                he.setForschungsnotiz(rs.getString("Forschungsnotiz"));

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
(HeftID, HeftEintragTypID, Nro, Titel, Datum, SeiteVon, SeiteBis, Forschungsnotiz)
VALUES (?, ?, ?, ?, ?, ?, ?, ?)
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
            stmt.setString(8, eintrag.getForschungsnotiz());

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
        SeiteBis = ?,
        Forschungsnotiz = ?
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
            stmt.setString(8, eintrag.getForschungsnotiz());

            stmt.setInt(9, eintrag.getHeftEintragID());

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
SELECT he.HeftEintragID,
       he.HeftID,
       he.HeftEintragTypID,
       he.Nro,
       he.Titel,
       he.Datum,
       he.SeiteVon,
       he.SeiteBis,
       he.Sortierung,
       he.Bemerkung,
       he.Forschungsnotiz,
       he.IstAktiv,
       q.Jahr AS BandJahr,
       q.Land AS GebietAnzeige,
       h.HeftNummer
FROM dbo.HeftEintrag he
INNER JOIN dbo.Heft h
    ON he.HeftID = h.HeftID
INNER JOIN dbo.Quelle q
    ON h.BandID = q.QuelleID
WHERE he.Titel IS NOT NULL
  AND LOWER(he.Titel) LIKE LOWER(?)
ORDER BY q.Land, q.Jahr, h.Sortierung, he.Sortierung, he.SeiteVon
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

                    int bandJahr = rs.getInt("BandJahr");
                    if (!rs.wasNull()) {
                        eintrag.setBandJahrAnzeige(String.valueOf(bandJahr));
                    }

                    eintrag.setGebietAnzeige(rs.getString("GebietAnzeige"));
                    eintrag.setHeftNummerAnzeige(rs.getString("HeftNummer"));

                    liste.add(eintrag);
                }
            }
        }

        return liste;
    }

    public List<HeftEintrag> findByTitelContainsAndBandId(String suchtext, int bandId) throws SQLException {
        List<HeftEintrag> liste = new ArrayList<>();

        String sql = """
SELECT he.HeftEintragID,
       he.HeftID,
       he.HeftEintragTypID,
       he.Nro,
       he.Titel,
       he.Datum,
       he.SeiteVon,
       he.SeiteBis,
       he.Sortierung,
       he.Bemerkung,
       he.IstAktiv,
       q.Jahr AS BandJahr,
       q.Land AS GebietAnzeige,
       h.HeftNummer
FROM dbo.HeftEintrag he
INNER JOIN dbo.Heft h
    ON he.HeftID = h.HeftID
INNER JOIN dbo.Quelle q
    ON h.BandID = q.QuelleID
WHERE he.Titel IS NOT NULL
  AND LOWER(he.Titel) LIKE LOWER(?)
  AND q.QuelleID = ?
ORDER BY q.Jahr, h.Sortierung, he.Sortierung, he.SeiteVon
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

                    int bandJahr = rs.getInt("BandJahr");
                    if (!rs.wasNull()) {
                        eintrag.setBandJahrAnzeige(String.valueOf(bandJahr));
                    }

                    eintrag.setGebietAnzeige(rs.getString("GebietAnzeige"));
                    eintrag.setHeftNummerAnzeige(rs.getString("HeftNummer"));

                    liste.add(eintrag);
                }
            }
        }

        return liste;
    }

    public List<HeftEintrag> findByTitelContainsAndGebiet(String suchtext, String gebiet) throws SQLException {
        List<HeftEintrag> liste = new ArrayList<>();

        String sql = """
    SELECT he.HeftEintragID,
           he.HeftID,
           he.HeftEintragTypID,
           he.Nro,
           he.Titel,
           he.Datum,
           he.SeiteVon,
           he.SeiteBis,
           he.Sortierung,
           he.Bemerkung,
           he.IstAktiv,
           q.Jahr AS BandJahr,
           q.Land AS GebietAnzeige,
           h.HeftNummer        
    FROM dbo.HeftEintrag he
    INNER JOIN dbo.Heft h
        ON he.HeftID = h.HeftID
    INNER JOIN dbo.Quelle q
        ON h.BandID = q.QuelleID
    WHERE he.Titel IS NOT NULL
      AND LOWER(he.Titel) LIKE LOWER(?)
      AND q.EbeneTyp = 'BAND'
      AND q.Land = ?
    ORDER BY q.Jahr, h.Sortierung, he.Sortierung, he.SeiteVon
    """;

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, "%" + suchtext + "%");
            stmt.setString(2, gebiet);

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

                    int seiteBis = rs.getInt("SeiteBis");
                    if (!rs.wasNull()) {
                        eintrag.setSeiteBis(seiteBis);
                    }

                    eintrag.setSortierung(rs.getInt("Sortierung"));
                    eintrag.setBemerkung(rs.getString("Bemerkung"));
                    eintrag.setIstAktiv(rs.getBoolean("IstAktiv"));

                    int bandJahr = rs.getInt("BandJahr");
                    if (!rs.wasNull()) {
                        eintrag.setBandJahrAnzeige(String.valueOf(bandJahr));
                    }

                    eintrag.setGebietAnzeige(rs.getString("GebietAnzeige"));

                    eintrag.setHeftNummerAnzeige(rs.getString("HeftNummer"));

                    liste.add(eintrag);
                }
            }
        }

        return liste;
    }

    public List<HeftEintragTyp> findAllTypen(){

        List<HeftEintragTyp> liste = new ArrayList<>();

        String sql = """
        SELECT HeftEintragTypID,
               Bezeichnung
        FROM HeftEintragTyp
        WHERE IstAktiv = 1
        ORDER BY Sortierung
    """;

        try(PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()){

            while(rs.next()){

                HeftEintragTyp typ = new HeftEintragTyp();

                typ.setHeftEintragTypID(
                        rs.getInt("HeftEintragTypID")
                );

                typ.setBezeichnung(
                        rs.getString("Bezeichnung")
                );

                liste.add(typ);
            }

        }catch(SQLException ex){
            ex.printStackTrace();
        }

        return liste;
    }
}