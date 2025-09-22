package org.example.client.services;

import org.example.client.views.ChatView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerServerService implements Runnable {
    private final int port;
    private final ChatView chatView;

    public PeerServerService(int port, ChatView chatView) {
        this.port = port;
        this.chatView = chatView;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Peer server started on port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))
        ) {
            String line = in.readLine();
            if (line != null) {
                // Xử lý thông điệp hiện diện từ peer: PRESENCE LOGOUT <username>
                if (line.startsWith("PRESENCE LOGOUT ")) {
                    String username = line.substring("PRESENCE LOGOUT ".length()).trim();
                    if (!username.isEmpty()) {
                        chatView.removePeer(username);
                    }
                    return;
                }
                // format: "username|message"
                String[] parts = line.split("\\|", 2);
                String sender = parts[0];
                String msg = parts.length > 1 ? parts[1] : "";

                chatView.addMessage(sender, msg, false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try { clientSocket.close(); } catch (Exception ignored) {}
        }
    }
}
