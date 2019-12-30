package Interact;

import java.util.ArrayList;
import server.SocketServerThread;

public class Chat {

    private String name;
    private ArrayList<SocketServerThread> members;
    private final int maxMembers = 2;

    public Chat(String name) {
        this.name = name;
        members = new ArrayList<>();
    }

    public Chat(String name, ArrayList<SocketServerThread> members) {
        this(name);
        this.members = members;   
    }

    public String getName() {
        return name;
    }

    public ArrayList<SocketServerThread> getMembers() {
        return members;
    }

    public boolean addMember(SocketServerThread member) {
        if (members.size() < maxMembers) {
            members.add(member);
            return true;
        }
        return false;
    }

    public boolean removeMember(SocketServerThread member) {
        return members.remove(member);
    }
}
