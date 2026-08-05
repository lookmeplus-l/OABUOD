package fdb.r23studio.ai.model;

import java.util.ArrayList;
import java.util.List;

public class ChatMessage {

    public static final int ROLE_USER = 0;
    public static final int ROLE_AI = 1;

    public int role;
    public String text = "";
    public List<String> images = new ArrayList<>();
    public boolean streaming;
    public boolean error;
    public long timestamp = System.currentTimeMillis();

    public static ChatMessage user(String text) {
        ChatMessage msg = new ChatMessage();
        msg.role = ROLE_USER;
        msg.text = text;
        return msg;
    }

    public static ChatMessage ai() {
        ChatMessage msg = new ChatMessage();
        msg.role = ROLE_AI;
        msg.streaming = true;
        return msg;
    }
}
