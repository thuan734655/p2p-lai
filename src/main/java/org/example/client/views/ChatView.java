package org.example.client.views;

import com.google.gson.Gson;
import org.example.client.services.MessageListenerService;
import org.example.client.services.PeerServerService;
import org.example.common.AddNewPeer;
import org.example.common.PeerInfo;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatView extends JFrame {
    private JList<String> friendList;
    private DefaultListModel<String> friendModel;
    private JTextPane chatPane;      // thay JTextArea bằng JTextPane
    private StyledDocument doc;
    private JTextField inputField;

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Gson gson = new Gson();

    private final String myUsername;

    public ChatView(PeerInfo[] peers, Socket socket, BufferedReader in, PrintWriter out, String username, String portNumber) {
        super("Chat App - " + username);
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.myUsername = username;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);

        // danh sách bạn bè online
        friendModel = new DefaultListModel<>();
        for (PeerInfo p : peers) {
            friendModel.addElement(p.getUsername() + " (" + p.getIp() + ":" + p.getPort() + ")");
        }
        friendList = new JList<>(friendModel);

        // khu vực tin nhắn
        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();

        // input nhắn tin
        inputField = new JTextField();
        JButton sendBtn = new JButton("Send");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        // layout
        setLayout(new BorderLayout());
        add(new JScrollPane(friendList), BorderLayout.WEST);
        add(new JScrollPane(chatPane), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage()); // enter cũng gửi

        // 🚀 Khởi động PeerServerService để nhận tin P2P
        int myPort = Integer.parseInt(portNumber);
        new Thread(new PeerServerService(myPort, this)).start();

        // chạy listener riêng để nghe server trung tâm
        new Thread(new MessageListenerService(in, this)).start();

        setVisible(true);
    }

    // cập nhật danh sách bạn bè online
    public void updatePeerList(AddNewPeer newPeer) {
        SwingUtilities.invokeLater(() -> {
            String text = newPeer.peer.getUsername() + " (" +
                    newPeer.peer.getIp() + ":" +
                    newPeer.peer.getPort() + ")";
            if (!friendModel.contains(text)) {
                friendModel.addElement(text);
            }
        });
    }

    // thêm tin nhắn với style căn trái/phải
    public void addMessage(String sender, String msg, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                String styleName = isMe ? "Me" : "Friend_" + sender;

                if (chatPane.getStyle(styleName) == null) {
                    Style style = chatPane.addStyle(styleName, null);

                    if (isMe) {
                        StyleConstants.setAlignment(style, StyleConstants.ALIGN_RIGHT);
                        StyleConstants.setForeground(style, Color.BLUE);
                    } else {
                        StyleConstants.setAlignment(style, StyleConstants.ALIGN_LEFT);
                        StyleConstants.setForeground(style, Color.BLACK);
                    }
                }

                Style style = chatPane.getStyle(styleName);

                doc.insertString(doc.getLength(), sender + ": " + msg + "\n", style);
                doc.setParagraphAttributes(doc.getLength(), 1, style, false);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        String selected = friendList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Chọn một người bạn để gửi!");
            return;
        }

        String[] parts = selected.split("\\(");
        String addr = parts[1].replace(")", ""); // ip:port
        String[] ipPort = addr.split(":");
        String ip = ipPort[0];
        int port = Integer.parseInt(ipPort[1]);

        try {
            Socket peerSocket = new Socket(ip, port);
            PrintWriter pw = new PrintWriter(peerSocket.getOutputStream(), true);

            // gửi kèm cả username + nội dung tin nhắn
            pw.println(myUsername + "|" + text);

            // hiển thị tin nhắn của mình
            addMessage(myUsername, text, true);

            peerSocket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không gửi được tin nhắn!");
        }

        inputField.setText("");
    }
}
