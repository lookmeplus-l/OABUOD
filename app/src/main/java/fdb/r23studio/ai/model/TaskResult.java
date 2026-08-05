package fdb.r23studio.ai.model;

import java.util.ArrayList;
import java.util.List;

public class TaskResult {

    public String text = "";
    public List<String> images = new ArrayList<>();
    public boolean streaming;
    public long timestamp = System.currentTimeMillis();

    public static TaskResult generating() {
        TaskResult result = new TaskResult();
        result.streaming = true;
        return result;
    }
}
