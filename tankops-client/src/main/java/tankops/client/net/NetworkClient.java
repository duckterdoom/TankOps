package tankops.client.net;

public class NetworkClient {
    private static NetworkClient instance;

    public static NetworkClient getInstance() {
        if (instance == null) {
            instance = new NetworkClient();
        } return instance;
    }

    public boolean connect(String host, int port) {
        System.out.println("Connecting to " + host + ":" + port);
        return false;
    }
}
