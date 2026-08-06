package fdb.r23studio.ai;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONObject;
import org.json.JSONTokener;

import fdb.r23studio.ai.core.DoubaoEngine;
import fdb.r23studio.ai.core.JsBridge;
import fdb.r23studio.ai.ui.chat.ChatFragment;
import fdb.r23studio.ai.ui.image.ImageFragment;
import fdb.r23studio.ai.ui.office.OfficeFragment;
import fdb.r23studio.ai.ui.video.VideoFragment;

public class MainActivity extends AppCompatActivity implements JsBridge.EventListener {

    private ChatFragment chatFragment;
    private ImageFragment imageFragment;
    private VideoFragment videoFragment;
    private OfficeFragment officeFragment;

    private Fragment currentFragment;

    private WebView hiddenWebView;
    private LinearLayout uiContainer;
    private LinearLayout loginOverlay;
    private ImageView loginQrImage;
    private ProgressBar loginQrProgress;
    private TextView loginQrStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hiddenWebView = findViewById(R.id.engine_webview);
        uiContainer = findViewById(R.id.ui_container);
        loginOverlay = findViewById(R.id.login_overlay);
        loginQrImage = findViewById(R.id.login_qr_image);
        loginQrProgress = findViewById(R.id.login_qr_progress);
        loginQrStatus = findViewById(R.id.login_qr_status);

        App.get().getEngine().init(this, hiddenWebView, App.get().getJsBridge());
        App.get().getEngine().setOnPageLoadedListener(() -> {
            if (loginOverlay != null && loginOverlay.getVisibility() == View.VISIBLE) {
                openScanLogin();
            }
        });
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
            return false;
        });

        Button btnLoginDone = findViewById(R.id.btn_login_done);
        Button btnLoginCancel = findViewById(R.id.btn_login_cancel);
        btnLoginDone.setOnClickListener(v -> onLoginDoneClicked());
        btnLoginCancel.setOnClickListener(v -> onLoginCancelClicked());

        App.get().getJsBridge().addListener(this);
        show(chatFragment());

        if (!App.get().isLoggedIn()) {
            showLoginOverlay();
        }
    }

    public void showLoginOverlay() {
        if (loginOverlay == null) return;
        hiddenWebView.setAlpha(1f);
        uiContainer.setVisibility(View.GONE);
        loginOverlay.setVisibility(View.VISIBLE);
        resetQrUi();
        App.get().getEngine().reload();
        // 页面重新加载后由 onPageFinished 触发登录面板打开与二维码提取
    }

    public void hideLoginOverlay() {
        if (loginOverlay == null) return;
        loginOverlay.setVisibility(View.GONE);
        uiContainer.setVisibility(View.VISIBLE);
        hiddenWebView.setAlpha(0f);
        App.get().getEngine().stopQrWatch();
    }

    private void resetQrUi() {
        loginQrImage.setVisibility(View.INVISIBLE);
        loginQrProgress.setVisibility(View.VISIBLE);
        loginQrStatus.setText(R.string.login_waiting_qr);
    }

    /** 登录页加载完成后调用：自动打开豆包登录面板并切换到扫码登录 */
    public void openScanLogin() {
        resetQrUi();
        App.get().getEngine().triggerScanLogin(new DoubaoEngine.JsCallback() {
            @Override
            public void onResult(String raw) {
                if (loginOverlay == null || loginOverlay.getVisibility() != View.VISIBLE) return;
                runOnUiThread(() -> {
                    if (raw != null && "false".equals(raw)) {
                        loginQrStatus.setText(R.string.login_open_webview);
                    }
                });
            }
        });
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

    /** 供 Fragment（如聊天页快捷功能条）切换底部导航 */
    public void switchToTab(int itemId) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(itemId);
    }

    public void switchToChat() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_chat);
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
        Fragment[] all = {chatFragment, imageFragment, videoFragment, officeFragment};
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
    public void onEvent(String type, String payload) {
        if ("login-qr".equals(type) && payload != null) {
            runOnUiThread(() -> showQr(payload));
        } else if ("login-panel".equals(type)) {
            // 登录面板已打开（或点击扫码按钮），开始提取二维码
            runOnUiThread(() -> {
                App.get().getEngine().startQrWatch();
                if ("no-scan-button".equals(payload)) {
                    loginQrStatus.setText(R.string.login_open_webview);
                }
            });
        }
    }

    private void showQr(String base64DataUrl) {
        try {
            String b64 = base64DataUrl;
            int idx = base64DataUrl.indexOf(',');
            if (idx >= 0) b64 = base64DataUrl.substring(idx + 1);
            byte[] bytes = Base64.decode(b64, Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
            if (bmp == null) return;
            loginQrImage.setImageBitmap(bmp);
            loginQrImage.setVisibility(View.VISIBLE);
            loginQrProgress.setVisibility(View.GONE);
            loginQrStatus.setText(R.string.login_hint);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        App.get().getJsBridge().removeListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        App.get().getEngine().onActivityResult(requestCode, resultCode, data);
    }
}
