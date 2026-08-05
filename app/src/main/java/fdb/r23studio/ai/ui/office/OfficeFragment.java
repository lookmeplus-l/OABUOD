package fdb.r23studio.ai.ui.office;

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
import fdb.r23studio.ai.R;
import fdb.r23studio.ai.core.DoubaoEngine;
import fdb.r23studio.ai.core.JsBridge;
import fdb.r23studio.ai.model.TaskResult;
import fdb.r23studio.ai.ui.task.TaskAdapter;

public class OfficeFragment extends Fragment implements JsBridge.EventListener {

    private static final String DEFAULT_AGENT = "officeDoc";

    private String currentAgent = DEFAULT_AGENT;
    private EditText input;
    private TextView status;
    private RecyclerView resultsView;
    private final List<TaskResult> results = new ArrayList<>();
    private TaskAdapter adapter;
    private TaskResult current;
    private Button docBtn;
    private Button tableBtn;
    private Button slideBtn;
    private Button mindBtn;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_office, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        input = view.findViewById(R.id.task_input);
        status = view.findViewById(R.id.task_status);
        Button generate = view.findViewById(R.id.task_generate);
        resultsView = view.findViewById(R.id.task_results);
        docBtn = view.findViewById(R.id.office_doc);
        tableBtn = view.findViewById(R.id.office_table);
        slideBtn = view.findViewById(R.id.office_slide);
        mindBtn = view.findViewById(R.id.office_mind);

        adapter = new TaskAdapter(results);
        resultsView.setLayoutManager(new LinearLayoutManager(requireContext()));
        resultsView.setAdapter(adapter);

        docBtn.setOnClickListener(v -> selectAgent(DEFAULT_AGENT, docBtn));
        tableBtn.setOnClickListener(v -> selectAgent("officeTable", tableBtn));
        slideBtn.setOnClickListener(v -> selectAgent("officeSlide", slideBtn));
        mindBtn.setOnClickListener(v -> selectAgent("officeMind", mindBtn));
        selectAgent(DEFAULT_AGENT, docBtn);

        generate.setOnClickListener(v -> generate());
        App.get().getJsBridge().addListener(this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        App.get().getJsBridge().removeListener(this);
    }

    private void selectAgent(String agent, Button button) {
        currentAgent = agent;
        int selectedBg = getColorCompat(R.color.oabuod_primary);
        int normalBg = getColorCompat(R.color.oabuod_bg_light);
        int selectedText = getColorCompat(R.color.oabuod_bg);
        int normalText = getColorCompat(R.color.oabuod_text_main);
        styleButton(docBtn, button, selectedBg, normalBg, selectedText, normalText);
        styleButton(tableBtn, button, selectedBg, normalBg, selectedText, normalText);
        styleButton(slideBtn, button, selectedBg, normalBg, selectedText, normalText);
        styleButton(mindBtn, button, selectedBg, normalBg, selectedText, normalText);
    }

    private void styleButton(Button target, Button active, int selectedBg, int normalBg, int selectedText, int normalText) {
        boolean selected = target == active;
        target.setBackgroundTintList(android.content.res.ColorStateList.valueOf(selected ? selectedBg : normalBg));
        target.setTextColor(selected ? selectedText : normalText);
    }

    private int getColorCompat(int res) {
        return androidx.core.content.ContextCompat.getColor(requireContext(), res);
    }

    private void generate() {
        String text = input.getText().toString().trim();
        if (text.isEmpty()) return;

        current = TaskResult.generating();
        results.add(0, current);
        adapter.notifyItemInserted(0);
        status.setText(R.string.task_generating);
        input.setText("");

        App.get().getEngine().sendMessage(text, currentAgent, new DoubaoEngine.JsCallback() {
            @Override
            public void onResult(String raw) {
                String result = decodeString(raw);
                if ("no-input".equals(result)) {
                    requireActivity().runOnUiThread(() -> {
                        results.remove(current);
                        current = null;
                        adapter.notifyDataSetChanged();
                        status.setText(R.string.task_no_result);
                        Toast.makeText(requireContext(), R.string.task_generating, Toast.LENGTH_SHORT).show();
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
                if (current == null) {
                    current = TaskResult.generating();
                    results.add(0, current);
                    adapter.notifyItemInserted(0);
                }
                if (!text.isEmpty()) {
                    current.text += text;
                    current.streaming = false;
                }
                if (images != null && images.length() > 0) {
                    List<String> urls = new ArrayList<>();
                    for (int i = 0; i < images.length(); i++) {
                        urls.add(images.optString(i));
                    }
                    current.images = urls;
                    current.streaming = false;
                }
                status.setText(R.string.task_result_hint);
                adapter.notifyItemChanged(results.indexOf(current));
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
}
