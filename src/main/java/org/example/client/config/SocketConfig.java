package org.example.client.config;

import java.net.Socket;

public class SocketConfig {
    public static int port = 7000;
    public static String host = "localhost";

    public static Socket createSocket() {
        try {
            Socket socket = new Socket(host,port);
            System.out.println("Connected to server!");
            return  socket;
        }
        catch (Exception e) {
            System.out.println(e.getMessage());
            return  null;
        }
    }
}
