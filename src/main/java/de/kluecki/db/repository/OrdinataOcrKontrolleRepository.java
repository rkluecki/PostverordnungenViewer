package de.kluecki.db.repository;

import de.kluecki.db.model.OrdinataOcrKontrolle;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class OrdinataOcrKontrolleRepository {

    private final Connection connection;

    public OrdinataOcrKontrolleRepository(Connection connection) {
        this.connection = connection;
    }

    public List<OrdinataOcrKontrolle> findAll() {

        List<OrdinataOcrKontrolle> liste = new ArrayList<>();

        String sql = """
            SELECT
                OrdinataOcrKontrolleID,
                QuelleID,
                GebietBezeichnung,
                JahrVon,
                JahrBis,
                BandJahrAnzeige,
                UnterbandTitel,
                QuellenTitel,
                ArchivName,
                QuellenUrl,
                ManifestId,
                QuellenStatus,
                Erschliessbarkeit,
                OcrStatus,
                ImportStatus,
                PruefStatus,
                Prioritaet,
                SeitenGesamt,
                SeitenMitOcr,
                SeitenOhneOcr,
                NaechsterSchritt,
                Bemerkung,
                IstErledigt,
                ErstelltAm,
                GeaendertAm,
                ZuletztGeprueftAm
            FROM dbo.OrdinataOcrKontrolle
            ORDER BY
                IstErledigt,
                CASE Prioritaet
                    WHEN 'SEHR_HOCH' THEN 1
                    WHEN 'HOCH' THEN 2
                    WHEN 'MITTEL' THEN 3
                    WHEN 'NIEDRIG' THEN 4
                    ELSE 5
                END,
                GebietBezeichnung,
                JahrVon,
                JahrBis,
                OrdinataOcrKontrolleID
            """;

        try (var stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                OrdinataOcrKontrolle eintrag =
                        new OrdinataOcrKontrolle();

                eintrag.setOrdinataOcrKontrolleID(
                        rs.getInt("OrdinataOcrKontrolleID")
                );

                eintrag.setQuelleID(
                        (Integer) rs.getObject("QuelleID")
                );

                eintrag.setGebietBezeichnung(
                        rs.getString("GebietBezeichnung")
                );

                eintrag.setJahrVon(
                        (Integer) rs.getObject("JahrVon")
                );

                eintrag.setJahrBis(
                        (Integer) rs.getObject("JahrBis")
                );

                eintrag.setBandJahrAnzeige(
                        rs.getString("BandJahrAnzeige")
                );

                eintrag.setUnterbandTitel(
                        rs.getString("UnterbandTitel")
                );

                eintrag.setQuellenTitel(
                        rs.getString("QuellenTitel")
                );

                eintrag.setArchivName(
                        rs.getString("ArchivName")
                );

                eintrag.setQuellenUrl(
                        rs.getString("QuellenUrl")
                );

                eintrag.setManifestId(
                        rs.getString("ManifestId")
                );

                eintrag.setQuellenStatus(
                        rs.getString("QuellenStatus")
                );

                eintrag.setErschliessbarkeit(
                        rs.getString("Erschliessbarkeit")
                );

                eintrag.setOcrStatus(
                        rs.getString("OcrStatus")
                );

                eintrag.setImportStatus(
                        rs.getString("ImportStatus")
                );

                eintrag.setPruefStatus(
                        rs.getString("PruefStatus")
                );

                eintrag.setPrioritaet(
                        rs.getString("Prioritaet")
                );

                eintrag.setSeitenGesamt(
                        (Integer) rs.getObject("SeitenGesamt")
                );

                eintrag.setSeitenMitOcr(
                        (Integer) rs.getObject("SeitenMitOcr")
                );

                eintrag.setSeitenOhneOcr(
                        (Integer) rs.getObject("SeitenOhneOcr")
                );

                eintrag.setNaechsterSchritt(
                        rs.getString("NaechsterSchritt")
                );

                eintrag.setBemerkung(
                        rs.getString("Bemerkung")
                );

                eintrag.setIstErledigt(
                        rs.getBoolean("IstErledigt")
                );

                if (rs.getTimestamp("ErstelltAm") != null) {
                    eintrag.setErstelltAm(
                            rs.getTimestamp("ErstelltAm")
                                    .toLocalDateTime()
                    );
                }

                if (rs.getTimestamp("GeaendertAm") != null) {
                    eintrag.setGeaendertAm(
                            rs.getTimestamp("GeaendertAm")
                                    .toLocalDateTime()
                    );
                }

                if (rs.getTimestamp("ZuletztGeprueftAm") != null) {
                    eintrag.setZuletztGeprueftAm(
                            rs.getTimestamp("ZuletztGeprueftAm")
                                    .toLocalDateTime()
                    );
                }

                liste.add(eintrag);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return liste;
    }

    public void insert(OrdinataOcrKontrolle eintrag) {

        String sql = """
        INSERT INTO dbo.OrdinataOcrKontrolle
        (
            QuelleID,
            GebietBezeichnung,
            JahrVon,
            JahrBis,
            BandJahrAnzeige,
            UnterbandTitel,
            QuellenTitel,
            ArchivName,
            QuellenUrl,
            ManifestId,
            QuellenStatus,
            Erschliessbarkeit,
            OcrStatus,
            ImportStatus,
            PruefStatus,
            Prioritaet,
            SeitenGesamt,
            SeitenMitOcr,
            SeitenOhneOcr,
            NaechsterSchritt,
            Bemerkung,
            IstErledigt,
            GeaendertAm,
            ZuletztGeprueftAm
        )
        VALUES
        (
            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
            ?, ?, ?, ?
        )
        """;

        try (var stmt = connection.prepareStatement(sql)) {

            if (eintrag.getQuelleID() != null) {
                stmt.setInt(1, eintrag.getQuelleID());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }

            stmt.setString(2, eintrag.getGebietBezeichnung());

            setNullableInteger(stmt, 3, eintrag.getJahrVon());
            setNullableInteger(stmt, 4, eintrag.getJahrBis());

            stmt.setString(5, eintrag.getBandJahrAnzeige());
            stmt.setString(6, eintrag.getUnterbandTitel());
            stmt.setString(7, eintrag.getQuellenTitel());
            stmt.setString(8, eintrag.getArchivName());
            stmt.setString(9, eintrag.getQuellenUrl());
            stmt.setString(10, eintrag.getManifestId());

            stmt.setString(11, eintrag.getQuellenStatus());
            stmt.setString(12, eintrag.getErschliessbarkeit());
            stmt.setString(13, eintrag.getOcrStatus());
            stmt.setString(14, eintrag.getImportStatus());
            stmt.setString(15, eintrag.getPruefStatus());
            stmt.setString(16, eintrag.getPrioritaet());

            setNullableInteger(stmt, 17, eintrag.getSeitenGesamt());
            setNullableInteger(stmt, 18, eintrag.getSeitenMitOcr());
            setNullableInteger(stmt, 19, eintrag.getSeitenOhneOcr());

            stmt.setString(20, eintrag.getNaechsterSchritt());
            stmt.setString(21, eintrag.getBemerkung());
            stmt.setBoolean(22, eintrag.isIstErledigt());

            if (eintrag.getGeaendertAm() != null) {
                stmt.setTimestamp(
                        23,
                        java.sql.Timestamp.valueOf(eintrag.getGeaendertAm())
                );
            } else {
                stmt.setNull(23, java.sql.Types.TIMESTAMP);
            }

            if (eintrag.getZuletztGeprueftAm() != null) {
                stmt.setTimestamp(
                        24,
                        java.sql.Timestamp.valueOf(
                                eintrag.getZuletztGeprueftAm()
                        )
                );
            } else {
                stmt.setNull(24, java.sql.Types.TIMESTAMP);
            }

            stmt.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(
                    "OCR-Kontrolllisteneintrag konnte nicht gespeichert werden.",
                    e
            );
        }
    }

    public void update(OrdinataOcrKontrolle eintrag) {

        String sql = """
        UPDATE dbo.OrdinataOcrKontrolle
        SET
            QuelleID = ?,
            GebietBezeichnung = ?,
            JahrVon = ?,
            JahrBis = ?,
            BandJahrAnzeige = ?,
            UnterbandTitel = ?,
            QuellenTitel = ?,
            ArchivName = ?,
            QuellenUrl = ?,
            ManifestId = ?,
            QuellenStatus = ?,
            Erschliessbarkeit = ?,
            OcrStatus = ?,
            ImportStatus = ?,
            PruefStatus = ?,
            Prioritaet = ?,
            SeitenGesamt = ?,
            SeitenMitOcr = ?,
            SeitenOhneOcr = ?,
            NaechsterSchritt = ?,
            Bemerkung = ?,
            IstErledigt = ?,
            GeaendertAm = SYSDATETIME(),
            ZuletztGeprueftAm = ?
        WHERE OrdinataOcrKontrolleID = ?
        """;

        try (var stmt = connection.prepareStatement(sql)) {

            if (eintrag.getQuelleID() != null) {
                stmt.setInt(1, eintrag.getQuelleID());
            } else {
                stmt.setNull(1, java.sql.Types.INTEGER);
            }

            stmt.setString(2, eintrag.getGebietBezeichnung());

            setNullableInteger(stmt, 3, eintrag.getJahrVon());
            setNullableInteger(stmt, 4, eintrag.getJahrBis());

            stmt.setString(5, eintrag.getBandJahrAnzeige());
            stmt.setString(6, eintrag.getUnterbandTitel());
            stmt.setString(7, eintrag.getQuellenTitel());
            stmt.setString(8, eintrag.getArchivName());
            stmt.setString(9, eintrag.getQuellenUrl());
            stmt.setString(10, eintrag.getManifestId());

            stmt.setString(11, eintrag.getQuellenStatus());
            stmt.setString(12, eintrag.getErschliessbarkeit());
            stmt.setString(13, eintrag.getOcrStatus());
            stmt.setString(14, eintrag.getImportStatus());
            stmt.setString(15, eintrag.getPruefStatus());
            stmt.setString(16, eintrag.getPrioritaet());

            setNullableInteger(stmt, 17, eintrag.getSeitenGesamt());
            setNullableInteger(stmt, 18, eintrag.getSeitenMitOcr());
            setNullableInteger(stmt, 19, eintrag.getSeitenOhneOcr());

            stmt.setString(20, eintrag.getNaechsterSchritt());
            stmt.setString(21, eintrag.getBemerkung());
            stmt.setBoolean(22, eintrag.isIstErledigt());

            if (eintrag.getZuletztGeprueftAm() != null) {
                stmt.setTimestamp(
                        23,
                        java.sql.Timestamp.valueOf(
                                eintrag.getZuletztGeprueftAm()
                        )
                );
            } else {
                stmt.setNull(23, java.sql.Types.TIMESTAMP);
            }

            stmt.setInt(
                    24,
                    eintrag.getOrdinataOcrKontrolleID()
            );

            int geaenderteZeilen = stmt.executeUpdate();

            if (geaenderteZeilen == 0) {
                throw new RuntimeException(
                        "Der OCR-Kontrolllisteneintrag wurde nicht gefunden."
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "OCR-Kontrolllisteneintrag konnte nicht aktualisiert werden.",
                    e
            );
        }
    }

    public void delete(int ordinataOcrKontrolleID) {

        String sql = """
        DELETE FROM dbo.OrdinataOcrKontrolle
        WHERE OrdinataOcrKontrolleID = ?
        """;

        try (var stmt = connection.prepareStatement(sql)) {

            stmt.setInt(1, ordinataOcrKontrolleID);

            int geloeschteZeilen = stmt.executeUpdate();

            if (geloeschteZeilen == 0) {
                throw new RuntimeException(
                        "Der OCR-Kontrolllisteneintrag wurde nicht gefunden."
                );
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "OCR-Kontrolllisteneintrag konnte nicht gelöscht werden.",
                    e
            );
        }
    }

    private void setNullableInteger(
            java.sql.PreparedStatement stmt,
            int parameterIndex,
            Integer wert) throws java.sql.SQLException {

        if (wert != null) {
            stmt.setInt(parameterIndex, wert);
        } else {
            stmt.setNull(parameterIndex, java.sql.Types.INTEGER);
        }
    }
}