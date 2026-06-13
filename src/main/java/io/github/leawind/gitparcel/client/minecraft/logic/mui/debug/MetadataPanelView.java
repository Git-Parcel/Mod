package io.github.leawind.gitparcel.client.minecraft.logic.mui.debug;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.core.Context;
import icyllis.modernui.markflow.Markflow;
import icyllis.modernui.markflow.MarkflowPlugin;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.view.View;
import icyllis.modernui.widget.ScrollView;
import icyllis.modernui.widget.TextView;
import io.github.leawind.gitparcel.client.api.GitParcelClient;
import io.github.leawind.gitparcel.common.api.GitParcel;

public class MetadataPanelView extends ScrollView {

  private final TextView mPreview;
  private final Markflow mMarkflow;

  private MetadataPanelView(@NonNull Context context, @NonNull Markflow markflow) {
    super(context);
    mMarkflow = markflow;

    int dp6 = dp(6);
    setPadding(dp6, dp6, dp6, dp6);

    mPreview = new TextView(context);
    mPreview.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
    mPreview.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
    mPreview.setTextIsSelectable(true);
    mPreview.setSpannableFactory(Spannable.NO_COPY_FACTORY);
    mPreview.setEditableFactory(Editable.NO_COPY_FACTORY);

    addView(mPreview);
  }

  public static MetadataPanelView create(@NonNull Context context) {
    var builder = Markflow.builder(context);
    Typeface monoFont = Typeface.getSystemFont("JetBrains Mono Medium");
    if (monoFont != Typeface.SANS_SERIF) {
      builder.usePlugin(
          new MarkflowPlugin() {
            @Override
            public void configureTheme(@NonNull MarkflowTheme.Builder builder) {
              builder.codeTypeface(monoFont);
            }
          });
    }
    var markflow = builder.build();
    var view = new MetadataPanelView(context, markflow);
    view.refresh();
    return view;
  }

  public void refresh() {
    var specs = GitParcelClient.get().getParcelFormatSpecs();
    if (specs == null) {
      mMarkflow.setMarkdown(mPreview, "Parcel formats unavailable\n");
      return;
    }

    var sb = new StringBuilder();
    sb.append("## Metadata\n\n");

    sb.append("- Protocol version: ").append(GitParcel.PROTOCOL_VERSION).append("\n");
    sb.append("- World parcel count: ")
        .append(GitParcelClient.get().getParcels().size())
        .append("\n");

    sb.append("\n");
    sb.append(getFormatsText());

    mMarkflow.setMarkdown(mPreview, sb);
  }

  private String getFormatsText() {
    var specs = GitParcelClient.get().getParcelFormatSpecs();
    if (specs == null) {
      return "Parcel formats unavailable\n";
    }

    var sb = new StringBuilder();
    sb.append("### Registered Parcel Formats\n\n");

    for (var spec : specs.toSet().stream().sorted().toList()) {
      sb.append("- `").append(spec).append("`: ");

      if (specs.hasSaver(spec)) {
        if (specs.hasLoader(spec)) {
          sb.append("Save and Load");
        } else {
          sb.append("Save only");
        }
      } else {
        sb.append("Load only");
      }
      sb.append("\n");
    }

    return sb.toString();
  }
}
