package io.github.leawind.gitparcel.mc.client.mui;

import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.markflow.Markflow;
import icyllis.modernui.markflow.MarkflowPlugin;
import icyllis.modernui.markflow.MarkflowTheme;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.text.Editable;
import icyllis.modernui.text.Spannable;
import icyllis.modernui.text.Typeface;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.TextView;
import io.github.leawind.gitparcel.mc.client.GitParcelClient;
import org.jspecify.annotations.NonNull;

public class GitParcelDebugFragment extends Fragment implements ScreenCallback {

  private Markflow mMarkflow;

  public GitParcelDebugFragment() {}

  @Override
  public void onCreate(DataSet savedInstanceState) {
    super.onCreate(savedInstanceState);
    var builder = Markflow.builder(requireContext());
    Typeface monoFont = Typeface.getSystemFont("JetBrains Mono Medium");
    if (monoFont != Typeface.SANS_SERIF) {
      builder.usePlugin(
          new MarkflowPlugin() {
            @Override
            public void configureTheme(MarkflowTheme.@NonNull Builder builder) {
              builder.codeTypeface(monoFont);
            }
          });
    }
    mMarkflow = builder.build();
  }

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {

    TextView preview = new TextView(requireContext());
    int dp6 = preview.dp(6);
    preview.setPadding(dp6, dp6, dp6, dp6);
    preview.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG_LTR);
    preview.setTextAlignment(View.TEXT_ALIGNMENT_TEXT_START);
    preview.setTextIsSelectable(true);
    preview.setSpannableFactory(Spannable.NO_COPY_FACTORY);
    preview.setEditableFactory(Editable.NO_COPY_FACTORY);

    mMarkflow.setMarkdown(preview, getFormatSpecsText());

    return preview;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private static String getFormatSpecsText() {
    var specs = GitParcelClient.PARCEL_FORMAT_SPECS;
    if (specs == null) {
      return "Parcel formats unavailable\n";
    }

    var sb = new StringBuilder();
    sb.append("### Supported Parcel Formats\n\n");

    for (var spec : specs.toSet().stream().sorted().toList()) {
      sb.append("- `").append(spec).append("`: ");

      if (specs.hasSaver(spec)) {
        if (specs.hasLoader(spec)) {
          sb.append("Save and Load");
        } else {
          sb.append("Save");
        }
      } else {
        sb.append("Load");
      }
      sb.append("\n");
    }

    return sb.toString();
  }
}
