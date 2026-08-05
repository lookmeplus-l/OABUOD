package fdb.r23studio.ai.ui.chat;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fdb.r23studio.ai.R;
import fdb.r23studio.ai.model.ChatMessage;
import fdb.r23studio.ai.util.ImageLoader;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.VH> {

    private final List<ChatMessage> messages;

    public ChatAdapter(List<ChatMessage> messages) {
        this.messages = messages;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_message, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ChatMessage msg = messages.get(position);

        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) holder.bubble.getLayoutParams();
        lp.gravity = msg.role == ChatMessage.ROLE_USER ? Gravity.END : Gravity.START;
        holder.bubble.setLayoutParams(lp);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(16f);
        if (msg.role == ChatMessage.ROLE_USER) {
            bg.setColor(Color.parseColor("#16498C"));
            holder.text.setTextColor(Color.WHITE);
        } else {
            bg.setColor(Color.parseColor("#E9EFF7"));
            holder.text.setTextColor(Color.parseColor("#1A1A1A"));
        }
        holder.bubble.setBackground(bg);

        if (msg.streaming && msg.text.isEmpty()) {
            holder.text.setText(R.string.chat_thinking);
        } else {
            holder.text.setText(msg.text);
        }

        holder.images.removeAllViews();
        if (msg.images != null && !msg.images.isEmpty()) {
            holder.images.setVisibility(View.VISIBLE);
            for (String src : msg.images) {
                ImageView iv = new ImageView(holder.images.getContext());
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(holder, 240), LinearLayout.LayoutParams.WRAP_CONTENT);
                ilp.topMargin = dp(holder, 6);
                iv.setLayoutParams(ilp);
                iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                holder.images.addView(iv);
                ImageLoader.load(src, iv);
            }
        } else {
            holder.images.setVisibility(View.GONE);
        }
    }

    private int dp(VH holder, int value) {
        return Math.round(value * holder.itemView.getResources().getDisplayMetrics().density);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        LinearLayout bubble;
        TextView text;
        LinearLayout images;

        VH(@NonNull View itemView) {
            super(itemView);
            bubble = itemView.findViewById(R.id.bubble_container);
            text = itemView.findViewById(R.id.msg_text);
            images = itemView.findViewById(R.id.msg_images);
        }
    }
}
