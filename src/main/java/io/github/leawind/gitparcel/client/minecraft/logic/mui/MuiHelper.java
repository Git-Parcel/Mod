package io.github.leawind.gitparcel.client.minecraft.logic.mui;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

import icyllis.modernui.R;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.resources.TypedValue;
import icyllis.modernui.util.ColorStateList;
import icyllis.modernui.util.StateSet;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.Switch;
import icyllis.modernui.widget.TextView;
import net.minecraft.client.Minecraft;

public final class MuiHelper {

  private MuiHelper() {}

  /** Create an outlined card container, similar to PreferencesFragment.createCategoryList. */
  public static LinearLayout createOutlinedCard(Context context) {
    var layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setClipToPadding(false);

    var value = new TypedValue();
    var theme = context.getTheme();

    // Card background with rounded corners and outline
    final int dp12 = layout.dp(12);
    var bg = new ShapeDrawable();
    bg.setCornerRadius(dp12);
    if (theme.resolveAttribute(R.ns, R.attr.colorSurface, value, true)) {
      bg.setColor(theme.getResources().loadColorStateList(value, null, theme));
    }

    int[] strokeColors = new int[2];
    theme.resolveAttribute(R.ns, R.attr.colorOutlineVariant, value, true);
    strokeColors[0] = value.data;
    theme.resolveAttribute(R.ns, R.attr.colorOutline, value, true);
    strokeColors[1] = ColorStateList.modulateColor(value.data, 0.12f);
    bg.setStroke(
        layout.dp(1),
        new ColorStateList(
            new int[][] {StateSet.get(StateSet.VIEW_STATE_ENABLED), StateSet.WILD_CARD},
            strokeColors));

    layout.setBackground(bg);
    layout.setPadding(dp12, dp12, dp12, dp12);

    var params = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    params.setMargins(0, layout.dp(6), 0, layout.dp(6));
    layout.setLayoutParams(params);

    return layout;
  }

  /** Create a category title using colorPrimary. */
  public static void addCategoryTitle(ViewGroup parent, String title) {
    var context = parent.getContext();
    var view = new TextView(context);
    view.setId(R.id.title);
    view.setText(title);
    view.setTextSize(16);

    var value = new TypedValue();
    if (context.getTheme().resolveAttribute(R.ns, R.attr.colorPrimary, value, true)) {
      view.setTextColor(context.getResources().loadColorStateList(value, null, context.getTheme()));
    }

    final int dp6 = view.dp(6);
    var params = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    params.gravity = Gravity.START;
    params.setMargins(0, parent.getChildCount() == 0 ? dp6 : view.dp(18), 0, 0);
    parent.addView(view, params);
  }

  /** Create a standardized Switch row (label on left, switch on right). */
  public static Switch createSwitchRow(LinearLayout parent, String label) {
    var context = parent.getContext();
    final int dp6 = parent.dp(6);

    var layout = new LinearLayout(context);
    layout.setOrientation(LinearLayout.HORIZONTAL);
    layout.setHorizontalGravity(Gravity.START);
    layout.setMinimumHeight(parent.dp(44));

    var title = new TextView(context);
    title.setText(label);
    title.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
    title.setTextSize(14);
    var titleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT, 1);
    titleParams.gravity = Gravity.CENTER_VERTICAL;
    layout.addView(title, titleParams);

    var toggle = new Switch(context);
    toggle.setId(R.id.button1);
    var toggleParams = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    toggleParams.gravity = Gravity.CENTER_VERTICAL;
    layout.addView(toggle, toggleParams);

    var layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT);
    layoutParams.gravity = Gravity.CENTER;
    layoutParams.setMargins(dp6, 0, dp6, 0);
    layout.setLayoutParams(layoutParams);

    parent.addView(layout);
    return toggle;
  }

  public static LinearLayout.LayoutParams lpButton(int marginEnd) {
    var lp = new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT);
    lp.setMarginEnd(marginEnd);
    return lp;
  }

  public static void sendCommand(String command) {
    var mc = Minecraft.getInstance();
    if (mc.player != null) {
      mc.player.connection.sendCommand(command);
    }
  }
}
