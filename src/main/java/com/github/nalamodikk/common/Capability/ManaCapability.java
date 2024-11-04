package com.github.nalamodikk.common.Capability;

import com.github.nalamodikk.common.block.entity.mana_crafting.ManaCraftingTableBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;

public class ManaCapability {
    public static final Capability<IUnifiedManaHandler> MANA = CapabilityManager.get(new CapabilityToken<>() {});

    @Mod.EventBusSubscriber
    public static class EventHandler {
        @SubscribeEvent
        public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
            if (event.getObject() instanceof ManaCraftingTableBlockEntity blockEntity) {
                // 檢查是否已經存在該能力
                if (!blockEntity.getCapability(ManaCapability.MANA).isPresent()) {
                    // 創建並注入 Mana 能力
                    event.addCapability(
                            new ResourceLocation("magical_industry", "mana"),
                            new ManaProvider()
                    );
                }
            }
        }
    }


    public static class ManaProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {
        private final IUnifiedManaHandler manaStorage = (IUnifiedManaHandler) new ManaStorage(1000);
        private final LazyOptional<IUnifiedManaHandler> lazyOptional = LazyOptional.of(() -> manaStorage);

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction side) {
            return cap == MANA ? lazyOptional.cast() : LazyOptional.empty();
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.putInt("mana", manaStorage.getMana());
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            manaStorage.setMana(nbt.getInt("mana"));
        }
    }
}
