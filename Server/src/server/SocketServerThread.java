package server;

import Interact.Chat;
import message.ChatMessage;
import message.JoinInteractRequest;
import message.JoinInteractResponse;
import Interact.Room;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import static java.lang.Thread.sleep;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SocketServerThread implements Runnable {

    private Socket socket;
    private ObjectInputStream serverInput;
    private ObjectOutputStream serverOutput;
    private ArrayList<SocketServerThread> socketThreads;
    private boolean alive;
    private String name;

    public SocketServerThread(Socket socket, ArrayList<SocketServerThread> socketThreads) throws IOException {
        this.socket = socket;
        this.socketThreads = socketThreads;
        alive = true;
        serverOutput = new ObjectOutputStream(this.socket.getOutputStream());
        serverInput = new ObjectInputStream(this.socket.getInputStream());
    }

    public Socket getSocket() {
        return socket;
    }

    public ObjectInputStream getServerInput() {
        return serverInput;
    }

    public ObjectOutputStream getServerOutput() {
        return serverOutput;
    }

    public ArrayList<SocketServerThread> getSocketThreads() {
        return socketThreads;
    }

    public boolean isAlive() {
        return alive;
    }

    private void sendToClient(Object message) throws IOException {
        serverOutput.writeObject(message);
        serverOutput.flush();
    }

    private synchronized void broadcast(ChatMessage message) throws IOException {
        for (SocketServerThread sT : socketThreads) {
            if (sT != this) {
                sT.sendToClient(message);
            }
        }
    }

    private synchronized void sendToRoomMembers(ChatMessage message) throws IOException {
        for (Room room : SocketServer.rooms) {
            System.out.println(room.getName());
            if (room.getName().equalsIgnoreCase(message.getRoomName())) {
                for (SocketServerThread sT : room.getMembers()) {
                    if (sT != this) {
                        sT.sendToClient(message);
                    }
                }
                break;
            }
        }
    }

    private synchronized void sendToChatMembers(ChatMessage message) throws IOException {
        for (Chat chat : SocketServer.chats) {
            if (chat.getName().equalsIgnoreCase(message.getRoomName())) {
                for (SocketServerThread sT : chat.getMembers()) {
                    if (sT != this) {
                        sT.sendToClient(message);
                    }
                }
                break;
            }
        }
    }

    private void onChatMessage(ChatMessage message) throws IOException {
        switch (message.getType()) {
            case BROADCAST:
                broadcast(message);
                break;
            case ROOM:
                sendToRoomMembers(message);
                break;
            case CHAT:
                sendToChatMembers(message);
                break;
        }
    }

    private synchronized void onJoinRoomRequest(JoinInteractRequest request) throws IOException {
        for (Room room : SocketServer.rooms) {
            if (room.getName().equalsIgnoreCase(request.getRoomName())) {
                boolean joined = room.addMember(this);
                if (joined) {
                    sendToClient(new JoinInteractResponse("Joined room: " + room.getName()));
                } else {
                    sendToClient(new JoinInteractResponse("Room: " + room.getName() + " is full", true));
                }
                return;
            }
        }
        // If no room with this name
        Room newRoom;
        newRoom = new Room(request.getRoomName());
        newRoom.addMember(this);
        SocketServer.rooms.add(newRoom);
        sendToClient(new JoinInteractResponse("Joined room: " + newRoom.getName()));
    }

    private synchronized void onJoinChatRequest(JoinInteractRequest request) throws IOException {
        for (Chat chat : SocketServer.chats) {
            if (chat.getName().equalsIgnoreCase(request.getRoomName())) {
                boolean joined = chat.addMember(this);
                if (joined) {
                    sendToClient(new JoinInteractResponse("Joined Chat with " + chat.getName()));
                } else {
                    sendToClient(new JoinInteractResponse(chat.getName() + " is chatting with someone else or refused to chat with you.", true));
                }
                return;
            }
        }
        sendToClient(new JoinInteractResponse("No such User is online", true));
    }

    private void cleanUp() throws IOException {
        serverOutput.close();
        serverInput.close();
        socket.close();

        // Make sure no other thread is accessing the shared ArrayList before accessing it
        synchronized (socketThreads) {
            socketThreads.remove(this);
        }

        // Make sure no other thread is accessing the shared ArrayList before accessing it
        synchronized (SocketServer.rooms) {
            for (Room room : SocketServer.rooms) {
                if (room.getMembers().contains(this)) {
                    room.removeMember(this);
                }
            }
        }
        synchronized (SocketServer.chats) {
            for (Chat chat : SocketServer.chats) {
                if (chat.getMembers().contains(this)) {
                    chat.removeMember(this);
                }
            }
        }
    }

    @Override
    public void run() {
        while (alive) {
            try {
                Object incomingMessage = serverInput.readObject();
                System.out.println(incomingMessage.toString());
                if (!alive) {
                    break;
                }
                if (incomingMessage instanceof ChatMessage) {
                    onChatMessage((ChatMessage) incomingMessage);
                } else if (incomingMessage instanceof JoinInteractRequest) {
                    JoinInteractRequest temp = (JoinInteractRequest) incomingMessage;
                    switch (temp.getType()) {
                        case CHAT:
                            for (SocketServerThread sT : SocketServer.socketThreads) {
                                if (sT.getName().equalsIgnoreCase(temp.getRoomName())) {
                                    sT.sendToClient(temp.getName());
                                }
                            }
                            sleep(1000);
                            onJoinChatRequest(temp);
                            break;
                        case ROOM:
                            onJoinRoomRequest(temp);
                            break;
                        default:
                            throw new AssertionError();
                    }
                } else if (incomingMessage instanceof String) {
                    this.name = (String) incomingMessage;
                    Chat member = new Chat(this.name);
                    member.addMember(this);
                    SocketServer.chats.add(member);
                } else if (incomingMessage instanceof Integer) {
                    String[] online = new String[SocketServer.socketThreads.size()];
                    int j = 0;
                    for (var i : SocketServer.socketThreads) {
                        online[j++] = i.name;
                    }
                    sendToClient(online);
                } else if (incomingMessage instanceof Boolean) {
                    for (Chat chat : SocketServer.chats) {
                        if (chat.getName().equalsIgnoreCase(this.name)) {
                            chat.accept = (Boolean) incomingMessage;
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                killThread();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        try {
            cleanUp();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getName() {
        return name;
    }

    public void killThread() {
        alive = false;
    }
}
