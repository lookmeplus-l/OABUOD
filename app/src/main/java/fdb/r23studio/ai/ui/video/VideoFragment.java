package fdb.r23studio.ai.ui.video;

import fdb.r23studio.ai.R;
import fdb.r23studio.ai.ui.task.AgentTaskFragment;

public class VideoFragment extends AgentTaskFragment {

    @Override
    protected String getAgentKey() {
        return "video";
    }

    @Override
    protected int getTitleRes() {
        return R.string.nav_video;
    }
}
