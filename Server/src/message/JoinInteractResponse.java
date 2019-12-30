package message;

import java.io.Serializable;

public class JoinInteractResponse implements Serializable {
    private String response;
    private boolean error;

    public JoinInteractResponse(String response) {
        this.response = response;
        this.error = false;
    }

    public JoinInteractResponse(String response, boolean error) {
        this.response = response;
        this.error = error;
    }

    public String getResponse() {
        return response;
    }

    public boolean isError() {
        return error;
    }
}
