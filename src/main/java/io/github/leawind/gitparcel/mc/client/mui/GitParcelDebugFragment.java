package io.github.leawind.gitparcel.mc.client.mui;

import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import io.github.leawind.gitparcel.mc.client.GitParcelClient;
import org.jspecify.annotations.NonNull;

public class GitParcelDebugFragment extends Fragment implements ScreenCallback {

  public GitParcelDebugFragment() {}

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, DataSet savedInstanceState) {
    Context context = requireContext();

    LinearLayout root = new LinearLayout(context);
    root.setOrientation(LinearLayout.VERTICAL);

    TextView textView = new TextView(context);
    textView.setText(getFormatSpecs());
    textView.setTextSize(16);
    root.addView(
        textView,
        new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

    return root;
  }

  @Override
  public boolean isPauseScreen() {
    return false;
  }

  private static String getFormatSpecs() {
    var sb = new StringBuilder();
    sb.append("Supported Parcel Formats\n");
    var specs = GitParcelClient.PARCEL_FORMAT_SPECS;

    if (specs == null) {
      sb.append("Unavailable\n");
    } else {
      sb.append("  Savers:\n");
      specs.savers().forEach(spec -> sb.append("    ").append(spec).append('\n'));
      sb.append("  Loaders:\n");
      specs.loaders().forEach(spec -> sb.append("    ").append(spec).append('\n'));
    }

    return sb.toString();
  }
}
