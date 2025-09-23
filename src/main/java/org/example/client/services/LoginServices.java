package org.example.client.services;

import com.google.gson.Gson;
import org.example.client.config.SocketConfig;
import org.example.client.views.ChatView;
import org.example.common.PeerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.ServerSocket;

public class LoginServices {
    public static String Login (String username) {
        try {
            Gson gson = new Gson();

            // Tự động chọn một cổng P2P đang rảnh trên máy local
            int dynamicPort;
            try (ServerSocket ss = new ServerSocket(0)) {
                dynamicPort = ss.getLocalPort();
            }

            Socket socket = SocketConfig.createSocket();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            out.println( "LOGIN"+","+username + "," + dynamicPort);

            String data =  in.readLine();
            PeerInfo [] listPeer = gson.fromJson(data,PeerInfo[].class);
            new ChatView(listPeer,socket,in,out,username,String.valueOf(dynamicPort));
            return "ok";
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
