package io.github.leawind.gitparcel.server.minecraft.logic.commands;

import io.github.leawind.gitparcel.common.api.snapshot.SnapshotId;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Renders abbreviated snapshot IDs as click-to-copy chat components, so players can obtain the
 * full object ID that {@code restore} requires without transcribing it from hover text.
 */
public final class SnapshotIdText {
  private SnapshotIdText() {}

  public static MutableComponent abbreviatedCopyable(SnapshotId id) {
    return abbreviatedCopyable(id.value());
  }

  public static MutableComponent abbreviatedCopyable(String objectId) {
    String abbreviated = objectId.substring(0, Math.min(8, objectId.length()));
    /*? if >=1.21.11 {*/
    var click = new ClickEvent.CopyToClipboard(objectId);
    var hover = new HoverEvent.ShowText(Component.literal(objectId));
    /*?} else {*/
    /*var click = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, objectId);
    var hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(objectId));
    *//*? }*/
    return Component.literal(abbreviated)
        .withStyle(Style.EMPTY.withClickEvent(click).withHoverEvent(hover));
  }
}
