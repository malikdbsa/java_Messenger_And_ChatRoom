package client;

import message.ChatMessage;
import message.ChatMessageType;
import message.JoinInteractRequest;
import message.JoinInteractResponse;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.*;
import java.util.*;
import java.util.Scanner;
import message.InteractType;

public class SocketClient1 {

    private String name;
    private Socket socket;
    private ObjectInputStream clientInput;
    private ObjectOutputStream clientOutput;

    public SocketClient1(String name) {
        this.name = name;
    }

    public static void main(String[] args) {
        try {
            Scanner sc = new Scanner(System.in);
            String name;
            do {
                System.out.print("Please enter your name: ");
                name = sc.nextLine();
            } while (name.trim().equals(""));
            SocketClient1 client = new SocketClient1(name);
            client.connectToServer("localhost", 6000);
        } catch (Exception e) {
            System.out.println("Could not connect to server!");
            e.printStackTrace();
        }
    }

    private void initiate() {
        try {
            Scanner sc = new Scanner(System.in);
            String room;
            JoinInteractResponse response;
            new Thread(() -> this.listenToServer()).start();
            while (true) {
                System.out.println("Welcome to ChatEasy");
                System.out.println("1- Enter a Live Chat with a User.");
                System.out.println("2- See Online Users.");
                System.out.println("3- Send a message to a User.");
                System.out.println("4- Check your messages.");
                System.out.println("5- Enter a Chat Room.");
                System.out.println("6- Send a file to a User.");
                System.out.println("7- Check files recieved.");
                System.out.println("8- Send a message to all online Users");
                System.out.println("0- Exit.");
                System.out.println("Choice: ");
                int c = (Integer) sc.nextInt();
                switch (c) {
                    case 1:
                        System.out.print("Enter a username to chat live with them:");
                        room = sc.next();
                        sendToServer(new JoinInteractRequest(name, room, InteractType.CHAT));
                        System.out.println("Waiting for Server to reply.(it may take a few seconds");
                        response = (JoinInteractResponse) getServerReply();
                        System.out.println(response.getResponse());
                        if (response.isError()) {
                            continue;
                        }
                        inChat(room);
                        break;
                    case 2:
                        sendToServer(1);
                        String[] online = (String[]) getServerReply();
                        System.out.println("These users are online.");
                        for (String i : online) {
                            System.out.println(i);
                        }
                        break;
                    case 3:
                        //Under Construction
                        break;
                    case 4:
                        //Under Construction
                        break;
                    case 5:
                        System.out.print("Please enter a room name to join: ");
                        room = sc.next();
                        sendToServer(new JoinInteractRequest(name, room, InteractType.ROOM));
                        response = (JoinInteractResponse) getServerReply();
                        System.out.println(response.getResponse());
                        if (response.isError()) {
                            continue;
                        }
                        inRoom(room);
                        break;
                    case 6:
                        //Already done just integration needed
                        break;
                    case 7:
                        //Already done just integration needed
                        break;
                    case 8:
                        //Already done just integration needed
                        break;
                    case 0:
                        System.exit(0);
                        break;

                }

            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            System.exit(0);
        }
    }

    private void inRoom(String room) throws IOException {
        System.out.println("You are now connected. (type 'leave' to exit)");
        while (true) {
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine();
            if (input.trim().equalsIgnoreCase("leave")) {
                break;
            }
            sendToServer(new ChatMessage(name, room, ChatMessageType.ROOM, input, new Date()));
        }
    }

    private void inChat(String room) throws IOException {
        System.out.println("You are now connected to " + room + " (type 'leave' to exit)");
        while (true) {
            Scanner sc = new Scanner(System.in);
//            System.out.print("You: ");
            String input = sc.nextLine();
            if (input.trim().equalsIgnoreCase("leave")) {
                break;
            }
            sendToServer(new ChatMessage(name, room, ChatMessageType.CHAT, input, new Date()));
        }
    }

    public String getName() {
        return name;
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getClientInput() {
        return clientInput;
    }

    public ObjectOutputStream getClientOutput() {
        return clientOutput;
    }

    private synchronized void listenToServer() {
        SimpleDateFormat ft
                = new SimpleDateFormat("hh:mm:ss a");
        while (true) {
            try {
                Object incomingMessage = getServerReply();
                System.out.println(incomingMessage.toString());
                if (incomingMessage instanceof ChatMessage) {
                    ChatMessage receivedMessage = (ChatMessage) getServerReply();
                    System.out.println(ft.format(receivedMessage.getDate()) + "   " + receivedMessage.getSenderName() + ": " + receivedMessage.getMessage());
                } else if (incomingMessage instanceof String) {
                    String room = (String) incomingMessage;
                    boolean a = accept(room);
                    sendToServer(a);
                    if (a) {
                        sendToServer(new JoinInteractRequest(name, room, InteractType.CHAT));
                        JoinInteractResponse response = (JoinInteractResponse) getServerReply();
                        System.out.println(response.getResponse());
                        if (response.isError()) {
                            continue;
                        }
                        inChat(room);
                    }else{
                        continue;
                    }
                }
            } catch (EOFException e) {
                System.out.println("Connection was lost with server!");
                e.printStackTrace();
                break;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("An error occurred while receiving the message from the server!");
                e.printStackTrace();
                break;
            }
        }

    }

    private boolean accept(String name) {
        System.out.println("Recieved Live Chat Request from " + name);
        System.out.print("Accept?(Y/N)");
        Scanner sc = new Scanner(System.in);
        String in = sc.next();
        if (in.trim().equalsIgnoreCase("y")) {
            return true;
        } else if (in.trim().equalsIgnoreCase("n")) {
            return false;
        } else {
            System.out.println("Wrong Input");
        }
        return accept(name);
    }

    private void writeFile(String filePath, byte[] bytes) throws IOException {
        Files.write(Paths.get(filePath).getFileName(), bytes);
    }

    private byte[] readFile(String filePath) throws IOException {
        return Files.readAllBytes(Paths.get(filePath));
    }

    public void sendToServer(Object message) throws IOException {
        clientOutput.writeObject(message);
        clientOutput.flush();
    }

    public Object getServerReply() throws IOException, ClassNotFoundException {
        return clientInput.readObject();
    }

    public void connectToServer(String serverAddress, int serverPort) throws IOException {
        socket = new Socket(serverAddress, serverPort);
        clientInput = new ObjectInputStream(socket.getInputStream());
        clientOutput = new ObjectOutputStream(socket.getOutputStream());
        sendToServer(name);
        initiate();
    }
}
