package io.github.leawind.gitparcel.client.minecraft.logic.mui.debug;

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import io.github.leawind.gitparcel.client.minecraft.logic.mui.MuiHelper;
import net.minecraft.client.Minecraft;

public class ControlPanelView extends ScrollView {

  private final LinearLayout mContent;

  public ControlPanelView(@NonNull Context context) {
    super(context);
    int dp8 = dp(8);
    setPadding(dp8, dp8, dp8, dp8);

    mContent = new LinearLayout(context);
    mContent.setOrientation(LinearLayout.VERTICAL);
    mContent.setClipToPadding(false);

    addView(mContent);

    refresh();
  }

  public void refresh() {
    var context = getContext();
    mContent.removeAllViews();

    // === Actions ===
    MuiHelper.addCategoryTitle(mContent, "Actions");

    var actionsCard = MuiHelper.createOutlinedCard(context);

    var actions = new LinearLayout(context);
    actions.setOrientation(LinearLayout.HORIZONTAL);

    {
      var btnForceSync = new Button(context, null, R.attr.buttonOutlinedStyle);
      btnForceSync.setText("Remove all parcels");
      btnForceSync.setTextSize(16);
      btnForceSync.setOnClickListener(
          ignored -> {
            var mc = Minecraft.getInstance();
            if (mc.player != null) {
              mc.player.connection.sendCommand("parcel #a delete");
            }
          });
      actions.addView(btnForceSync, MuiHelper.lpButton(dp(4)));
    }

    actionsCard.addView(actions);

    mContent.addView(actionsCard);
  }
}
