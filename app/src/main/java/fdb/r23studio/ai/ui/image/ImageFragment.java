package fdb.r23studio.ai.ui.image;

import fdb.r23studio.ai.R;
import fdb.r23studio.ai.ui.task.AgentTaskFragment;

public class ImageFragment extends AgentTaskFragment {

    @Override
    protected String getAgentKey() {
        return "image";
    }

    @Override
    protected int getTitleRes() {
        return R.string.nav_image;
    }
}
