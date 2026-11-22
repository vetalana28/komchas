package ru.komchas.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Класс I: Тихий. Стоит неподвижно, не моргает, не атакует.
 * Поворачивается к ближайшему игроку только тогда, когда на него не смотрят.
 */
public class TikhiyEntity extends PathfinderMob {
    private static final double WATCH_RANGE = 64.0;
    /** Косинус половины угла «на меня смотрят». 0.9 примерно 25 градусов. */
    private static final double OBSERVED_DOT = 0.9;

    public TikhiyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setInvulnerable(true);
        this.setNoAi(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide || this.tickCount % 5 != 0) {
            return;
        }
        Player nearest = this.level().getNearestPlayer(this, WATCH_RANGE);
        if (nearest == null || isObservedBy(nearest)) {
            return; // пока на него смотрят, он замирает
        }
        double dx = nearest.getX() - this.getX();
        double dz = nearest.getZ() - this.getZ();
        float yaw = (float) (Mth.atan2(dz, dx) * (180.0 / Math.PI)) - 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
    }

    private boolean isObservedBy(Player player) {
        Vec3 look = player.getViewVector(1.0F).normalize();
        Vec3 toMe = new Vec3(
                this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ()).normalize();
        return look.dot(toMe) > OBSERVED_DOT && player.hasLineOfSight(this);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSqr) {
        return false;
    }
}
