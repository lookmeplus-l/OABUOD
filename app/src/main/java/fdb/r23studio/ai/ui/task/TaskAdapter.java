package fdb.r23studio.ai.ui.task;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import fdb.r23studio.ai.R;
import fdb.r23studio.ai.model.TaskResult;
import fdb.r23studio.ai.util.ImageLoader;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.VH> {

    private final List<TaskResult> results;

    public TaskAdapter(List<TaskResult> results) {
        this.results = results;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_task_result, parent, false);
        return new VH(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        TaskResult result = results.get(position);

        holder.image.setVisibility(View.GONE);
        if (result.images != null && !result.images.isEmpty()) {
            holder.image.setVisibility(View.VISIBLE);
            ImageLoader.load(result.images.get(0), holder.image);
        }

        if (result.streaming && result.text.isEmpty() && (result.images == null || result.images.isEmpty())) {
            holder.text.setText(R.string.task_generating);
        } else {
            holder.text.setText(result.text);
        }
    }

    @Override
    public int getItemCount() {
        return results.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        ImageView image;
        TextView text;

        VH(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.result_image);
            text = itemView.findViewById(R.id.result_text);
        }
    }
}
