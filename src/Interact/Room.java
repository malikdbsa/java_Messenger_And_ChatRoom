package Interact;

import server.SocketServerThread;

import java.util.ArrayList;

public class Room {
    private String name;
    private ArrayList<SocketServerThread> members;
    private int maxMembers;

    public Room(String name) {
        this.name = name;
        members = new ArrayList<>();
        maxMembers = 4;
    }


    public Room(String name, ArrayList<SocketServerThread> members) {
        this(name);
        this.members = members;
        maxMembers = 4;
    }
    
    public Room(String name, ArrayList<SocketServerThread> members,int maxMembers) {
        this(name);
        this.members = members;
        this.maxMembers = maxMembers;
    }

    public String getName() {
        return name;
    }

    public ArrayList<SocketServerThread> getMembers() {
        return members;
    }

    public int getMaxMembers() {
        return maxMembers;
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
