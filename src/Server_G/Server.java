package Server_G;

import Server_G.Pages.Lobby;
import Client_G.Pages.*;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class Server {
    public static ServerSocket socket;
    private int port;
    private String UserName;
    public static final int qDuration = 10000; //10 second
    private final int clientLimit = 2; //start the quiz with n clients
    String logS = "";

    private SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");


    public static ArrayList<ClientManager> threadList = new ArrayList<>();
    public static ArrayList<Socket> clientList = new ArrayList<>();

    public void serverStart() {
        try {
            if (socket != null && !socket.isClosed())
                socket.close();
            socket = new ServerSocket(8082);
            String logS = "Server Created!\n";
            Lobby.clientsLogTxtAr.setText(logS);
            int countClient = 0;
            while (countClient < clientLimit) {
                Socket client = socket.accept();
                clientList.add(client);
                countClient++;
                logS = String.format("client %d has connected!, port is: %d\n", countClient, client.getPort());
                Lobby.clientsLogTxtAr.appendText(logS);
                ClientManager t = new ClientManager(this, client, String.valueOf(client.getPort()));
                threadList.add(t);
            }
            logS = "------------------------------------------\nQuiz has started ...\n";
            Lobby.clientsLogTxtAr.appendText(logS);
            for (int i = 0; i < threadList.size(); i++) {
                threadList.get(i).start();
            }
            while (!socket.isClosed()) ;
            System.out.println("Server has downed!");
//            while(true);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    public Server(int port, String name) throws IOException {
        setUserName(name);
        setPort(port);
        this.serverStart();
    }

    private void display(String msg) {
        String time = sdf.format(new Date()) + " " + msg;
        System.out.println(time);
    }

    public synchronized boolean broadcast(String message) {
        // add timestamp to the message
        String time = sdf.format(new Date());

        // to check if message is private i.e. client to client message
        String[] w = message.split(" ", 3);

        boolean isPrivate = false;
        if (w[1].charAt(0) == '@')
            isPrivate = true;


        // if private message, send message to mentioned username only
        if (isPrivate) {
            String tocheck = w[1].substring(1, w[1].length());

            message = w[0] + w[2];
            String messageServerLog = time + " " + "from: " + w[0] + " to: " + w[1] + " message: " + w[2] + "\n";
            // display message to server
            System.out.print(messageServerLog);
            String messageLf = time + " " + message + "\n";
            boolean found = false;
            // we loop in reverse order to find the mentioned username
            for (int y = threadList.size(); --y >= 0; ) {
                ClientManager ct1 = threadList.get(y);
                String check = ct1.getUsername();
                if (check.equals(tocheck)) {
                    // try to write to the Client if it fails remove it from the list
                    if (!ct1.writeMsg(messageLf)) {
//                        threadList.remove(y);
                        display("Disconnected Client " + ct1.getUsername() + " removed from list.");
                    }
                    // username found and delivered the message
                    found = true;
                    break;
                }
            }
            // mentioned user not found, return false
            if (!found) {
                return false;
            }
        }
        // if message is a broadcast message
        else {
            String messageLf = time + " " + message + "\n";
            // display message
            System.out.print(messageLf);

            // we loop in reverse order in case we would have to remove a Client
            // because it has disconnected
            for (int i = threadList.size(); --i >= 0; ) {
                ClientManager ct = threadList.get(i);
                // try to write to the Client if it fails remove it from the list
                if (!ct.writeMsg(messageLf)) {
//                    threadList.remove(i);
                    display("Disconnected Client " + ct.getUsername() + " removed from list.");
                }
            }
        }
        return true;


    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUserName() {
        return UserName;
    }

    public void setUserName(String userName) {
        UserName = userName;
    }

}
