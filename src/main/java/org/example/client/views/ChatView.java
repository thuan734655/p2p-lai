package org.example.client.views;

import com.google.gson.Gson;
import org.example.client.services.MessageListenerService;
import org.example.client.services.PeerServerService;
import org.example.common.AddNewPeer;
import org.example.common.PeerInfo;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.List;

public class ChatView extends JFrame {
    private JList<String> friendList;
    private DefaultListModel<String> friendModel;
    private JTextPane chatPane;
    private StyledDocument doc;
    private JTextField inputField;

    private final Socket socket;
    private final BufferedReader in;
    private final PrintWriter out;
    private final Gson gson = new Gson();

    private final String myUsername;

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

    private final java.util.List<ChatMessage> timeline = new ArrayList<>();
    private final Map<String, PeerInfo> peersMap = new HashMap<>();

    public ChatView(PeerInfo[] peers, Socket socket, BufferedReader in, PrintWriter out, String username, String portNumber) {
        super("Chat App - " + username);
        this.socket = socket;
        this.in = in;
        this.out = out;
        this.myUsername = username;

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setSize(800, 600);

        friendModel = new DefaultListModel<>();
        for (PeerInfo p : peers) {
            if (p.getUsername() != null && p.getUsername().equals(myUsername)) {
                continue;
            }
            friendModel.addElement(p.getUsername());
            peersMap.put(p.getUsername(), p);
        }
        friendList = new JList<>(friendModel);
        friendList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        friendList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                refreshConversation(null);
            }
        });

        chatPane = new JTextPane();
        chatPane.setEditable(false);
        doc = chatPane.getStyledDocument();

        inputField = new JTextField();
        JButton sendBtn = new JButton("Send");

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendBtn, BorderLayout.EAST);

        setLayout(new BorderLayout());
        add(new JScrollPane(friendList), BorderLayout.WEST);
        add(new JScrollPane(chatPane), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());

        int myPort = Integer.parseInt(portNumber);
        new Thread(new PeerServerService(myPort, this)).start();

        new Thread(new MessageListenerService(in, this)).start();

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                broadcastPresenceLogout();
            }
        });

        setVisible(true);
    }

    public void updatePeerList(AddNewPeer newPeer) {
        SwingUtilities.invokeLater(() -> {
            if (newPeer.peer.getUsername() != null && newPeer.peer.getUsername().equals(myUsername)) {
                return;
            }
            String username = newPeer.peer.getUsername();
            if (!friendModel.contains(username)) {
                friendModel.addElement(username);
            }
            peersMap.put(username, newPeer.peer);
        });
    }

    public void removePeer(String username) {
        SwingUtilities.invokeLater(() -> {
            friendModel.removeElement(username);
            peersMap.remove(username);
            addSystemNotice(username + " đã offline.");
        });
    }

    public void addMessage(String sender, String msg, boolean isMe) {
        addMessage(sender, myUsername, msg, isMe);
    }

    public void addMessage(String sender, String receiver, String msg, boolean isMe) {
        SwingUtilities.invokeLater(() -> {
            try {
                timeline.add(new ChatMessage(sender + " -> " + receiver, msg, isMe));
                appendStyled(sender + " -> " + receiver, msg, isMe);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        java.util.List<String> selectedItems = friendList.getSelectedValuesList();
        if (selectedItems == null || selectedItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Chọn ít nhất một người bạn để gửi!");
            return;
        }

        for (String selected : selectedItems) {
            String peerUsername = selected.trim();
            PeerInfo pi = peersMap.get(peerUsername);
            if (pi == null) continue;
            String ip = pi.getIp();
            int port = Integer.parseInt(pi.getPort());

            try {
                Socket peerSocket = new Socket(ip, port);
                PrintWriter pw = new PrintWriter(peerSocket.getOutputStream(), true);
                pw.println(myUsername + "|" + text);
                addMessage(myUsername, peerUsername, text, true);
                peerSocket.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        inputField.setText("");
    }

    private String getSelectedUsername() {
        String selected = friendList.getSelectedValue();
        if (selected == null) return null;
        return selected.trim();
    }

    private void refreshConversation(String username) {
        try {
            chatPane.setText("");
            doc = new DefaultStyledDocument();
            chatPane.setDocument(doc);
            for (ChatMessage m : timeline) {
                appendStyled(m.sender, m.text, m.isMe);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void appendStyled(String sender, String msg, boolean isMe) throws BadLocationException {
        String styleName = isMe ? "Me" : "Friend_" + sender;

        if (chatPane.getStyle(styleName) == null) {
            Style style = chatPane.addStyle(styleName, null);
            if (isMe) {
                StyleConstants.setForeground(style, new Color(25, 118, 210));
            } else {
                StyleConstants.setForeground(style, new Color(33, 33, 33));
            }
            StyleConstants.setLeftIndent(style, isMe ? 60f : 10f);
            StyleConstants.setRightIndent(style, isMe ? 10f : 60f);
            StyleConstants.setFirstLineIndent(style, 0f);
            StyleConstants.setSpaceAbove(style, 4f);
            StyleConstants.setSpaceBelow(style, 4f);
        }

        Style style = chatPane.getStyle(styleName);

        int start = doc.getLength();
        String line = sender + ": " + msg + "\n";
        doc.insertString(start, line, null);

        int end = doc.getLength();
        Element root = doc.getDefaultRootElement();
        int startPara = root.getElementIndex(start);
        int endPara = root.getElementIndex(end);

        for (int i = startPara; i <= endPara; i++) {
            Element paragraph = root.getElement(i);
            int pStart = paragraph.getStartOffset();
            int pEnd = paragraph.getEndOffset();

            SimpleAttributeSet attrs = new SimpleAttributeSet(style);
            StyleConstants.setAlignment(attrs, isMe ? StyleConstants.ALIGN_RIGHT : StyleConstants.ALIGN_LEFT);
            doc.setParagraphAttributes(pStart, pEnd - pStart, attrs, false);
        }
    }

    private void addSystemNotice(String text) {
        SwingUtilities.invokeLater(() -> {
            try {
                String styleName = "SystemNotice";
                if (chatPane.getStyle(styleName) == null) {
                    Style style = chatPane.addStyle(styleName, null);
                    StyleConstants.setForeground(style, new Color(120, 120, 120));
                    StyleConstants.setItalic(style, true);
                    StyleConstants.setLeftIndent(style, 10f);
                    StyleConstants.setRightIndent(style, 10f);
                    StyleConstants.setSpaceAbove(style, 4f);
                    StyleConstants.setSpaceBelow(style, 4f);
                }

                Style style = chatPane.getStyle(styleName);
                int start = doc.getLength();
                String line = text + "\n";
                doc.insertString(start, line, style);

                int end = doc.getLength();
                Element root = doc.getDefaultRootElement();
                int startPara = root.getElementIndex(start);
                int endPara = root.getElementIndex(end);
                for (int i = startPara; i <= endPara; i++) {
                    Element paragraph = root.getElement(i);
                    int pStart = paragraph.getStartOffset();
                    int pEnd = paragraph.getEndOffset();
                    SimpleAttributeSet attrs = new SimpleAttributeSet(style);
                    StyleConstants.setAlignment(attrs, StyleConstants.ALIGN_LEFT);
                    doc.setParagraphAttributes(pStart, pEnd - pStart, attrs, false);
                }
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        });
    }

    private void broadcastPresenceLogout() {
        java.util.List<String> items = new ArrayList<>();
        for (int i = 0; i < friendModel.size(); i++) {
            items.add(friendModel.getElementAt(i));
        }
        for (String username : items) {
            try {
                PeerInfo pi = peersMap.get(username);
                if (pi == null) continue;
                String ip = pi.getIp();
                int port = Integer.parseInt(pi.getPort());

                try (Socket s = new Socket(ip, port); PrintWriter pw = new PrintWriter(s.getOutputStream(), true)) {
                    pw.println("PRESENCE LOGOUT " + myUsername);
                }
            } catch (Exception ignored) {
            }
        }
    }
}
