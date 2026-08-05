package fdb.r23studio.ai.core;

import android.webkit.JavascriptInterface;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class JsBridge {

    public interface EventListener {
        void onEvent(String type, String payload);
    }

    private final List<EventListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(EventListener listener) {
        if (listener != null) listeners.add(listener);
    }

    public void removeListener(EventListener listener) {
        listeners.remove(listener);
    }

    @JavascriptInterface
    public void onEvent(String type, String payload) {
        for (EventListener listener : listeners) {
            if (listener != null) {
                listener.onEvent(type, payload);
            }
        }
    }
}
