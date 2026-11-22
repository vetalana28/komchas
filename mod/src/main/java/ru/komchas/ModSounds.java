package ru.komchas;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, KomChas.MODID);

    /** Сирена оповещения. Пока ссылается на ванильный горн, потом заменяется своей записью. */
    public static final RegistryObject<SoundEvent> SIREN = register("siren");
    /** Детский смех во дворе. Пока заглушка. */
    public static final RegistryObject<SoundEvent> CHILDREN_LAUGH = register("children_laugh");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(KomChas.MODID, name)));
    }

    private ModSounds() {}
}
