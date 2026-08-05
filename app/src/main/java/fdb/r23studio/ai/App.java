package fdb.r23studio.ai;

import android.app.Application;
import android.content.SharedPreferences;

import fdb.r23studio.ai.core.DoubaoEngine;
import fdb.r23studio.ai.core.JsBridge;

public class App extends Application {

    private static final String PREFS = "oabuod_prefs";
    private static final String KEY_LOGGED_IN = "logged_in";

    private static App instance;
    private final DoubaoEngine engine = new DoubaoEngine();
    private final JsBridge jsBridge = new JsBridge();

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
    }

    public static App get() {
        return instance;
    }

    public DoubaoEngine getEngine() {
        return engine;
    }

    public JsBridge getJsBridge() {
        return jsBridge;
    }

    public boolean isLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOGGED_IN, false);
    }

    public void setLoggedIn(boolean loggedIn) {
        getSharedPreferences(PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LOGGED_IN, loggedIn)
                .apply();
    }
}
