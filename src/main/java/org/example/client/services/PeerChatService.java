package org.example.client.services;

import org.example.client.views.ChatView;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class PeerChatService {
    private final int listenPort;
    private final ChatView chatView;

    public PeerChatService(int listenPort, ChatView chatView) {
        this.listenPort = listenPort;
        this.chatView = chatView;
    }

    // chạy server socket để lắng nghe tin nhắn từ peer
    public void startListener() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
                System.out.println("Listening for peer messages on port " + listenPort);
                while (true) {
                    Socket peerSocket = serverSocket.accept();
                    new Thread(() -> handlePeer(peerSocket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handlePeer(Socket peerSocket) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(peerSocket.getInputStream()))) {
            String line = reader.readLine();
            if (line != null) {
                // format: sender|msg
                String[] parts = line.split("\\|", 2);
                String sender = parts[0];
                String msg = parts.length > 1 ? parts[1] : "";
                chatView.addMessage(sender, msg, false);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try { peerSocket.close(); } catch (IOException ignored) {}
        }
    }

    // gửi tin nhắn đến 1 peer
    public static void sendMessage(String myUsername, String peerIp, int peerPort, String message, ChatView chatView) {
        try (Socket socket = new Socket(peerIp, peerPort);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            // gửi kèm username
            out.println(myUsername + "|" + message);

            // hiển thị luôn tin nhắn mình vừa gửi
            chatView.addMessage(myUsername, message, true);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
