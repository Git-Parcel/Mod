package io.github.leawind.gitparcel.client.minecraft.logic.mui.debug;

import static icyllis.modernui.view.ViewGroup.LayoutParams.*;

import icyllis.modernui.R;
import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.graphics.drawable.ShapeDrawable;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewManager;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.Switch;
import icyllis.modernui.widget.TextView;
import io.github.leawind.gitparcel.client.minecraft.logic.mui.MuiHelper;
import io.github.leawind.gitparcel.common.api.permission.ParcelPermissions;
import io.github.leawind.gitparcel.common.api.world.Parcel;
import net.minecraft.client.Minecraft;

public class ParcelCardView extends LinearLayout {

  private final Parcel mParcel;
  private final LinearLayout mDetailSection;
  private boolean mDetailVisible;

  public ParcelCardView(@NonNull Context context, @NonNull Parcel parcel) {
    super(context);
    mParcel = parcel;
    setOrientation(LinearLayout.VERTICAL);

    setPadding(dp(12), dp(12), dp(12), dp(12));

    // Title line (clickable to expand)
    addView(createTitleLine());

    // Spacing
    addVerticalSpace(dp(4));

    // Info line
    addView(createInfoLine());

    // Spacing
    addVerticalSpace(dp(8));

    // Button row
    addView(createButtonRow());

    // Detail section (initially hidden)
    mDetailSection = createDetailSection();
    mDetailSection.setVisibility(View.GONE);
    addView(mDetailSection);
  }

  private View createTitleLine() {
    var context = getContext();
    var meta = mParcel.meta();
    var name = meta.name() != null ? meta.name() : "Unnamed";

    var title = new TextView(context);
    title.setText(name + "    " + meta.formatSpec());
    title.setTextSize(16);
    title.setTextColor(0xFFFFFFFF);
    title.setClickable(true);
    title.setFocusable(true);
    title.setOnClickListener(ignored -> toggleDetail());
    return title;
  }

  private View createInfoLine() {
    var context = getContext();
    var meta = mParcel.meta();
    var bb = mParcel.getBoundingBox();
    var center = bb.getCenter();
    var size = meta.size();

    var info = new TextView(context);
    info.setText(
        size.getX()
            + " x "
            + size.getY()
            + " x "
            + size.getZ()
            + "   @ ("
            + center.getX()
            + ", "
            + center.getY()
            + ", "
            + center.getZ()
            + ")");
    info.setTextSize(14);
    info.setTextColor(0xFFAAAAAA);
    return info;
  }

  private View createButtonRow() {
    var context = getContext();

    var row = new LinearLayout(context);
    row.setOrientation(LinearLayout.HORIZONTAL);
    row.setGravity(Gravity.CENTER_VERTICAL);

    // Spacer pushes Teleport to the right
    var spacer = new View(context);
    row.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

    var btnTeleport = new Button(context, null, R.attr.buttonOutlinedStyle);
    btnTeleport.setText("Teleport");
    btnTeleport.setTextSize(16);
    btnTeleport.setTooltipText("Teleport to this parcel");
    btnTeleport.setOnClickListener(
        ignored -> MuiHelper.sendCommand("parcel " + mParcel.uuid() + " teleport"));
    row.addView(btnTeleport, MuiHelper.lpButton(0));

    return row;
  }

