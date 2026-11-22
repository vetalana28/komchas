package ru.komchas;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ru.komchas.entity.TikhiyEntity;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, KomChas.MODID);

    /** MISC, чтобы Тихие не появлялись сами по правилам спавна: их ставит сценарий или карта. */
    public static final RegistryObject<EntityType<TikhiyEntity>> TIKHIY = ENTITIES.register("tikhiy",
            () -> EntityType.Builder.of(TikhiyEntity::new, MobCategory.MISC)
                    .sized(0.6F, 1.95F)
                    .clientTrackingRange(10)
                    .build(KomChas.MODID + ":tikhiy"));

    public static void onAttributes(EntityAttributeCreationEvent event) {
        event.put(TIKHIY.get(), TikhiyEntity.createAttributes().build());
    }

    private ModEntities() {}
}
