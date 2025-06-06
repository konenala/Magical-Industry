package com.github.nalamodikk.client.event;

import com.github.nalamodikk.KoniavacraftMod;
import com.github.nalamodikk.narasystem.nara.network.server.NaraSyncPacket;
import com.github.nalamodikk.narasystem.nara.network.client.OpenNaraInitScreenPacket;
import com.github.nalamodikk.narasystem.nara.util.NaraHelper;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.slf4j.Logger;
@EventBusSubscriber(modid = KoniavacraftMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)

public class PlayerLoginEvent {
    public static final Logger LOGGER = LogUtils.getLogger();

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        PacketDistributor.sendToPlayer(player, new NaraSyncPacket(NaraHelper.isBound(player)));

        if (!NaraHelper.isBound(player)) {
            PacketDistributor.sendToPlayer(player, new OpenNaraInitScreenPacket());
            LOGGER.debug("open player one login gui!");
        }
    }
}
