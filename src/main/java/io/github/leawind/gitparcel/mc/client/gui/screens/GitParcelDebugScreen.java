package io.github.leawind.gitparcel.mc.client.gui.screens;

import icyllis.modernui.core.Context;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.mc.ScreenCallback;
import icyllis.modernui.mc.SimpleScreen;
import icyllis.modernui.util.DataSet;
import icyllis.modernui.view.LayoutInflater;
import icyllis.modernui.view.View;
import icyllis.modernui.view.ViewGroup;
import icyllis.modernui.widget.Button;
import icyllis.modernui.widget.LinearLayout;
import icyllis.modernui.widget.TextView;
import io.github.leawind.gitparcel.core.GitParcelTranslations;
import io.github.leawind.gitparcel.mc.client.GitParcelClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GitParcelDebugScreen extends SimpleScreen {

  private static final Component TITLE = GitParcelTranslations.of("gui.gitparcel.debug");

  public GitParcelDebugScreen(@Nullable Screen lastScreen) {
    super(new DebugFragment(lastScreen), null, lastScreen, TITLE);
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

  public static class DebugFragment extends Fragment implements ScreenCallback {

    private final @Nullable Screen lastScreen;

    DebugFragment(@Nullable Screen lastScreen) {
      this.lastScreen = lastScreen;
    }

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

      Button backButton = new Button(context, null);
      backButton.setText("返回");
      backButton.setOnClickListener(v -> Minecraft.getInstance().setScreen(lastScreen));
      root.addView(
          backButton,
          new LinearLayout.LayoutParams(
              ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

      return root;
    }

    @Override
    public boolean isPauseScreen() {
      return false;
    }
  }
}
