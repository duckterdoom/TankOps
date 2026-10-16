package tankops.client.state;

public class ClientConfig {
    private static String username = "Player1";
    private static String serverAddress = "localhost:5555";

    public static void setUsername(String name) {
        username = name.trim();
    }

    public static String getUsername() {
        return username;
    }

    public static void setServerAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            serverAddress = "localhost:5555";
        } else {
            serverAddress = address.trim();
        }
    }

    public static String getServerAddress() {
        return serverAddress;
    }

    public static String getHost() {
        if (serverAddress.contains(":")) {
            return serverAddress.split(":")[0];
        }
        return serverAddress;
    }

    public static int getPort() {
        if (serverAddress.contains(":")) {
            try {
                return Integer.parseInt(serverAddress.split(":")[1]);
            } catch (NumberFormatException e) {
                return 5555;
            }
        }

        return 5555;
    }
}
