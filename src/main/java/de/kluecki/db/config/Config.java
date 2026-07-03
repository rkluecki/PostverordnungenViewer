package de.kluecki.db.config;

import java.util.prefs.Preferences;

public class Config {

    private static final String KEY_IMAGE_ROOT_PATH = "imageRootPath";
    private static final String KEY_BACKUP_PATH = "backupPath";

    private static final String DEFAULT_IMAGE_ROOT_PATH =
            "D:\\Historische_PO_Daten\\Verordnungen";

    private static final String DEFAULT_BACKUP_PATH =
            "D:\\Historische_PO_Daten\\Backup\\Ordinata";

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(Config.class);

    public static String getImageRootPath() {
        return PREFS.get(KEY_IMAGE_ROOT_PATH, DEFAULT_IMAGE_ROOT_PATH);
    }

    public static void setImageRootPath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        PREFS.put(KEY_IMAGE_ROOT_PATH, path.trim());
    }

    public static String getDefaultImageRootPath() {
        return DEFAULT_IMAGE_ROOT_PATH;
    }

    public static String getBackupPath() {
        return PREFS.get(KEY_BACKUP_PATH, DEFAULT_BACKUP_PATH);
    }

    public static void setBackupPath(String path) {
        if (path == null || path.isBlank()) {
            return;
        }

        PREFS.put(KEY_BACKUP_PATH, path.trim());
    }

    public static String getDefaultBackupPath() {
        return DEFAULT_BACKUP_PATH;
    }
}