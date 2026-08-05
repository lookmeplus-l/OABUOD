package fdb.r23studio.ai.ui.login;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;
import org.json.JSONTokener;

import fdb.r23studio.ai.App;
import fdb.r23studio.ai.R;
import fdb.r23studio.ai.core.DoubaoEngine;
import fdb.r23studio.ai.core.JsBridge;
import fdb.r23studio.ai.util.ImageLoader;

public class LoginActivity extends AppCompatActivity implements JsBridge.EventListener {

    private ImageView qrImage;
    private ProgressBar qrProgress;
    private TextView qrStatus;
    private boolean qrShown;
    private boolean finishing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        qrImage = findViewById(R.id.qr_image);
        qrProgress = findViewById(R.id.qr_progress);
        qrStatus = findViewById(R.id.qr_status);
        Button btnDone = findViewById(R.id.btn_login_done);
        Button btnCancel = findViewById(R.id.btn_login_cancel);
        Button btnOpenWebView = findViewById(R.id.btn_open_webview);

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkLogin();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnOpenWebView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(DoubaoEngine.HOME_URL)));
                } catch (Exception ignored) {
                }
            }
        });

        App.get().getJsBridge().addListener(this);
        App.get().getEngine().startQrWatch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.get().getJsBridge().removeListener(this);
        App.get().getEngine().stopQrWatch();
    }

    private void checkLogin() {
        App.get().getEngine().isLoggedIn(new DoubaoEngine.JsCallback() {
            @Override
            public void onResult(String raw) {
                boolean loggedIn = parseLoggedIn(raw);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (loggedIn) {
                            App.get().setLoggedIn(true);
                            Toast.makeText(LoginActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            Toast.makeText(LoginActivity.this, R.string.login_not_ready, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private boolean parseLoggedIn(String raw) {
        if (raw == null || "null".equals(raw)) return false;
        try {
            Object outer = new JSONTokener(raw).nextValue();
            String inner = String.valueOf(outer);
            JSONObject obj = new JSONObject(inner);
            return obj.optBoolean("loggedIn", false);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void onEvent(String type, String payload) {
        if ("login-qr".equals(type) && payload != null && !finishing) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    qrShown = true;
                    qrProgress.setVisibility(View.GONE);
                    qrStatus.setText(R.string.login_hint);
                    ImageLoader.load(payload, qrImage);
                }
            });
        }
    }
}