  private LinearLayout createDetailSection() {
    var context = getContext();
    int dp8 = dp(8);
    int dp4 = dp(4);

    var detail = new LinearLayout(context);
    detail.setOrientation(LinearLayout.VERTICAL);
    detail.setPadding(dp8, dp8, 0, dp8);

    var meta = mParcel.meta();
    var transform = mParcel.transform();

    appendDetailLine(detail, "UUID", mParcel.uuid().toString());
    appendDetailLine(detail, "Format", meta.formatSpec().toString());
    appendDetailLine(
        detail,
        "Size",
        meta.size().getX() + " x " + meta.size().getY() + " x " + meta.size().getZ());
    appendDetailLine(
        detail,
        "Anchor",
        meta.anchor().getX() + ", " + meta.anchor().getY() + ", " + meta.anchor().getZ());
    if (meta.description() != null) {
      appendDetailLine(detail, "Desc", meta.description());
    }

    var perm = mParcel.permissions();
    appendDetailLine(
        detail,
        "Permissions",
        "save="
            + perm.get(ParcelPermissions.SAVE)
            + " load="
            + perm.get(ParcelPermissions.LOAD)
            + " config="
            + perm.get(ParcelPermissions.CONFIG)
            + " commit="
            + perm.get(ParcelPermissions.COMMIT));

    var translation = transform.translation();
    appendDetailLine(
        detail,
        "Translation",
        translation.getX() + ", " + translation.getY() + ", " + translation.getZ());
    appendDetailLine(detail, "Rotation", transform.rotation().toString());
    appendDetailLine(detail, "Mirror", transform.mirror().toString());

    addVerticalSpaceIn(detail, dp8);

    // Action buttons row
    var actionButtons = new LinearLayout(context);
    actionButtons.setOrientation(LinearLayout.HORIZONTAL);
    actionButtons.setGravity(Gravity.CENTER_VERTICAL);

    // Toggle row: Wireframe, Show Anchor, Exclude Entities
    var toggleRow = new LinearLayout(context);
    toggleRow.setOrientation(LinearLayout.HORIZONTAL);
    toggleRow.setGravity(Gravity.CENTER_VERTICAL);

    toggleRow.addView(
        createBorderedToggle(
            "Wireframe",
            mParcel.visual().showWireframe(),
            (isChecked) ->
                MuiHelper.sendCommand(
                    "parcel " + mParcel.uuid() + " config set visual.showWireframe " + isChecked)));

    toggleRow.addView(
        createBorderedToggle(
            "Show Anchor",
            mParcel.visual().showAnchor(),
            (isChecked) ->
                MuiHelper.sendCommand(
                    "parcel " + mParcel.uuid() + " config set visual.showAnchor " + isChecked)));

    toggleRow.addView(
        createBorderedToggle(
            "Exclude Entities",
            mParcel.meta().getExcludeEntities(),
            (isChecked) ->
                MuiHelper.sendCommand(
                    "parcel " + mParcel.uuid() + " config set meta.excludeEntities " + isChecked)));

    actionButtons.addView(toggleRow);

    // Spacer pushes buttons to the right
    var spacer = new View(context);
    actionButtons.addView(spacer, new LinearLayout.LayoutParams(0, 1, 1.0f));

    var btnSave = new Button(context, null, R.attr.buttonOutlinedStyle);
    btnSave.setText("Save");
    btnSave.setTextSize(14);
    btnSave.setTooltipText("Save this parcel");
    btnSave.setOnClickListener(
        ignored -> MuiHelper.sendCommand("parcel " + mParcel.uuid() + " save"));
    actionButtons.addView(btnSave, MuiHelper.lpButton(dp4));

    var btnDelete = new Button(context, null, R.attr.buttonOutlinedStyle);
    btnDelete.setText("Delete");
    btnDelete.setTextSize(14);
    btnDelete.setTooltipText("Delete this parcel");
    btnDelete.setOnClickListener(
        ignored -> {
          MuiHelper.sendCommand("parcel " + mParcel.uuid() + " delete");
          var parent = getParent();
          if (parent instanceof ViewManager parentView) {
            parentView.removeView(this);
          }
        });
    actionButtons.addView(btnDelete, MuiHelper.lpButton(dp4));

    var btnCopyUuid = new Button(context, null, R.attr.buttonOutlinedStyle);
    btnCopyUuid.setText("Copy UUID");
    btnCopyUuid.setTextSize(14);
    btnCopyUuid.setTooltipText("Copy parcel UUID to clipboard");
    btnCopyUuid.setOnClickListener(
        ignored -> {
          var keyboard = Minecraft.getInstance().keyboardHandler;
          if (keyboard != null) {
            keyboard.setClipboard(mParcel.uuid().toString());
          }
        });
    actionButtons.addView(btnCopyUuid, MuiHelper.lpButton(0));

    detail.addView(actionButtons);

    return detail;
  }

  private View createBorderedToggle(
      String label, boolean checked, java.util.function.Consumer<Boolean> onChanged) {
    var context = getContext();

    var group = new LinearLayout(context);
    group.setOrientation(LinearLayout.HORIZONTAL);
    group.setGravity(Gravity.CENTER_VERTICAL);
    group.setPadding(dp(8), dp(4), dp(8), dp(4));

    var border = new ShapeDrawable();
    border.setShape(ShapeDrawable.RECTANGLE);
    border.setCornerRadius(dp(4));
    var value = new icyllis.modernui.resources.TypedValue();
    var theme = context.getTheme();
    if (theme.resolveAttribute(R.ns, R.attr.colorOutlineVariant, value, true)) {
      border.setStroke(dp(1), value.data);
    }
    group.setBackground(border);

    var textView = new TextView(context);
    textView.setText(label);
    textView.setTextSize(14);
    textView.setTextColor(0xFFBBBBBB);
    group.addView(textView, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1.0f));

    var toggle = new Switch(context);
    toggle.setChecked(checked);
    toggle.setOnCheckedChangeListener((buttonView, isChecked) -> onChanged.accept(isChecked));
    group.addView(toggle, new LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT));

    return group;
  }

  private void toggleDetail() {
    mDetailVisible = !mDetailVisible;
    mDetailSection.setVisibility(mDetailVisible ? View.VISIBLE : View.GONE);
  }

  private void appendDetailLine(LinearLayout parent, String label, String value) {
    var line = new TextView(getContext());
    line.setText("  " + label + ": " + value);
    line.setTextSize(13);
    line.setTextColor(0xFFBBBBBB);
    parent.addView(line);
  }

  private void addVerticalSpace(int dpValue) {
    var space = new View(getContext());
    space.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, dpValue));
    addView(space);
  }

  private void addVerticalSpaceIn(LinearLayout parent, int dpValue) {
    var space = new View(getContext());
    space.setLayoutParams(new LinearLayout.LayoutParams(MATCH_PARENT, dpValue));
    parent.addView(space);
  }
}
