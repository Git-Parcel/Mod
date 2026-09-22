package io.github.leawind.gitparcel.server.minecraft.logic.commands.parcel.web;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.leawind.gitparcel.common.api.permission.PermissionLevel;
import io.github.leawind.gitparcel.common.minecraft.logic.permission.MinecraftPermissions;
import io.github.leawind.gitparcel.common.utils.Translations;
import io.github.leawind.gitparcel.server.minecraft.logic.commands.GitParcelBaseCommand;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebService;
import io.github.leawind.gitparcel.server.minecraft.logic.web.WebServiceManager;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.UnknownHostException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/**
 * Starts and stops the browser management console.
 *
 * <p>Requires the same permission level as vanilla {@code /publish}: opening a management entry
 * point is a server-owner action.
 */
public final class WebSubcommand extends GitParcelBaseCommand {
  public static final int DEFAULT_PORT = 5639;
  public static final String DEFAULT_BIND = "127.0.0.1";

  private static final String ARG_PORT = "port";
  private static final String ARG_BIND = "bind";

  private WebSubcommand() {}

  public static ArgumentBuilder<CommandSourceStack, ?> build() {
    return Commands.literal("web")
        .requires(MinecraftPermissions.require(PermissionLevel.OWNERS))
        .then(
            Commands.literal("start")
                .executes(ctx -> start(ctx, DEFAULT_PORT, DEFAULT_BIND))
                .then(
                    Commands.argument(ARG_PORT, IntegerArgumentType.integer(1, 65535))
                        .executes(ctx -> start(ctx, currentPort(ctx), DEFAULT_BIND))
                        .then(
                            Commands.argument(ARG_BIND, StringArgumentType.word())
                                .executes(
                                    ctx -> start(ctx, currentPort(ctx), currentBind(ctx))))))
        .then(Commands.literal("stop").executes(WebSubcommand::stop))
        .then(Commands.literal("status").executes(WebSubcommand::status));
  }

  private static int currentPort(CommandContext<CommandSourceStack> ctx) {
    return IntegerArgumentType.getInteger(ctx, ARG_PORT);
  }

  private static String currentBind(CommandContext<CommandSourceStack> ctx) {
    return StringArgumentType.getString(ctx, ARG_BIND);
  }

  private static int start(CommandContext<CommandSourceStack> ctx, int port, String bind) {
    var source = ctx.getSource();
    var manager = WebServiceManager.get(source.getServer());
    if (manager.service().isPresent()) {
      source.sendFailure(
          Translations.of(
              "command.gitparcel.parcel.web.already_running",
              entryUrl(manager.service().orElseThrow())));
      return 0;
    }

    InetAddress address;
    try {
      address = InetAddress.getByName(bind);
    } catch (UnknownHostException e) {
      source.sendFailure(Translations.of("command.gitparcel.parcel.web.invalid_bind", bind));
      return 0;
    }

    WebService service;
    try {
      service = manager.start(new InetSocketAddress(address, port));
    } catch (Exception e) {
      source.sendFailure(
          Translations.of("command.gitparcel.parcel.web.start_failure", describe(e)));
      return 0;
    }

    source.sendSystemMessage(
        Translations.of("command.gitparcel.parcel.web.started", clickableUrl(entryUrl(service))));
    if (!address.isLoopbackAddress()) {
      source.sendSystemMessage(
          Translations.of("command.gitparcel.parcel.web.public_bind_warning", bind));
    }
    return 1;
  }

  private static int stop(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    var manager = WebServiceManager.get(source.getServer());
    if (manager.service().isEmpty()) {
      source.sendFailure(Translations.of("command.gitparcel.parcel.web.not_running"));
      return 0;
    }
    manager.stop();
    source.sendSystemMessage(Translations.of("command.gitparcel.parcel.web.stopped"));
    return 1;
  }

  private static int status(CommandContext<CommandSourceStack> ctx) {
    var source = ctx.getSource();
    var manager = WebServiceManager.get(source.getServer());
    var service = manager.service();
    if (service.isEmpty()) {
      source.sendSystemMessage(Translations.of("command.gitparcel.parcel.web.not_running"));
    } else {
      source.sendSystemMessage(
          Translations.of(
              "command.gitparcel.parcel.web.status.running",
              clickableUrl(entryUrl(service.get()))));
    }
    return 1;
  }

  private static String entryUrl(WebService service) {
    var address = service.address();
    var host = displayHost(address);
    return "http://" + host + ":" + address.getPort() + "/?token=" + service.tokenHex();
  }

  /** A wildcard bind has no single reachable host; every other address renders as given. */
  private static String displayHost(InetSocketAddress address) {
    var inet = address.getAddress();
    if (inet == null || inet.isAnyLocalAddress()) {
      return "<server-address>";
    }
    return inet.isLoopbackAddress() ? "127.0.0.1" : address.getHostString();
  }

  private static Component clickableUrl(String url) {
    MutableComponent text = Component.literal(url);
    try {
      /*? if >=1.21.11 {*/
      var click = new ClickEvent.OpenUrl(URI.create(url));
      var hover = new HoverEvent.ShowText(Component.literal(url));
      /*?} else {*/
      /*var click = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
      var hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(url));
       *//*? }*/
      return text.withStyle(Style.EMPTY.withClickEvent(click).withHoverEvent(hover));
    } catch (IllegalArgumentException e) {
      // Placeholder hosts such as <server-address> are rendered as plain text.
      return text;
    }
  }
}
