package fdb.r23studio.ai.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import fdb.r23studio.ai.App;
import fdb.r23studio.ai.MainActivity;
import fdb.r23studio.ai.R;
import fdb.r23studio.ai.ui.login.LoginActivity;

public class HomeFragment extends Fragment {

    private TextView loginStatus;
    private Button goLogin;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        loginStatus = view.findViewById(R.id.home_login_status);
        goLogin = view.findViewById(R.id.btn_go_login);

        View cardChat = view.findViewById(R.id.card_chat);
        View cardImage = view.findViewById(R.id.card_image);
        View cardVideo = view.findViewById(R.id.card_video);
        View cardOffice = view.findViewById(R.id.card_office);

        cardChat.setOnClickListener(v -> switchTab(R.id.nav_chat));
        cardImage.setOnClickListener(v -> switchTab(R.id.nav_image));
        cardVideo.setOnClickListener(v -> switchTab(R.id.nav_video));
        cardOffice.setOnClickListener(v -> switchTab(R.id.nav_office));

        goLogin.setOnClickListener(v -> startActivity(new Intent(requireContext(), LoginActivity.class)));
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshLoginState();
    }

    private void refreshLoginState() {
        if (loginStatus == null) return;
        if (App.get().isLoggedIn()) {
            loginStatus.setText(R.string.home_logged_in);
            goLogin.setVisibility(View.GONE);
        } else {
            loginStatus.setText(R.string.home_not_logged_in);
            goLogin.setVisibility(View.VISIBLE);
        }
    }

    private void switchTab(int menuId) {
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).getSupportFragmentManager();
            com.google.android.material.bottomnavigation.BottomNavigationView nav =
                    requireActivity().findViewById(R.id.bottom_nav);
            nav.setSelectedItemId(menuId);
        }
    }
}
