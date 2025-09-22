package org.example.common;

public class PeerInfo {
    private final String username;
    private final String ip;
    private final String port;

    public PeerInfo(String username,String port, String ip) {
        this.username = username;
        this.ip = ip;
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    @Override
    public String toString() {
        return username + "@" + ip + ":" + port;
    }
}
