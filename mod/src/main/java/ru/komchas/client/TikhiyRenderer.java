package ru.komchas.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.resources.ResourceLocation;
import ru.komchas.KomChas;
import ru.komchas.entity.TikhiyEntity;

/** Рендер Тихого: человеческая модель, чуть вытянутая и тоньше нормы. Текстура почти чёрная. */
public class TikhiyRenderer extends HumanoidMobRenderer<TikhiyEntity, HumanoidModel<TikhiyEntity>> {
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(KomChas.MODID, "textures/entity/tikhiy.png");

    public TikhiyRenderer(EntityRendererProvider.Context context) {
        super(context, new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER)), 0.5F);
    }

    @Override
    protected void scale(TikhiyEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(0.92F, 1.12F, 0.92F);
    }

    @Override
    public ResourceLocation getTextureLocation(TikhiyEntity entity) {
        return TEXTURE;
    }
}
