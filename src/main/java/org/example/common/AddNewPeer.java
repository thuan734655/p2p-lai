package org.example.common;

public class AddNewPeer {
    public  String message;
    public  PeerInfo peer;

    public AddNewPeer(String message, PeerInfo peer) {
        this.message = message;
        this.peer = peer;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public PeerInfo getPeer() {
        return peer;
    }

    public void setPeer(PeerInfo peer) {
        this.peer = peer;
    }
}
