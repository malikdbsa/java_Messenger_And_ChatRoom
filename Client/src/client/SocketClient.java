package client;

import message.*;
import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

public class SocketClient {

    private String name;
    private Socket socket;
    private ObjectInputStream clientInput;
    private ObjectOutputStream clientOutput;

    public SocketClient(String name) {
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
            SocketClient client = new SocketClient(name);
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

            while (true) {
                System.out.println("Welcome to ChatEasy");
                System.out.println("1- Enter a Live Chat with a User.");
                System.out.println("2- See Online Users.");
                System.out.println("3- Enter a Chat Room.");
                System.out.println("0- Exit.");
                System.out.println("Choice: ");
                int c = (Integer) sc.nextInt();
                switch (c) {
                    case 1:
                        System.out.print("Enter a username to chat live with them:");
                        room = sc.next();
                        sendToServer(new JoinInteractRequest(name, room, InteractType.CHAT));
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
                            System.out.print(i + "- ");
                            System.out.println(i);
                        }
                        break;
                    case 3:
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
                    case 0:
                        System.exit(0);
                        break;
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.exit(0);
        }
    }

    private void inRoom(String room) throws IOException {
        new Thread(() -> this.listenToServer()).start();
        System.out.println("You are now connected. (type 'leave' to exit)");
        System.out.println("To Send a file write 'file /path/to/file");
        while (true) {
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine();
            if (input.trim().equalsIgnoreCase("leave")) {
                break;
            }
            if (input.startsWith("file ")) {
                try {
                    sendToServer(new ChatMessage(name, room, input.substring(5), readFile(input.substring(5)), ChatMessageType.ROOM, new Date()));
                } catch (IOException e) {
                    System.out.println("File not found in the location specified");
                    System.out.println(e);
                }
            } else {
                sendToServer(new ChatMessage(name, room, ChatMessageType.ROOM, input, new Date()));
            }
        }
    }

    private void inChat(String room) throws IOException {
        new Thread(() -> this.listenToServer()).start();
        System.out.println("You are now connected to " + room + " (type 'leave' to exit)");
        System.out.println("To Send a file write 'file /path/to/file");
        while (true) {
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine();
            if (input.trim().equalsIgnoreCase("leave")) {
                break;
            }
            if (input.startsWith("file ")) {
                try {
                    sendToServer(new ChatMessage(name, room, input.substring(5), readFile(input.substring(5)), ChatMessageType.CHAT, new Date()));
                } catch (IOException e) {
                    System.out.println("File not found in the location specified");
                    System.out.println(e);
                }
            } else {
                sendToServer(new ChatMessage(name, room, ChatMessageType.CHAT, input, new Date()));
            }
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
                ChatMessage receivedMessage = (ChatMessage) getServerReply();
                if (receivedMessage.getFile() != null) {
                    String[] parts = receivedMessage.getMessage().split("\\.");
                    InputStream is = new ByteArrayInputStream(receivedMessage.getFile());
                    writeFile(receivedMessage.getMessage() + "_COPY." + parts[1], is);
                    System.out.println(ft.format(receivedMessage.getDate()) + " | " + "Received file: " + parts[0] + "_COPY." + parts[1]);
                } else {
                    System.out.println(ft.format(receivedMessage.getDate()) + " | " + receivedMessage.getSenderName() + ": " + receivedMessage.getMessage());
                }
            } catch (EOFException e) {
                System.out.println("Connection was lost with server!");
                break;
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("An error occurred while receiving the message from the server!");
                break;
            }
        }

    }

    public int writeFile(String fileName, InputStream inputStream) throws IOException {
        File f = new File(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
        int bytesWritten = 0;
        int b;
        while ((b = inputStream.read()) != -1) {
            bos.write(b);
            bytesWritten++;
        }
        bos.close();

        return bytesWritten;
    }

    public byte[] readFile(String fileName) throws IOException {
        File file = new File(fileName);
        byte[] fileData = new byte[(int) file.length()];
        try (BufferedInputStream br = new BufferedInputStream(new FileInputStream(file))) {
            int bytesRead = 0;
            int b;
            while ((b = br.read()) != -1) {
                fileData[bytesRead++] = (byte) b;
            }
        }
        return fileData;
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
