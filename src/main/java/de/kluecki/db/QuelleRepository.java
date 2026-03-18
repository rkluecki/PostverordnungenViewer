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

    public Integer findeQuelleId(int seiteVon, int seiteBis){

        String sql = """
        SELECT QuelleID
        FROM Quelle
        WHERE EbeneTyp = 'EINTRAG'
        AND SeiteVon <= ?
        AND SeiteBis >= ?
    """;

        try(Connection con = DatabaseConnection.getConnection();
            java.sql.PreparedStatement ps = con.prepareStatement(sql)){

            ps.setInt(1, seiteVon);
            ps.setInt(2, seiteBis);

            ResultSet rs = ps.executeQuery();

            if(rs.next()){
                return rs.getInt("QuelleID");
            }

        }catch(Exception ex){
            ex.printStackTrace();
        }

        return null;
    }
}