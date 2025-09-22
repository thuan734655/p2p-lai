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
import java.util.*;
import java.util.List;

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

    // Lưu trữ hội thoại theo từng người bạn (username -> danh sách tin nhắn)
    private static class ChatMessage {
        final String sender;
        final String text;
        final boolean isMe;
        ChatMessage(String sender, String text, boolean isMe) {
            this.sender = sender;
            this.text = text;
            this.isMe = isMe;
        }
    }

    private final Map<String, java.util.List<ChatMessage>> conversations = new HashMap<>();

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
            // Bỏ qua chính mình trong danh sách bạn bè
            if (p.getUsername() != null && p.getUsername().equals(myUsername)) {
                continue;
            }
            friendModel.addElement(p.getUsername() + " (" + p.getIp() + ":" + p.getPort() + ")");
            conversations.putIfAbsent(p.getUsername(), new ArrayList<>());
        }
        friendList = new JList<>(friendModel);
        friendList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                String selectedUser = getSelectedUsername();
                refreshConversation(selectedUser);
            }
        });

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
            // Không thêm bản thân vào danh sách bạn bè
            if (newPeer.peer.getUsername() != null && newPeer.peer.getUsername().equals(myUsername)) {
                return;
            }
            String text = newPeer.peer.getUsername() + " (" +
                    newPeer.peer.getIp() + ":" +
                    newPeer.peer.getPort() + ")";
            if (!friendModel.contains(text)) {
                friendModel.addElement(text);
                conversations.putIfAbsent(newPeer.peer.getUsername(), new ArrayList<>());
            }
        });
    }

    // thêm tin nhắn với style căn trái/phải
    public void addMessage(String sender, String msg, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                if (isMe) {
                    // Gắn tin nhắn mình gửi vào hội thoại với người đang chọn
                    String peer = getSelectedUsername();
                    if (peer == null) return;
                    conversations.putIfAbsent(peer, new ArrayList<>());
                    conversations.get(peer).add(new ChatMessage(sender, msg, true));

                    // Chỉ hiển thị nếu đang xem đúng người đó
                    if (peer.equals(getSelectedUsername())) {
                        appendStyled(sender, msg, true);
                    }
                } else {
                    // Tin nhắn đến: gắn theo người gửi (sender)
                    String peer = sender;
                    conversations.putIfAbsent(peer, new ArrayList<>());
                    conversations.get(peer).add(new ChatMessage(sender, msg, false));

                    // Nếu đang xem hội thoại của người gửi, hiển thị ngay
                    String current = getSelectedUsername();
                    if (peer.equals(current)) {
                        appendStyled(sender, msg, false);
                    }
                }
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

        // Lấy username của người nhận để gắn hội thoại
        String peerUsername = parts[0].trim();

        try {
            Socket peerSocket = new Socket(ip, port);
            PrintWriter pw = new PrintWriter(peerSocket.getOutputStream(), true);

            // gửi kèm cả username + nội dung tin nhắn
            pw.println(myUsername + "|" + text);

            // Lưu và hiển thị tin nhắn của mình trong hội thoại với peer đang chọn
            conversations.putIfAbsent(peerUsername, new ArrayList<>());
            conversations.get(peerUsername).add(new ChatMessage(myUsername, text, true));
            if (peerUsername.equals(getSelectedUsername())) {
                appendStyled(myUsername, text, true);
            }

            peerSocket.close();
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Không gửi được tin nhắn!");
        }

        inputField.setText("");
    }

    // Lấy username từ item đang chọn trong danh sách bạn bè
    private String getSelectedUsername() {
        String selected = friendList.getSelectedValue();
        if (selected == null) return null;
        int idx = selected.indexOf(" (");
        if (idx <= 0) return selected.trim();
        return selected.substring(0, idx).trim();
    }

    // Làm mới hiển thị hội thoại theo người được chọn
    private void refreshConversation(String username) {
        try {
            chatPane.setText("");
            doc = new DefaultStyledDocument();
            chatPane.setDocument(doc);
            if (username == null) return;
            List<ChatMessage> msgs = conversations.getOrDefault(username, Collections.emptyList());
            for (ChatMessage m : msgs) {
                appendStyled(m.sender, m.text, m.isMe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper chèn tin nhắn với style trái/phải (căn đoạn đúng vùng vừa chèn)
    private void appendStyled(String sender, String msg, boolean isMe) throws BadLocationException {
        String styleName = isMe ? "Me" : "Friend_" + sender;

        // Tạo style nếu chưa có
        if (chatPane.getStyle(styleName) == null) {
            Style style = chatPane.addStyle(styleName, null);
            // màu chữ
            if (isMe) {
                StyleConstants.setForeground(style, new Color(25, 118, 210)); // xanh dương
            } else {
                StyleConstants.setForeground(style, new Color(33, 33, 33)); // đen đậm
            }
            // căn lề đoạn
            StyleConstants.setLeftIndent(style, isMe ? 60f : 10f);
            StyleConstants.setRightIndent(style, isMe ? 10f : 60f);
            StyleConstants.setFirstLineIndent(style, 0f);
            StyleConstants.setSpaceAbove(style, 4f);
            StyleConstants.setSpaceBelow(style, 4f);
        }

        Style style = chatPane.getStyle(styleName);

        // Xác định vị trí bắt đầu trước khi chèn
        int start = doc.getLength();
        String line = sender + ": " + msg + "\n";
        doc.insertString(start, line, null); // chèn text trước

        // Căn đoạn cho vùng vừa chèn
        int end = doc.getLength();
        Element root = doc.getDefaultRootElement();
        int startPara = root.getElementIndex(start);
        int endPara = root.getElementIndex(end);

        // Đặt alignment theo từng đoạn trong vùng [start, end)
        for (int i = startPara; i <= endPara; i++) {
            Element paragraph = root.getElement(i);
            int pStart = paragraph.getStartOffset();
            int pEnd = paragraph.getEndOffset();

            SimpleAttributeSet attrs = new SimpleAttributeSet(style);
            StyleConstants.setAlignment(attrs, isMe ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            doc.setParagraphAttributes(pStart, pEnd - pStart, attrs, false);
        }
    }
}
