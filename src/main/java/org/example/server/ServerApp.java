package org.example.server;

import org.example.common.ClientSession;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ServerApp {
    public static void main(String[] args) {
        int port = 7000;
        Set<ClientSession> sessions = new CopyOnWriteArraySet<>();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());

                new Thread(new ClientHandler(clientSocket, sessions)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
