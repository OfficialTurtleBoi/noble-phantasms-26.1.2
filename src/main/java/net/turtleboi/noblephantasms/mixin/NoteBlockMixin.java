package net.turtleboi.noblephantasms.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.NoteBlock;
import net.turtleboi.noblephantasms.block.entity.TrophyHeadBlockEntity;
import net.turtleboi.noblephantasms.item.custom.TrophyHeadItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NoteBlock.class)
public abstract class NoteBlockMixin {
    @Inject(method = "getCustomSoundId", at = @At("HEAD"), cancellable = true)
    private void noblePhantasms$getTrophySound(Level level, BlockPos pos,
                                                CallbackInfoReturnable<Identifier> cir) {
        if (level.getBlockEntity(pos.above()) instanceof TrophyHeadBlockEntity trophyHead) {
            cir.setReturnValue(TrophyHeadItem.getNoteBlockSound(trophyHead.getTrophyData(), level));
        }
    }
}
