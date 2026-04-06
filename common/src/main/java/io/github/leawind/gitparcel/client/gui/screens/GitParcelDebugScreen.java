package io.github.leawind.gitparcel.client.gui.screens;

import io.github.leawind.gitparcel.GitParcelTranslations;
import io.github.leawind.gitparcel.client.GitParcelClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class GitParcelDebugScreen extends Screen {
  private static final Component TITLE = GitParcelTranslations.of("gui.gitparcel.debug");

  private @Nullable final Screen lastScreen;
  final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

  public GitParcelDebugScreen(@Nullable Screen lastScreen) {
    super(TITLE);
    this.lastScreen = lastScreen;
  }

  private String getFormatInfos() {
    var sb = new StringBuilder();
    sb.append("Supported Parcel Formats\n");
    var infos = GitParcelClient.PARCEL_FORMAT_INFOS;

    if (infos == null) {
      sb.append("Unavailable\n");
    } else {
      sb.append("  Savers:\n");
      infos.savers().forEach(info -> sb.append("    ").append(info).append('\n'));
      sb.append("  Loaders:\n");
      infos.loaders().forEach(info -> sb.append("    ").append(info).append('\n'));
    }

    return sb.toString();
  }

  @Override
  protected void init() {
    layout.addTitleHeader(TITLE, font);

    layout.addToContents(new MultiLineTextWidget(Component.literal(getFormatInfos()), font));

    layout.addToFooter(
        Button.builder(CommonComponents.GUI_BACK, button -> onClose()).width(80).build());

    layout.visitWidgets(this::addRenderableWidget);

    repositionElements();
  }

  @Override
  protected void repositionElements() {
    layout.setHeaderHeight(60);
    layout.arrangeElements();
  }

  @Override
  public void render(@NonNull GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
    super.render(guiGraphics, mouseX, mouseY, delta);
  }

  @Override
  public void onClose() {
    minecraft.setScreen(lastScreen);
  }
}
