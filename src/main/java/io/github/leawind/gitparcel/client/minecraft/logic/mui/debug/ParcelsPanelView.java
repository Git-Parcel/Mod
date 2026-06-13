package io.github.leawind.gitparcel.client.minecraft.logic.mui.debug;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.view.Gravity;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.client.minecraft.logic.mui.MuiHelper;

public class ParcelsPanelView extends ScrollView {

  private final LinearLayout mContent;
  private final TextView mEmptyHint;

  public ParcelsPanelView(@NonNull Context context) {
    super(context);
    setPadding(dp(8), dp(8), dp(8), dp(8));

    mContent = new LinearLayout(context);
    mContent.setOrientation(LinearLayout.VERTICAL);
    mContent.setClipToPadding(false);

    mEmptyHint = new TextView(context);
    mEmptyHint.setText("No parcels in this dimension.");
    mEmptyHint.setTextSize(16);
    mEmptyHint.setGravity(Gravity.CENTER);
    mEmptyHint.setPadding(0, dp(24), 0, dp(24));
    mEmptyHint.setVisibility(View.GONE);
    mContent.addView(mEmptyHint);

    addView(mContent);

    refresh();
  }

  public void refresh() {
    var parcels = GitParcelClient.get().getParcels();
    mContent.removeAllViews();

    if (parcels.isEmpty()) {
      mContent.addView(mEmptyHint);
      mEmptyHint.setVisibility(View.VISIBLE);
      return;
    }

    mEmptyHint.setVisibility(View.GONE);

    // Category title
    MuiHelper.addCategoryTitle(mContent, "Parcels");

    // Parcel card container using outlined card
    var cardContainer = MuiHelper.createOutlinedCard(getContext());
    // Remove padding for full-width cards inside
    cardContainer.setPadding(0, 0, 0, 0);

    boolean first = true;
    for (var parcel : parcels.values()) {
      if (!first) {
        // Divider between cards using theme color
        var dividerDrawable = new icyllis.modernui.graphics.drawable.ShapeDrawable();
        dividerDrawable.setShape(icyllis.modernui.graphics.drawable.ShapeDrawable.HLINE);
        var value = new icyllis.modernui.resources.TypedValue();
        var theme = getContext().getTheme();
        if (theme.resolveAttribute(
            icyllis.modernui.R.ns, icyllis.modernui.R.attr.colorOutlineVariant, value, true)) {
          dividerDrawable.setColor(theme.getResources().loadColorStateList(value, null, theme));
        }
        dividerDrawable.setSize(dp(1), dp(1));

        var divider = new View(getContext());
        divider.setLayoutParams(
            new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1)));
        divider.setBackground(dividerDrawable);
        cardContainer.addView(divider);
      }
      first = false;

      var card = new ParcelCardView(getContext(), parcel);
      // Override layout params for inside the container
      card.setLayoutParams(
          new LinearLayout.LayoutParams(
              LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
      // Remove outer card background since we're inside a container card
      card.setBackground(null);
      cardContainer.addView(card);
    }

    mContent.addView(cardContainer);
  }
}
