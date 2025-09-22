package org.example.client.services;

import com.google.gson.Gson;
import org.example.client.views.ChatView;
import org.example.common.AddNewPeer;

import java.io.BufferedReader;

public class MessageListenerService implements Runnable {
    private final BufferedReader in;
    private final ChatView chatView;
    private final Gson gson = new Gson();

    public MessageListenerService(BufferedReader in, ChatView chatView) {
        this.in = in;
        this.chatView = chatView;
    }

    @Override
    public void run() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("Received from server: " + line);

                AddNewPeer newPeer = gson.fromJson(line, AddNewPeer.class);
                if (newPeer != null && "addNewPeer".equalsIgnoreCase(newPeer.getMessage())) {
                    chatView.updatePeerList(newPeer);
                } else {
                    if (line.contains("|")) {
                        String[] parts = line.split("\\|", 2);
                        String sender = parts[0];
                        String msg = parts.length > 1 ? parts[1] : "";
                        chatView.addMessage(sender, msg, false);
                    } else {
                        chatView.addMessage("Server", line, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
