package org.example.common;

import org.example.common.PeerInfo;
import java.net.Socket;

public class ClientSession {
    private final Socket socket;
    private final PeerInfo peerInfo;

    public ClientSession(Socket socket, PeerInfo peerInfo) {
        this.socket = socket;
        this.peerInfo = peerInfo;
    }

    public Socket getSocket() {
        return socket;
    }

    public PeerInfo getPeerInfo() {
        return peerInfo;
    }
}

