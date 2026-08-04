package net.turtleboi.noblephantasms.effect;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.effect.custom.CovenantEffect;
import net.turtleboi.noblephantasms.effect.custom.JudgementEffect;
import net.turtleboi.noblephantasms.effect.custom.LuminousEffect;
import net.turtleboi.noblephantasms.effect.custom.RebornEffect;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(BuiltInRegistries.MOB_EFFECT, NoblePhantasms.MOD_ID);

    public static final DeferredHolder<MobEffect, CovenantEffect> COVENANT =
            EFFECTS.register("covenant", CovenantEffect::new);

    public static final DeferredHolder<MobEffect, RebornEffect> REBORN =
            EFFECTS.register("reborn", RebornEffect::new);

    public static final DeferredHolder<MobEffect, JudgementEffect> JUDGEMENT =
            EFFECTS.register("judgement", JudgementEffect::new);

    public static final DeferredHolder<MobEffect, LuminousEffect> LUMINOUS =
            EFFECTS.register("luminous", LuminousEffect::new);

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
