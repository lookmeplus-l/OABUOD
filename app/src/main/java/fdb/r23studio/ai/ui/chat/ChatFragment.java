package fdb.r23studio.ai.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import fdb.r23studio.ai.App;
import fdb.r23studio.ai.MainActivity;
import fdb.r23studio.ai.R;
import fdb.r23studio.ai.core.DoubaoEngine;
import fdb.r23studio.ai.core.JsBridge;
import fdb.r23studio.ai.model.ChatMessage;

public class ChatFragment extends Fragment implements JsBridge.EventListener {

    private RecyclerView list;
    private EditText input;
    private Button sendButton;
    private TextView loginStatus;
    private final List<ChatMessage> messages = new ArrayList<>();
    private ChatAdapter adapter;
    private ChatMessage currentAi;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_chat, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        list = view.findViewById(R.id.chat_list);
        input = view.findViewById(R.id.chat_input);
        sendButton = view.findViewById(R.id.chat_send);
        loginStatus = view.findViewById(R.id.chat_login_status);

        adapter = new ChatAdapter(messages);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        list.setAdapter(adapter);

        sendButton.setOnClickListener(v -> send());
        updateLoginStatus();

        loginStatus.setOnClickListener(v -> {
            if (!App.get().isLoggedIn() && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showLoginOverlay();
            } else {
                Toast.makeText(requireContext(), R.string.home_logged_in, Toast.LENGTH_SHORT).show();
            }
        });

        // 快捷功能条：生图 / 视频 / 办公
        view.findViewById(R.id.quick_image).setOnClickListener(v -> switchTab(R.id.nav_image));
        view.findViewById(R.id.quick_video).setOnClickListener(v -> switchTab(R.id.nav_video));
        view.findViewById(R.id.quick_office).setOnClickListener(v -> switchTab(R.id.nav_office));

        App.get().getJsBridge().addListener(this);
    }

    private void switchTab(int itemId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToTab(itemId);
        }
    }

    private void updateLoginStatus() {
        if (loginStatus == null) return;
        loginStatus.setText(App.get().isLoggedIn() ? R.string.home_logged_in : R.string.home_not_logged_in);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateLoginStatus();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.get().getJsBridge().removeListener(this);
    }

    private void send() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;

        messages.add(ChatMessage.user(text));
        currentAi = ChatMessage.ai();
        messages.add(currentAi);
        adapter.notifyItemInserted(messages.size() - 1);
        scrollToBottom();
        input.setText("");

        App.get().getEngine().sendMessage(text, null, new DoubaoEngine.JsCallback() {
            @Override
            public void onResult(String raw) {
                String result = decodeString(raw);
                if ("no-input".equals(result)) {
                    requireActivity().runOnUiThread(() -> {
                        messages.remove(currentAi);
                        currentAi = null;
                        adapter.notifyDataSetChanged();
                        Toast.makeText(requireContext(), R.string.chat_thinking, Toast.LENGTH_SHORT).show();
                    });
                }
            }
        });
    }

    @Override
    public void onEvent(String type, String payload) {
        if (!"diff".equals(type) || payload == null) return;
        try {
            JSONObject obj = new JSONObject(payload);
            String text = obj.optString("text");
            JSONArray images = obj.optJSONArray("images");
            requireActivity().runOnUiThread(() -> {
                if (currentAi == null) {
                    currentAi = ChatMessage.ai();
                    messages.add(currentAi);
                    adapter.notifyItemInserted(messages.size() - 1);
                }
                if (!text.isEmpty()) {
                    currentAi.text += text;
                    currentAi.streaming = false;
                }
                if (images != null && images.length() > 0) {
                    List<String> urls = new ArrayList<>();
                    for (int i = 0; i < images.length(); i++) {
                        urls.add(images.optString(i));
                    }
                    currentAi.images = urls;
                    currentAi.streaming = false;
                }
                adapter.notifyItemChanged(messages.indexOf(currentAi));
                scrollToBottom();
            });
        } catch (Exception ignored) {
        }
    }

    private String decodeString(String raw) {
        if (raw == null || "null".equals(raw)) return null;
        try {
            return new org.json.JSONTokener(raw).nextValue().toString();
        } catch (Exception e) {
            return raw;
        }
    }

    private void scrollToBottom() {
        if (list != null) {
            list.scrollToPosition(messages.size() - 1);
        }
    }
}
