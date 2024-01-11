package org.code4all.gunix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Set;

public class TCPserver {
    int portNum;
    ServerSocket serverSocket;
    Socket clientSocket;
    HashMap<Socket, String> clientTable;
    HashMap<String, Socket> displayNameTable;

    public TCPserver(int portNum) throws IOException {
        this.portNum = portNum;
        System.out.println("== WhatsApp-Terminal server online on PORT : " + portNum + " ==");

        serverSocket = new ServerSocket(portNum);
        this.clientTable = new HashMap<Socket, String>();
        this.displayNameTable = new HashMap<String, Socket>();

        while (true) {
            clientSocket = serverSocket.accept();

            System.out.println("\n"+"new user from port " + clientSocket.getPort());

            Thread test = new Thread(new ClientHandler(clientSocket));
            test.start(); // Start a new thread for the ClientHandler
        }
    }

    public class ClientHandler implements Runnable {
        private Socket clientSocket;
        private String displayName;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                BufferedReader inputFromUser = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter write2User = new PrintWriter(clientSocket.getOutputStream(), true);

                write2User.println("Provide a display name");
                displayName = inputFromUser.readLine(); // Read the display name from the client
                String whisperSenderName = displayName;

                clientTable.put(clientSocket, displayName);
                displayNameTable.put(displayName, clientSocket); // just for whisper to make it quick

                System.out.println("Client Table : " + clientTable.values()+"\n");
                write2User.println("Server: Thank you, " + displayName + ". You are now connected.");
                write2User.println("==============================================================");

                Set clientSocketSet = clientTable.keySet();

                while (true) {
                    String clientMessage = inputFromUser.readLine();

                    if (clientMessage == null) {
                        break; // Client has disconnected
                    }

                    String lastMSG = "~" + displayName + " : " + clientMessage;
                    System.out.println(lastMSG);

                    if (clientMessage.startsWith("/list")) {
                        write2User.println(clientTable.values());
                    } else if (clientMessage.startsWith("/quit")) {
                        break;
                    } else if (clientMessage.startsWith("/w")) {
                        String[] whisperParts = clientMessage.split(" ",3);
                        String user2Send = whisperParts[1];
                        String whisper2Send = whisperParts[2];

                        Socket whisperReceiver = displayNameTable.get(user2Send);

                        if (whisperReceiver != null) {
                                PrintWriter whisper = new PrintWriter(whisperReceiver.getOutputStream(), true);
                                whisper.println("@"+whisperSenderName+" : "+whisper2Send);
                        } else {
                            // Handle the case when the user is not found
                            System.out.println("User not found: " + user2Send);
                        }


                        PrintWriter whisper = new PrintWriter(whisperReceiver.getOutputStream());
                        whisper.println(whisper2Send);
                    } else {

                        for (Object wannaSend : clientSocketSet) {
                            // Skip sending the message to the current client
                            Socket send2 = (Socket) wannaSend;

                            if (send2 != clientSocket) {
                                PrintWriter write2OtherClients = new PrintWriter(send2.getOutputStream(), true);
                                write2OtherClients.println(lastMSG);
                            }
                        }
                    }
                }
                inputFromUser.close();
                write2User.close();
                System.out.println(displayName + " has left the server."+"\n");

            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                clientTable.remove(clientSocket);
                displayNameTable.remove(displayName);
                System.out.println("-User List Updated-"+"\n");
            }
        }
    }
}
