package server;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
    private static final Map<String, Player> players = new HashMap<>();
    private static final List<ClientHandler> clients = new ArrayList<>();

    public static void main(String[] args) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(GameConfig.PORT)) {
            System.out.println("Server started on " + GameConfig.PORT);

            while (true) {
                Socket socket = serverSocket.accept();
                ClientHandler handler = new ClientHandler(socket, players, clients);
                clients.add(handler);
                new Thread(handler).start();
            }
        }
    }
}
