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

                AddNewPeer event = gson.fromJson(line, AddNewPeer.class);
                if (event != null && event.getMessage() != null) {
                    if ("addNewPeer".equalsIgnoreCase(event.getMessage())) {
                        chatView.updatePeerList(event);
                        continue;
                    }
                    // Bỏ qua mọi JSON control message khác (vd: removePeer) và KHÔNG in ra chat
                    continue;
                }

                if (line.contains("|")) {
                    String[] parts = line.split("\\|", 2);
                    String sender = parts[0];
                    String msg = parts.length > 1 ? parts[1] : "";
                    chatView.addMessage(sender, msg, false);
                } else {
                    // Bỏ qua các dòng từ server không theo định dạng sender|msg để tránh in JSON
                    if (!line.trim().startsWith("{")) {
                        chatView.addMessage("Server", line, false);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
