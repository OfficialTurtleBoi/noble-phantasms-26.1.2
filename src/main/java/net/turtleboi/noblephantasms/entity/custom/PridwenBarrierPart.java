package net.turtleboi.noblephantasms.entity.custom;

import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.PartEntity;
import org.jspecify.annotations.Nullable;

public final class PridwenBarrierPart extends PartEntity<PridwenBarrierEntity> {
    public PridwenBarrierPart(PridwenBarrierEntity parent) {
        super(parent);
        noPhysics = true;
    }

    public void updateBounds(Vec3 center, AABB bounds) {
        xo = getX();
        yo = getY();
        zo = getZ();
        xOld = getX();
        yOld = getY();
        zOld = getZ();
        setPos(center.x, center.y, center.z);
        setBoundingBox(bounds);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeHitByProjectile() {
        return isAlive();
    }

    public boolean canBlockProjectile(Projectile projectile) {
        return getParent().canBlockProjectile(projectile);
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean is(Entity other) {
        return this == other || getParent() == other;
    }

    @Override
    public @Nullable ItemStack getPickResult() {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return getParent().absorbProjectileImpact(level, source, amount, position());
    }
}
