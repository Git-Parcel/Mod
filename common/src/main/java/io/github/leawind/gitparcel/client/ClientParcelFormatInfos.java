package io.github.leawind.gitparcel.client;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.leawind.gitparcel.api.parcel.ParcelFormat;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record ClientParcelFormatInfos(
    List<ParcelFormat.Info> savers, List<ParcelFormat.Info> loaders) {
  public static final Codec<ClientParcelFormatInfos> CODEC =
      RecordCodecBuilder.create(
          inst ->
              inst.group(
                      ParcelFormat.Info.CODEC
                          .listOf()
                          .fieldOf("savers")
                          .forGetter(ClientParcelFormatInfos::savers),
                      ParcelFormat.Info.CODEC
                          .listOf()
                          .fieldOf("loaders")
                          .forGetter(ClientParcelFormatInfos::loaders))
                  .apply(inst, ClientParcelFormatInfos::new));

  /**
   * Cache of the parcel format infos received from the server.
   *
   * <p>Updated when received {@code UpdateParcelFormatInfosS2CPacket}.
   *
   * <p>Should be cleared when the client disconnects from the server.
   */
  public static @Nullable ClientParcelFormatInfos CACHE = null;
}
