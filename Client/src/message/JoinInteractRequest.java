package message;

import java.io.Serializable;

public class JoinInteractRequest implements Serializable {

    private String name;
    private String roomName;
    private InteractType type;

    public JoinInteractRequest(String name, String roomName, InteractType type) {
        this.name = name;
        this.roomName = roomName;
        this.type = type;
    }

    public JoinInteractRequest(String name, String roomName) {
        this.name = name;
        this.roomName = roomName;
    }

    public String getName() {
        return name;
    }

    public String getRoomName() {
        return roomName;
    }

    public InteractType getType() {
        return type;
    }
}
