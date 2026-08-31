package net.turtleboi.noblephantasms.datagen.providers;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.neoforged.neoforge.registries.DeferredItem;
import net.turtleboi.noblephantasms.NoblePhantasms;
import net.turtleboi.noblephantasms.block.ModBlocks;
import net.turtleboi.noblephantasms.entity.ModEntities;
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
        add(ModBlocks.RELIQUARY_STATION.get(), "Reliquary Station");
        addSimpleItem(ModItems.CARNWENNAN);
        addSimpleItem(ModItems.EXCALIBUR);
        addSimpleItem(ModItems.GRAM);
        addSimpleItem(ModItems.TYRFING);
        addSimpleItem(ModItems.HOFSKOR, "Hófskór");
        addSimpleItem(ModItems.GJALLARHORN);
        addSimpleItem(ModItems.HULIOSHJALMR, "Huliðshjálmr");
        addSimpleItem(ModItems.EYE_OF_HORUS, "Eye of Horus");
        addSimpleItem(ModItems.GUNGNIR);
        addSimpleItem(ModItems.WEBEN);
        addSimpleItem(ModItems.BIA_EN_PET, "Bia-en-Pet");
        addSimpleItem(ModItems.KUSANAGI_NO_TSURUGI, "Kusanagi-no-Tsurugi");
        addSimpleItem(ModItems.KAZAGURUMA);
        addSimpleItem(ModItems.KANABO, "Kanabō");
        addSimpleItem(ModItems.NORTHERN_AXE, "Northern Axe");
        addSimpleItem(ModItems.RHONGOMYNIAD);
        addSimpleItem(ModItems.PRIDWEN);
        addSimpleItem(ModItems.CLYDNO_HALTER, "Clydno Halter");
        addSimpleItem(ModItems.RECALL_BELL, "Recall Bell");
        addSimpleItem(ModItems.UCHIDE_NO_KOZUCHI, "Uchide no Kozuchi");
        addSimpleItem(ModItems.YAMAWARI);
        addSimpleItem(ModItems.ANDVARANAUT);
        addSimpleItem(ModItems.DRAUPNIR);
        addSimpleItem(ModItems.EAGLE_KNIGHT_TALONS, "Eagle Knight Talons");
        addSimpleItem(ModItems.MACUAHUITL);
        addSimpleItem(ModItems.MEGINGJORD, "Megingjörð");
        addSimpleItem(ModItems.HEKA);
        addSimpleItem(ModItems.NEKHAKHA);
        addSimpleItem(ModItems.MEDJU_NETJER, "Medju Netjer");
        addSimpleItem(ModItems.HOLY_GRAIL, "The Holy Grail");
        addSimpleItem(ModItems.SMOKING_MIRROR, "The Smoking Mirror");
        addSimpleItem(ModItems.RAIKO);
        addSimpleItem(ModItems.YASAKANI_NO_MAGATAMA, "Yasakani no Magatama");
        addSimpleItem(ModItems.YATA_NO_KAGAMI, "Yata no Kagami");
        addSimpleItem(ModItems.IWATOSHI, "Iwatōshi");
        addSimpleItem(ModItems.APILOLLI);
        addSimpleItem(ModItems.XIUHCOATL);
        addSimpleItem(ModItems.CHIMALLI);
        addSimpleItem(ModItems.TECPATL_OF_THE_FIFTH_SUN, "Técpatl of the Fifth Sun");
        addSimpleItem(ModItems.CLAWS_OF_TEPEYOLLOTL, "Claws of Tepeyollotl");
        addSimpleItem(ModItems.RELIC_FRAGMENT, "Relic Fragment");
        addSimpleItem(ModItems.MYTHICAL_RELIQUARY, "Mythical Reliquary");
        add(ModEntities.ANUBITE.get(), "Anubite");
        add(ModEntities.ECCLESIASTIC.get(), "Ecclesiastic");
        add(ModEntities.DRAUGR.get(), "Draugr");
        add(ModEntities.ONI.get(), "Oni");
        add(ModItems.ANUBITE_SPAWN_EGG.get(), "Anubite Spawn Egg");
        add(ModItems.ECCLESIASTIC_SPAWN_EGG.get(), "Ecclesiastic Spawn Egg");
        add(ModItems.DRAUGR_SPAWN_EGG.get(), "Draugr Spawn Egg");
        add(ModItems.ONI_SPAWN_EGG.get(), "Oni Spawn Egg");
        add("item.noblephantasms.relic_fragment.named", "%s Fragment");
        add("menu.noblephantasms.reliquary_station", "Reliquary Station");
        add("menu.noblephantasms.reliquary_station.forge", "Forge");
        add("menu.noblephantasms.mythical_reliquary", "Mythical Reliquary");
        add("menu.noblephantasms.mythical_reliquary.contents", "Table of Contents");
        add("menu.noblephantasms.mythical_reliquary.fragments", "Fragments: %s / %s");
        add("menu.noblephantasms.mythical_reliquary.select", "Choose Relic");
        add("menu.noblephantasms.mythical_reliquary.selected", "Chosen Relic");
        add("tooltip.noblephantasms.medju_netjer.reroll", "Turn the page (1 Lapis Lazuli)");
        add("tooltip.noblephantasms.medju_netjer.offer", "Medju Netjer reveals:");
        add("tooltip.noblephantasms.eye_of_horus.flavor",
                "Thoth restored Horus's wounded eye from scattered pieces, and the completed wedjat became a sign of protection and wholeness.");
        add("tooltip.noblephantasms.ankh.flavor",
                "This hieroglyph meant life; gods were often shown placing it at a pharaoh's lips to grant life's breath.");
        add("tooltip.noblephantasms.kheper_scarab.flavor",
                "Scarab amulets invoked Khepri, whose rising sun renewed itself each morning.");
        add("tooltip.noblephantasms.scales_of_maat.flavor",
                "Spell 125 of the Book of the Dead weighs the deceased's heart against Ma'at's feather.");
        add("tooltip.noblephantasms.weben.flavor",
                "The Egyptian verb behind its name described the sun in the act of rising and shining.");
        add("tooltip.noblephantasms.bia_en_pet.flavor",
                "Tutankhamun was buried with a dagger of meteoritic iron—a metal fallen from the sky in an age of bronze.");
        add("tooltip.noblephantasms.heka.flavor",
                "As royal regalia, the crook cast the pharaoh as shepherd and guardian of his people.");
        add("tooltip.noblephantasms.nekhakha.flavor",
                "Paired with the crook, the royal flail represented the pharaoh's power to drive, discipline, and command.");
        add("tooltip.noblephantasms.medju_netjer.flavor",
                "Hieroglyphs were 'words of the god,' sacred signs believed to carry the power they described.");
        add("tooltip.noblephantasms.eagle_knight_talons.flavor",
                "Elite Mexica warriors wore eagle-shaped war suits, invoking a predator that kills in its descent.");
        add("tooltip.noblephantasms.macuahuitl.flavor",
                "Mesoamerican warriors lined wooden weapons with inset obsidian blades, whose brittle edges cut even as they splintered.");
        add("tooltip.noblephantasms.smoking_mirror.flavor",
                "The god from whom it takes its name carried an obsidian mirror for divination and embodied change through conflict.");
        add("tooltip.noblephantasms.scabbard.flavor",
                "Malory valued Arthur's sheath above the sword itself, for its wearer could not lose a drop of blood.");
        add("tooltip.noblephantasms.carnwennan.flavor",
                "Welsh tradition calls Arthur's dagger the 'Little White Hilt'; later retellings let it shroud him in shadow.");
        add("tooltip.noblephantasms.bertilak.flavor",
                "The axe's namesake is revealed as the Green Knight, who claims the right to return Sir Gawain's axe blow and the right to his head...");
        add("tooltip.noblephantasms.excalibur.flavor",
                "Malory wrote that Arthur's drawn blade shone in his enemies' eyes with the light of thirty torches.");
        add("tooltip.noblephantasms.rhongomyniad.flavor",
                "Geoffrey of Monmouth calls Arthur's lance hard, broad, and fit for slaughter—the weapon he carried in the charge at Badon.");
        add("tooltip.noblephantasms.holy_grail.flavor",
                "Across medieval romances, the sacred vessel granted sustenance, healing, and divine grace.");
        add("tooltip.noblephantasms.pridwen.flavor",
                "Welsh tradition numbers Arthur's broad shield among the arms that guarded him and those who stood at his side.");
        add("tooltip.noblephantasms.clydno_halter.flavor",
                "One of Britain's Thirteen Treasures, its keeper found within it whatever horse he wished for.");
        add("tooltip.noblephantasms.gungnir.flavor",
                "Odin's dwarf-forged spear was so perfectly made that no throw could miss its mark.");
        add("tooltip.noblephantasms.gram.flavor",
                "Sigurd reforged his father's shattered sword, then slew Fafnir by thrusting upward from a pit beneath the dragon.");
        add("tooltip.noblephantasms.tyrfing.flavor",
                "The dwarfs Dvalinn and Durinn cursed their sword so that once drawn, it could not be sheathed before taking a life.");
        add("tooltip.noblephantasms.gjallarhorn.flavor",
                "Heimdall's horn will be heard through every realm when it calls the gods to Ragnarok.");
        add("tooltip.noblephantasms.hulioshjalmr.flavor",
                "In Norse saga, a 'concealing helmet' could mean not armor, but the very magic that made someone unseen.");
        add("tooltip.noblephantasms.andvaranaut.flavor",
                "Andvari's ring brought its owners a great hoard and a curse; each inheritor gained the treasure and paid in blood.");
        add("tooltip.noblephantasms.draupnir.flavor",
                "Every ninth night, Odin's golden ring dripped eight more rings of equal weight from itself.");
        add("tooltip.noblephantasms.meginjord.flavor",
                "Thor's 'power-belt' doubled his already immense strength whenever he fastened it.");
        add("tooltip.noblephantasms.hofskor.flavor",
                "Old Norse hoof-shoes were protective fittings for a steed; these bear molten signs of impossible swiftness.");
        add("tooltip.noblephantasms.kusanagi_no_tsurugi.flavor",
                "Yamato Takeru escaped a burning field by cutting down the grass and turning the flames with the wind.");
        add("tooltip.noblephantasms.kazaguruma.flavor",
                "Kusarigama wielders used the weighted chain to entangle a weapon or limb before closing with the sickle.");
        add("tooltip.noblephantasms.uchide_no_kozuchi.flavor",
                "In the tale of Issun-boshi, a stolen ogre's mallet grants wishes with a swing and restores the tiny hero to full size.");
        add("tooltip.noblephantasms.yamawari.flavor",
                "The child hero Kintaro was famed for superhuman strength and traditionally shown carrying a broad masakari axe.");
        add("tooltip.noblephantasms.raiko.flavor",
                "Raijin is encircled by drums whose thunder calls lightning down with every beat.");
        add("tooltip.noblephantasms.yasakani_no_magatama.flavor",
                "Susanoo crushed Amaterasu's jewel-string in his teeth and breathed five deities from its beads.");
        add("tooltip.noblephantasms.yata_no_kagami.flavor",
                "The sacred mirror drew Amaterasu from her cave by returning the sun goddess's own brilliance to her.");
        add("tooltip.noblephantasms.iwatoshi.flavor",
                "Benkei held the Stone-Cutter at the bridge where he died upright, and his enemies feared to approach him.");
        add("tooltip.noblephantasms.apilolli.flavor",
                "The Tlaloque carried rain in jars; thunder was the sound of their vessels breaking open above the earth.");
        add("tooltip.noblephantasms.xiuhcoatl.flavor",
                "Huitzilopochtli wielded the turquoise fire serpent at his birth upon Coatepec.");
        add("tooltip.noblephantasms.chimalli.flavor",
                "Feathered chimalli bore brilliant mosaics of rank, carrying a warrior's colors forward with every advance.");
        add("tooltip.noblephantasms.tecpatl_of_the_fifth_sun.flavor",
                "A celestial flint knife fell from heaven and shattered, birthing the countless Mimixcoa from its pieces.");
        add("tooltip.noblephantasms.claws_of_tepeyollotl.flavor",
                "Tepeyollotl, the jaguar Heart of the Mountain, ruled caves, echoes, and the sheer stone where his claws found purchase.");
        add("tooltip.noblephantasms.kanabo.flavor",
                "An iron-studded kanabō was a brutal emblem of strength; 'giving an oni a club' meant making the already formidable stronger still.");
        add("jei.noblephantasms.info.ankh",
                "Equip in a Curios charm or compatible totem slot. Fatal damage restores full health, "
                        + "releases a damaging and blinding radiant burst, and grants 15 seconds of invulnerability. "
                        + "Reborn prevents another activation for 2 minutes.");
        add("jei.noblephantasms.info.eye_of_horus",
                "Equip in a Curios necklace slot. Hold your crosshair on a living target for 2 seconds to apply "
                        + "Judgement for 15 seconds, marking it with a golden glow and making it take 25% more "
                        + "damage from all sources. Judged enemies you kill have a chance to drop glowing Eye "
                        + "Shards. Up to six pieces grant 5% damage and reduce gaze time by 0.25 seconds each. "
                        + "At six pieces, the next fully charged Judgement marks nearby enemies in a radiant burst.");
        add("jei.noblephantasms.info.kheper_scarab",
                "Right-click while held to toggle the scarab. While active in a Curios charm slot, it repairs "
                        + "each damaged item in your hands and armor slots by 1 durability every 2 seconds, "
                        + "consuming 1 experience point per durability restored.");
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
        add("jei.noblephantasms.info.carnwennan",
                "Look directly at a mob within 16 blocks and right-click to shadow-step behind it in a burst of "
                        + "black smoke. Carnwennan deals 50% more damage when striking a target from behind. "
                        + "5 second cooldown.");
        add("jei.noblephantasms.info.medju_netjer",
                "Use Medju Netjer on an enchanting table to install it. "
                        + "Sneak-right-click the table with an empty hand to remove it. "
                        + "While installed, it reveals enchantment offers and lets you reroll them for 1 Lapis Lazuli.");
        add("jei.noblephantasms.info.weben",
                "Charges for 10 seconds while carried in direct sunlight. A fully charged strike releases a fiery "
                        + "flare for bonus damage and ignites the target.");
        add("jei.noblephantasms.info.holy_grail",
                "Drink to gain Undying for 5 seconds. Damage cannot reduce you below half a heart while it lasts. "
                        + "3 minute cooldown.");
        add("jei.noblephantasms.info.smoking_mirror",
                "Use on a living target to make every mob within 10 blocks hunt it for 8 seconds.");
        add("jei.noblephantasms.info.eagle_knight_talons",
                "Crouch while airborne to dive rapidly. Landing creates a 4-block shockwave that deals more damage "
                        + "the farther you fell, while negating the slam's fall damage.");
        add("jei.noblephantasms.info.kazaguruma",
                "Right-click to throw the chained sickle up to 12 blocks. It damages and hooks the first living "
                        + "target struck, then pulls the target into melee range. "
                        + "3 second cooldown.");
        add("jei.noblephantasms.info.macuahuitl",
                "Hits apply Bleeding for 5 seconds. Repeated hits add up to 5 stacks, with each stack dealing "
                        + "1 armor-bypassing damage every second.");
        add("jei.noblephantasms.info.gram",
                "Every third fully charged damaging hit against a hostile or neutral target triggers Fafnir's "
                        + "Bite, adding damage that scales with how much the target's maximum health exceeds yours.");
        add("jei.noblephantasms.info.bia_en_pet",
                "Melee strikes ignore the target's armor while still respecting shields, enchantments, effects, absorption, and immunity frames.");
        add("jei.noblephantasms.info.clydno_halter",
                "Use on a horse to fit the halter and receive its bound Recall Bell. Ring the bell anywhere to summon the horse. "
                        + "Sneak-use the bell on that horse to recover the halter.");
        add("jei.noblephantasms.info.hofskor",
                "Equip in a Curios charm slot. Your horse gains 40% speed, two-block step height, and immunity to fall damage while ridden.");
        add("jei.noblephantasms.info.hulioshjalmr",
                "Wear the helmet and crouch for 1 second to fade from sight. Fully concealed players move 25% "
                        + "slower, cannot be detected or targeted by mobs, and remain concealed while moving. "
                        + "Attacking, taking damage, or interacting breaks concealment for 5 seconds.");
        add("jei.noblephantasms.info.gjallarhorn",
                "Sound Gjallarhorn to inflict Fear on non-allied creatures and players within 24 blocks for "
                        + "4 seconds. Feared creatures abandon combat and flee from the horn's wielder, while "
                        + "feared players lose control of their movement. The wielder and all allies have Fear "
                        + "removed and gain Strength for 8 seconds. 30 second cooldown.");
        add("jei.noblephantasms.info.pridwen",
                "Raise the shield to project an enlarged energy copy of Pridwen in front of you. Hostile projectiles and ranged attacks stop against it while allied attacks pass through.");
        add("jei.noblephantasms.info.raiko",
                "Use to beat the drum and call lightning at the point you are looking at within 40 blocks. 5 second cooldown.");
        add("jei.noblephantasms.info.tyrfing",
                "Carrying Tyrfing without wielding it reduces attack damage, attack speed, and movement speed. "
                        + "Wielding it suppresses the curse. Direct kills with Tyrfing grant Bloodlust for 12 seconds, stacking up to five times. "
                        + "Each stack grants +0.75 attack damage, +6% attack speed, and +4% movement speed.");
        add("jei.noblephantasms.info.yasakani_no_magatama",
                "Equip as a Curios necklace. Damage can birth one of five 30-second guardians. Each fights with your attack damage, grants +1 armor toughness and 4% damage reduction, and has a distinct role.");
        add("jei.noblephantasms.info.yata_no_kagami",
                "Raise the mirror as a shield. Projectiles it blocks reverse course at twice their incoming speed and belong to you after reflection.");
        add("jei.noblephantasms.info.iwatoshi",
                "Hold use to charge an expanding sweep, gaining a tier every second. Release early for a growing frontal cleave. At four seconds, Iwatōshi automatically unleashes a 360-degree cleave that knocks surrounding enemies outward.");
        add("jei.noblephantasms.info.apilolli",
                "Release a rain cloud that follows for 90 seconds, accelerates crops, extinguishes fire, solidifies lava, grants fishing Luck, speeds up bites, grants conduit power, and enables Riptide. Refill the empty jar at source water.");
        add("jei.noblephantasms.info.xiuhcoatl",
                "Fire a serpent that seeks hostile creatures, deals fire damage, and ignites its target. 8 second cooldown.");
        add("jei.noblephantasms.info.chimalli",
                "Raise the shield to build momentum. Lower it to surge in the direction you are looking and gain "
                        + "Speed I to III for up to 8 seconds, reaching maximum charge after 3 seconds of blocking.");
        add("jei.noblephantasms.info.tecpatl_of_the_fifth_sun",
                "Hold right-click to fire the knife's ten shards in sequence, or Shift-right-click to release every attached shard in a shotgun blast. Once firing ends, the shards return in reverse order and visibly rebuild the knife in your hand.");
        add("jei.noblephantasms.info.claws_of_tepeyollotl",
                "Equip in a Curios charm slot. Grants +1.5 attack damage. Crouch against a wall to cling and crawl, "
                        + "with a 45-degree neutral climbing window. Mining gains +5 speed, equivalent to Efficiency II, "
                        + "and stacks with tool enchantments. The claws can harvest anything a stone pickaxe can.");

        add("creativetab.noblephantasms.title", "Noble Phantasms");
        add("creativetab.noblephantasms.trophy_heads", "Trophy Heads");
        add("item.noblephantasms.trophy_head.named", "%s Head");
        add("item.noblephantasms.trophy_head.baby_named", "Baby %s Head");
        add("item.noblephantasms.trophy_head.variant_named", "%2$s %1$s Head");
        add("item.noblephantasms.trophy_head.baby_variant_named", "Baby %2$s %1$s Head");
        add("message.noblephantasms.bertilak.already_bound", "The Covenant is already bound");
        add("message.noblephantasms.bertilak.broken", "The Covenant has been broken");
        add("message.noblephantasms.bertilak.cooldown", "The Covenant can be bound again in %s seconds");
        add("message.noblephantasms.bertilak.fulfilled", "Covenant fulfilled: %s");
        add("message.noblephantasms.bertilak.target_bound", "That target is already bound by a Covenant");
        add("message.noblephantasms.clydno_halter.bound", "Bound to %s");
        add("message.noblephantasms.clydno_halter.already_bound", "%s already wears a Clydno Halter");
        add("message.noblephantasms.clydno_halter.recovered", "Recovered the halter from %s");
        add("message.noblephantasms.clydno_halter.unanswered", "The bound mount does not answer");
        add("tooltip.noblephantasms.recall_bell.mount", "Bound Mount: %s");
        add("effect.noblephantasms.covenant", "Covenant");
        add("effect.noblephantasms.judgement", "Judgement");
        add("effect.noblephantasms.luminous", "Luminous");
        add("effect.noblephantasms.reborn", "Reborn");
        add("effect.noblephantasms.bleeding", "Bleeding");
        add("effect.noblephantasms.feared", "Feared");
        add("effect.noblephantasms.ward", "Ward");
        add("effect.noblephantasms.undying", "Undying");
        add("effect.noblephantasms.chilled", "Chilled");
        add("effect.noblephantasms.frozen", "Frozen");
        add("death.attack.bleed", "%1$s bled out");
        add("death.attack.bleed.player", "%1$s bled out due to %2$s");
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
