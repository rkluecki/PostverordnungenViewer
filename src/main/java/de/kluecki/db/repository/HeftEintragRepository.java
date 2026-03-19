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
}