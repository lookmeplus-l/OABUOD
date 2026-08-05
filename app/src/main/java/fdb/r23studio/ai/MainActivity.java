package fdb.r23studio.ai;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;
import org.json.JSONTokener;

import fdb.r23studio.ai.core.DoubaoEngine;
import fdb.r23studio.ai.ui.chat.ChatFragment;
import fdb.r23studio.ai.ui.home.HomeFragment;
import fdb.r23studio.ai.ui.image.ImageFragment;
import fdb.r23studio.ai.ui.office.OfficeFragment;
import fdb.r23studio.ai.ui.video.VideoFragment;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private ChatFragment chatFragment;
    private ImageFragment imageFragment;
    private VideoFragment videoFragment;
    private OfficeFragment officeFragment;

    private Fragment currentFragment;

    private WebView hiddenWebView;
    private LinearLayout uiContainer;
    private LinearLayout loginOverlay;
    private boolean loginChecked;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hiddenWebView = findViewById(R.id.engine_webview);
        uiContainer = findViewById(R.id.ui_container);
        loginOverlay = findViewById(R.id.login_overlay);

        App.get().getEngine().init(this, hiddenWebView, App.get().getJsBridge());
        App.get().getEngine().loadHome();

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_chat) {
                show(chatFragment());
                return true;
            } else if (id == R.id.nav_image) {
                show(imageFragment());
                return true;
            } else if (id == R.id.nav_video) {
                show(videoFragment());
                return true;
            } else if (id == R.id.nav_office) {
                show(officeFragment());
                return true;
            }
            show(homeFragment());
            return true;
        });

        Button btnLoginDone = findViewById(R.id.btn_login_done);
        Button btnLoginCancel = findViewById(R.id.btn_login_cancel);
        btnLoginDone.setOnClickListener(v -> onLoginDoneClicked());
        btnLoginCancel.setOnClickListener(v -> onLoginCancelClicked());

        show(homeFragment());

        if (!App.get().isLoggedIn()) {
            showLoginOverlay();
        }
    }

    public void showLoginOverlay() {
        if (loginOverlay == null) return;
        hiddenWebView.setAlpha(1f);
        uiContainer.setVisibility(View.GONE);
        loginOverlay.setVisibility(View.VISIBLE);
        // 登录页未加载时重新加载豆包首页
        App.get().getEngine().reload();
    }

    public void hideLoginOverlay() {
        if (loginOverlay == null) return;
        loginOverlay.setVisibility(View.GONE);
        uiContainer.setVisibility(View.VISIBLE);
        hiddenWebView.setAlpha(0f);
    }

    private void onLoginDoneClicked() {
        App.get().getEngine().isLoggedIn(new DoubaoEngine.JsCallback() {
            @Override
            public void onResult(String raw) {
                boolean loggedIn = parseLoggedIn(raw);
                runOnUiThread(() -> {
                    if (loggedIn) {
                        App.get().setLoggedIn(true);
                        hideLoginOverlay();
                        Toast.makeText(MainActivity.this, R.string.login_success, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(MainActivity.this, R.string.login_not_ready, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void onLoginCancelClicked() {
        if (App.get().isLoggedIn()) {
            hideLoginOverlay();
        } else {
            finish();
        }
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

    public void switchToChat() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_chat);
    }

    private Fragment homeFragment() {
        if (homeFragment == null) homeFragment = new HomeFragment();
        return homeFragment;
    }

    private Fragment chatFragment() {
        if (chatFragment == null) chatFragment = new ChatFragment();
        return chatFragment;
    }

    private Fragment imageFragment() {
        if (imageFragment == null) imageFragment = new ImageFragment();
        return imageFragment;
    }

    private Fragment videoFragment() {
        if (videoFragment == null) videoFragment = new VideoFragment();
        return videoFragment;
    }

    private Fragment officeFragment() {
        if (officeFragment == null) officeFragment = new OfficeFragment();
        return officeFragment;
    }

    private void show(Fragment fragment) {
        if (fragment == null || fragment == currentFragment) return;
        FragmentTransaction tx = getSupportFragmentManager().beginTransaction();
        Fragment[] all = {homeFragment, chatFragment, imageFragment, videoFragment, officeFragment};
        for (Fragment f : all) {
            if (f != null && f != fragment && f.isAdded()) {
                tx.hide(f);
            }
        }
        if (fragment.isAdded()) {
            tx.show(fragment);
        } else {
            tx.add(R.id.fragment_container, fragment);
        }
        tx.commitAllowingStateLoss();
        currentFragment = fragment;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        App.get().getEngine().onActivityResult(requestCode, resultCode, data);
    }
}
