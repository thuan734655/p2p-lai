package org.example.server;

import com.google.gson.Gson;
import org.example.common.AddNewPeer;
import org.example.common.ClientSession;
import org.example.common.PeerInfo;

import java.io.*;
import java.net.Socket;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class ClientHandler implements Runnable {
    private static final Gson gson = new Gson();

    private final Socket socket;
    private final Set<ClientSession> sessions;
    private ClientSession mySession; // giữ thông tin session của client hiện tại
    private static final String MESSAGE = "addNewPeer";

    public ClientHandler(Socket socket, Set<ClientSession> sessions) {
        this.socket = socket;
        this.sessions = sessions;
    }

    @Override
    public void run() {
        try (
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println("Received: " + line);

                String[] parts = line.split(",");
                String command = parts[0];

                if ("LOGIN".equalsIgnoreCase(command) && parts.length == 3) {
                    String username = parts[1];
                    String peerPort = parts[2];
                    String ip = socket.getInetAddress().getHostAddress();

                    PeerInfo peerInfo = new PeerInfo(username, peerPort, ip);
                    mySession = new ClientSession(socket, peerInfo);
                    sessions.add(mySession);

                    // Gửi toàn bộ danh sách peer cho client vừa login
                    out.println(gson.toJson(getAllPeers()));

                    // Thông báo cho mọi người có peer mới
                    broadcastPeerList(peerInfo);
                }
                else if ("LOGOUT".equalsIgnoreCase(command) && mySession != null) {
                    sessions.remove(mySession);
                    socket.close();
                    broadcastRemovePeer(mySession.getPeerInfo());
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client disconnected: " + socket.getInetAddress());
        } finally {
            if (mySession != null) {
                sessions.remove(mySession);
                broadcastRemovePeer(mySession.getPeerInfo());
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    private Set<PeerInfo> getAllPeers() {
        Set<PeerInfo> peers = new CopyOnWriteArraySet<>();
        for (ClientSession s : sessions) {
            peers.add(s.getPeerInfo());
        }
        return peers;
    }

    private void broadcastPeerList(PeerInfo newPeer) {
        AddNewPeer addNewPeer = new AddNewPeer(MESSAGE, newPeer);
        String json = gson.toJson(addNewPeer);

        Iterator<ClientSession> it = sessions.iterator();
        while (it.hasNext()) {
            ClientSession s = it.next();
            try {
                PrintWriter out = new PrintWriter(s.getSocket().getOutputStream(), true);
                out.println(json);
            } catch (IOException e) {
                it.remove();
                try { s.getSocket().close(); } catch (IOException ignored) {}
            }
        }
    }

    private void broadcastRemovePeer(PeerInfo removedPeer) {
        String msg = gson.toJson(new AddNewPeer("removePeer", removedPeer));
        Iterator<ClientSession> it = sessions.iterator();
        while (it.hasNext()) {
            ClientSession s = it.next();
            try {
                PrintWriter out = new PrintWriter(s.getSocket().getOutputStream(), true);
                out.println(msg);
            } catch (IOException e) {
                it.remove();
                try { s.getSocket().close(); } catch (IOException ignored) {}
            }
        }
    }
}
