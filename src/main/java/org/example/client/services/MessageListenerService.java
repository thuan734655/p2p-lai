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

                // server gửi update danh sách peer
                AddNewPeer event = gson.fromJson(line, AddNewPeer.class);
                if (event != null && event.getMessage() != null) {
                    if ("addNewPeer".equalsIgnoreCase(event.getMessage())) {
                        chatView.updatePeerList(event);
                        continue;
                    }
                    // Bỏ qua removePeer từ server theo yêu cầu
                }

                // nếu server broadcast tin nhắn (trường hợp sau này), giả định format: sender|msg
                if (line.contains("|")) {
                    String[] parts = line.split("\\|", 2);
                    String sender = parts[0];
                    String msg = parts.length > 1 ? parts[1] : "";
                    chatView.addMessage(sender, msg, false);
                } else {
                    chatView.addMessage("Server", line, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
