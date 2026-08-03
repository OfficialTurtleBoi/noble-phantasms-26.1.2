package net.turtleboi.noblephantasms.datagen.providers;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.item.ModItems;

public class ModLanguageProvider extends LanguageProvider {
    public ModLanguageProvider(PackOutput output) {
        super(output, NoblePhantasms.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        addSimpleItem(ModItems.ANKH);
        addSimpleItem(ModItems.KHEPER_SCARAB, "Kheper Scarab");
        addSimpleItem(ModItems.SCALES_OF_MAAT, "Scales of Ma'at");
        addSimpleItem(ModItems.SCABBARD, "The Scabbard");
        addSimpleItem(ModItems.BERTILAK);
        addSimpleItem(ModItems.TROPHY_HEAD, "Trophy Head");
        add(ModBlocks.TROPHY_HEAD.get(), "Trophy Head");
        addSimpleItem(ModItems.CARNWENNAN);
        addSimpleItem(ModItems.EXCALIBUR);
        addSimpleItem(ModItems.GJALLARHORN);
        addSimpleItem(ModItems.HULIOSHJALMR, "Huliðshjálmr");
        addSimpleItem(ModItems.EYE_OF_HORUS, "Eye of Horus");
        addSimpleItem(ModItems.GUNGNIR);
        addSimpleItem(ModItems.KHOPESH_OF_RA, "Khopesh of Ra");
        addSimpleItem(ModItems.KUSANAGI_NO_TSURUGI, "Kusanagi-no-Tsurugi");
        addSimpleItem(ModItems.KAZAGURUMA);
        addSimpleItem(ModItems.RHONGOMYNIAD);
        addSimpleItem(ModItems.UCHIDE_NO_KOZUCHI, "Uchide no Kozuchi");
        addSimpleItem(ModItems.YAMAWARI);
        addSimpleItem(ModItems.ANDVARANAUT);
        addSimpleItem(ModItems.DRAUPNIR);
        addSimpleItem(ModItems.EAGLE_KNIGHT_TALONS, "Eagle Knight Talons");
        addSimpleItem(ModItems.MEGINGJORD, "Megingjörð");
        addSimpleItem(ModItems.HEKA);
        addSimpleItem(ModItems.NEKHAKHA);
        addSimpleItem(ModItems.BOOK_OF_THOTH, "Book of Thoth");
        add("tooltip.noblephantasms.book_of_thoth.reroll", "Turn the page (1 Lapis Lazuli)");
        add("tooltip.noblephantasms.book_of_thoth.offer", "Book of Thoth reveals:");
        add("jei.noblephantasms.info.ankh",
                "Equip in a Curios charm or compatible totem slot. Fatal damage restores full health, "
                        + "releases a damaging and blinding radiant burst, and grants 15 seconds of invulnerability. "
                        + "Reborn prevents another activation for 2 minutes.");
        add("jei.noblephantasms.info.eye_of_horus",
                "Equip in a Curios necklace slot. Hold your crosshair on a living target for 2 seconds to apply "
                        + "Judgement for 15 seconds, making it glow and take 25% more damage from all sources.");
        add("jei.noblephantasms.info.kheper_scarab",
                "Equip in a Curios charm slot. Repairs each damaged item in your hands and armor slots by "
                        + "1 durability every 2 seconds.");
        add("jei.noblephantasms.info.scales_of_maat",
                "Right-click a living target to set your health percentage and theirs to their average. "
                        + "Bosses can lose at most 10% of their maximum health per use. 30 second cooldown.");
        add("jei.noblephantasms.info.andvaranaut",
                "Equip in a Curios ring slot. Mob loot drops are doubled, but all damage you take is doubled.");
        add("jei.noblephantasms.info.draupnir",
                "Carry Draupnir to generate gold every 30 seconds. Equip it in a Curios ring slot to make piglins "
                        + "treat you as though you are wearing gold armor.");
        add("jei.noblephantasms.info.meginjord",
                "Equip in a Curios belt slot. Grants +4 melee damage, increased knockback and upward launch, "
                        + "50% block-breaking speed, and immunity to environmental movement penalties.");
        add("jei.noblephantasms.info.scabbard",
                "Equip in a Curios belt slot. Constantly regenerates health faster as your health falls and "
                        + "prevents poison, wither, and bleed effects.");
        add("jei.noblephantasms.info.book_of_thoth",
                "Use the Book of Thoth on an enchanting table to install it. "
                        + "Sneak-right-click the table with an empty hand to remove it. "
                        + "While installed, it reveals enchantment offers and lets you reroll them for 1 Lapis Lazuli.");
        add("jei.noblephantasms.info.eagle_knight_talons",
                "Crouch while airborne to dive rapidly. Landing creates a 4-block shockwave that deals more damage "
                        + "the farther you fell, while negating the slam's fall damage.");
        add("jei.noblephantasms.info.kazaguruma",
                "Right-click to throw the chained sickle up to 12 blocks. It damages and hooks the first living "
                        + "target struck, then pulls the target into melee range. "
                        + "3 second cooldown.");

        add("creativetab.noblephantasms.title", "Noble Phantasms");
        add("item.noblephantasms.trophy_head.named", "%s Head");
        add("item.noblephantasms.trophy_head.baby_named", "Baby %s Head");
        add("message.noblephantasms.bertilak.already_bound", "The Covenant is already bound");
        add("message.noblephantasms.bertilak.bound", "Covenant bound to %s");
        add("message.noblephantasms.bertilak.broken", "The Covenant has been broken");
        add("message.noblephantasms.bertilak.cooldown", "The Covenant can be bound again in %s seconds");
        add("message.noblephantasms.bertilak.fulfilled", "Covenant fulfilled: %s");
        add("message.noblephantasms.bertilak.target_bound", "That target is already bound by a Covenant");
        add("effect.noblephantasms.covenant", "Covenant");
        add("effect.noblephantasms.judgement", "Judgement");
        add("effect.noblephantasms.reborn", "Reborn");
        add("noblephantasms.configuration.title", "Noble Phantasms Configs");
        add("noblephantasms.configuration.section.noblephantasms.common.toml", "Noble Phantasms Configs");
        add("noblephantasms.configuration.section.noblephantasms.common.toml.title", "Noble Phantasms Configs");
    }

    protected void addSimpleItem(DeferredItem<?> item) {
        addSimpleItem(item, toDisplayName(item.getId().getPath()));
    }

    protected void addSimpleItem(DeferredItem<?> item, String displayName) {
        add(item.get(), displayName);
    }

    private static String toDisplayName(String registryPath) {
        return Arrays.stream(registryPath.split("_"))
                .filter(word -> !word.isEmpty())
                .map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
                .collect(Collectors.joining(" "));
    }
}
