package org.example.client.services;

import com.google.gson.Gson;
import org.example.client.config.SocketConfig;
import org.example.client.views.ChatView;
import org.example.common.PeerInfo;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class LoginServices {
    public static String Login (String username, String portNumber) {
        try {
            Gson gson = new Gson();

            Socket socket = SocketConfig.createSocket();
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
            out.println( "LOGIN"+","+username + "," + portNumber);

            String data =  in.readLine();
            PeerInfo [] listPeer = gson.fromJson(data,PeerInfo[].class);
            new ChatView(listPeer,socket,in,out,username,portNumber);
            return "ok";
        }catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }
}
