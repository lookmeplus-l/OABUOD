package fdb.r23studio.ai;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import fdb.r23studio.ai.ui.chat.ChatFragment;
import fdb.r23studio.ai.ui.home.HomeFragment;
import fdb.r23studio.ai.ui.image.ImageFragment;
import fdb.r23studio.ai.ui.login.LoginActivity;
import fdb.r23studio.ai.ui.office.OfficeFragment;
import fdb.r23studio.ai.ui.video.VideoFragment;

public class MainActivity extends AppCompatActivity {

    private HomeFragment homeFragment;
    private ChatFragment chatFragment;
    private ImageFragment imageFragment;
    private VideoFragment videoFragment;
    private OfficeFragment officeFragment;

    private Fragment currentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        WebView hiddenWebView = findViewById(R.id.engine_webview);
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

        show(homeFragment());

        if (!App.get().isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
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
