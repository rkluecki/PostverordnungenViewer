package de.kluecki.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class QuelleRepository {

    public List<Quelle> findAll() {
        List<Quelle> quellen = new ArrayList<>();

        String sql = "SELECT QuelleID, ParentQuelleID, QuelleTypID, Titel FROM Quelle ORDER BY QuelleID";

        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                int quelleID = rs.getInt("QuelleID");

                Integer parentQuelleID = (Integer) rs.getObject("ParentQuelleID");

                int quelleTypID = rs.getInt("QuelleTypID");
                String titel = rs.getString("Titel");

                Quelle quelle = new Quelle(quelleID, parentQuelleID, quelleTypID, titel);
                quellen.add(quelle);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return quellen;
    }
}