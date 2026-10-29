package tankops.client.state;

import java.util.prefs.Preferences;

public class ClientConfig {
    private static final Preferences SETTINGS = Preferences.userNodeForPackage(ClientConfig.class);
    private static String username = readSetting("username", "Player1");
    private static String serverAddress = readSetting("serverAddress", "localhost:5555");

    private static String readSetting(String key, String fallback) {
        try { return SETTINGS.get(key, fallback); }
        catch (SecurityException error) { return fallback; }
    }

    public static void saveSettings() {
        try {
            SETTINGS.put("username", username);
            SETTINGS.put("serverAddress", serverAddress);
            SETTINGS.flush();
        } catch (java.util.prefs.BackingStoreException | SecurityException error) {
            System.err.println("Could not remember connection settings: " + error.getMessage());
        }
    }

    public static void setUsername(String name) {
        username = name.trim();
    }

    public static String getUsername() {
        return username;
    }

    public static void setServerAddress(String address) {
        serverAddress = normalizeServerAddress(address);
    }

    /** Tailscale IPv4 / MagicDNS hostname, with an optional port. No DNS lookup on the UI thread. */
    public static String normalizeServerAddress(String address) {
        String value = address == null || address.isBlank() ? "localhost:5555" : address.trim();
        String[] parts = value.split(":", -1);
        if (parts.length > 2 || !parts[0].matches("[a-zA-Z0-9][a-zA-Z0-9._-]*")) {
            throw new IllegalArgumentException("Nhập IP Tailscale hoặc tên máy, ví dụ 100.80.10.20:5555. Không nhập http://.");
        }
        int port = 5555;
        try {
            if (parts.length == 2) port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Cổng phải là số từ 1 đến 65535.");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Cổng phải là số từ 1 đến 65535.");
        }
        return parts[0] + ":" + port;
    }

    public static String getServerAddress() {
        return serverAddress;
    }

    public static String getHost() {
        return normalizeServerAddress(serverAddress).split(":")[0];
    }

    public static int getPort() {
        return Integer.parseInt(normalizeServerAddress(serverAddress).split(":")[1]);
    }
}
