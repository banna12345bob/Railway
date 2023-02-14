package com.railwayteam.railways.util.packet;

import com.railwayteam.railways.Railways;
import com.railwayteam.railways.multiloader.environment.Env;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.simibubi.create.foundation.utility.Components;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ModVersionPacket extends SimplePacketBase {

  final String version;

  public ModVersionPacket(String version) {
    this.version = version;
  }

  public ModVersionPacket(FriendlyByteBuf buf) {
    version = buf.readUtf();
  }

  @Override
  public void write(FriendlyByteBuf buffer) {
    buffer.writeUtf(this.version);
  }

  @Override
  public void handle(Supplier<NetworkEvent.Context> context) {
    context.get().enqueueWork(()-> Env.CLIENT.runIfCurrent(()-> ()-> this.__handle(context)));
    context.get().setPacketHandled(true);
  }

  @Environment(EnvType.CLIENT)
  private void __handle(Supplier<NetworkEvent.Context> supplier) {
    if (!Railways.VERSION.equals(version) && Minecraft.getInstance().player != null) {
      String msg = "Steam 'n Rails version mismatch: Server is using version "+version+", you are using version "+Railways.VERSION+". This may cause problems.";
      Railways.LOGGER.warn(msg);
      Minecraft.getInstance().player.displayClientMessage(
          Components.literal(msg)
              .withStyle(ChatFormatting.DARK_RED),
          false);
    }
  }
}