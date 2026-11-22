package ru.komchas;

import com.mojang.logging.LogUtils;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import ru.komchas.system.GameEvents;

/**
 * «Комендантский час»: хоррор-сборка для двух игроков (Вета и Соня).
 * Этот мод держит всю игровую логику: расписание, сирены, Метку, мысли, диалоги и существ Хора.
 */
@Mod(KomChas.MODID)
public class KomChas {
    public static final String MODID = "komchas";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KomChas() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModSounds.SOUNDS.register(modBus);
        ModEntities.ENTITIES.register(modBus);
        modBus.addListener(ModEntities::onAttributes);
        MinecraftForge.EVENT_BUS.register(new GameEvents());
    }
}
