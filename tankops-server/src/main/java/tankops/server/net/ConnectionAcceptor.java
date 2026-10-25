package tankops.server.net;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Chạy trên thread riêng, chỉ làm một việc: chờ client mới và giao cho ClientSession.
 * Phải tách thread vì accept() là lệnh "chờ": nó đứng yên cho tới khi có client kết nối.
 */
public class ConnectionAcceptor implements Runnable {

    private final ServerSocket serverSocket;
    private final GameServer gameServer;

    public ConnectionAcceptor(ServerSocket serverSocket, GameServer gameServer) {
        this.serverSocket = serverSocket;
        this.gameServer = gameServer;
    }

    @Override
    public void run() {
        while (!serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept(); // chờ ở đây

                // Server đầy thì từ chối (áp dụng server.max-clients)
                if (gameServer.isFull()) {
                    System.out.println("[ConnectionAcceptor] Server đầy, từ chối client");
                    clientSocket.close();
                    continue;
                }

                // Mỗi client có 1 ClientSession + 1 thread riêng,
                // nên client này đứng chờ không làm chậm client khác.
                ClientSession session = new ClientSession(clientSocket, gameServer);
                gameServer.addSession(session);
                new Thread(session).start();

            } catch (IOException e) {
                System.out.println("[ConnectionAcceptor] Lỗi: " + e.getMessage());
            }
        }
    }
}