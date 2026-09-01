package net.turtleboi.noblephantasms.relic;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.turtleboi.noblephantasms.item.ModItems;
import net.turtleboi.noblephantasms.item.custom.RelicFragmentItem;

public final class RelicFragmentDefinitions {
    private static final List<Definition> DEFINITIONS = List.of(
            standard(ModItems.EYE_OF_HORUS, 6, 6).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.ANKH, 5).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.KHEPER_SCARAB, 5).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.SCALES_OF_MAAT, 6).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            weapon(ModItems.WEBEN, 8).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.BIA_EN_PET, 5).withStationTexture(TextureVariant.DISPLAY)
                    .withTextureFrameHeight(16)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.HEKA, 4).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.NEKHAKHA, 4).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.MEDJU_NETJER, 6).withCivilization(RelicFragmentItem.FragmentOrigin.EGYPTIAN),
            standard(ModItems.HOLY_GRAIL, 7).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            standard(ModItems.SMOKING_MIRROR, 7).withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            standard(ModItems.YASAKANI_NO_MAGATAMA, 7).withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            standard(ModItems.YATA_NO_KAGAMI, 6).withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            weapon(ModItems.IWATOSHI, 8).withStationTexture(TextureVariant.DISPLAY)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            standard(ModItems.APILOLLI, 6).withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            weapon(ModItems.XIUHCOATL, 8).withStationTexture(TextureVariant.DISPLAY)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            standard(ModItems.CHIMALLI, 6).withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            standard(ModItems.TECPATL_OF_THE_FIFTH_SUN, 8, 12)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            standard(ModItems.CLAWS_OF_TEPEYOLLOTL, 4)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            standard(ModItems.SCABBARD, 6).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            standard(ModItems.CARNWENNAN, 6).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            weapon(ModItems.BERTILAK, 8).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            weapon(ModItems.EXCALIBUR, 9).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            weapon(ModItems.RHONGOMYNIAD, 9).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            standard(ModItems.PRIDWEN, 6).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            standard(ModItems.CLYDNO_HALTER, 4).withCivilization(RelicFragmentItem.FragmentOrigin.ARTHURIAN),
            weapon(ModItems.GUNGNIR, 10).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            weapon(ModItems.GRAM, 9).withStationTexture(TextureVariant.DISPLAY)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.TYRFING, 7).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.GJALLARHORN, 6).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.HULIOSHJALMR, 6).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.UCHIDE_NO_KOZUCHI, 6)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            weapon(ModItems.YAMAWARI, 8).withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            standard(ModItems.ANDVARANAUT, 4).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.DRAUPNIR, 4).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.MEGINGJORD, 5).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            standard(ModItems.HOFSKOR, 4).withCivilization(RelicFragmentItem.FragmentOrigin.NORSE),
            weapon(ModItems.KUSANAGI_NO_TSURUGI, 8)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            held(ModItems.KAZAGURUMA, 8).withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            weapon(ModItems.KANABO, 8).withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE),
            standard(ModItems.EAGLE_KNIGHT_TALONS, 6)
                    .withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            weapon(ModItems.MACUAHUITL, 7).withCivilization(RelicFragmentItem.FragmentOrigin.AZTEC),
            standard(ModItems.RAIKO, 6).withTextureAlias("raiko_item")
                    .withCivilization(RelicFragmentItem.FragmentOrigin.JAPANESE));

    public static Definition get(Identifier relicId) {
        return DEFINITIONS.stream()
                .filter(definition -> definition.relicId().equals(relicId))
                .findFirst()
                .orElse(null);
    }

    public static boolean supports(Identifier relicId) {
        return get(relicId) != null;
    }

    public static List<Identifier> relicIds() {
        return DEFINITIONS.stream().map(Definition::relicId).toList();
    }

    public static List<Definition> definitions() {
        return DEFINITIONS;
    }

    private static Definition standard(Supplier<? extends Item> relic, int maximumPieces) {
        return standard(relic, 4, maximumPieces);
    }

    private static Definition standard(Supplier<? extends Item> relic, int minimumPieces, int maximumPieces) {
        return new Definition(relic, TextureVariant.STANDARD, TextureVariant.STANDARD,
                null, 0, minimumPieces, maximumPieces, RelicFragmentItem.FragmentOrigin.GENERIC);
    }

    private static Definition weapon(Supplier<? extends Item> relic, int maximumPieces) {
        return new Definition(relic, TextureVariant.WEAPON, TextureVariant.WEAPON,
                null, 0, 4, maximumPieces, RelicFragmentItem.FragmentOrigin.GENERIC);
    }

    private static Definition held(Supplier<? extends Item> relic, int maximumPieces) {
        return new Definition(relic, TextureVariant.HELD, TextureVariant.HELD,
                null, 0, 4, maximumPieces, RelicFragmentItem.FragmentOrigin.GENERIC);
    }

    public record Definition(Supplier<? extends Item> relic, TextureVariant textureVariant,
                             TextureVariant stationTextureVariant, String textureAlias,
                             int textureFrameHeight,
                             int minimumPieces, int maximumPieces,
                             RelicFragmentItem.FragmentOrigin civilization) {
        public Definition withTextureAlias(String textureAlias) {
            return new Definition(relic, textureVariant, stationTextureVariant,
                    textureAlias, textureFrameHeight, minimumPieces, maximumPieces, civilization);
        }

        public Definition withStationTexture(TextureVariant stationTextureVariant) {
            return new Definition(relic, textureVariant, stationTextureVariant,
                    textureAlias, textureFrameHeight, minimumPieces, maximumPieces, civilization);
        }

        public Definition withTextureFrameHeight(int textureFrameHeight) {
            return new Definition(relic, textureVariant, stationTextureVariant,
                    textureAlias, textureFrameHeight, minimumPieces, maximumPieces, civilization);
        }

        public Definition withCivilization(RelicFragmentItem.FragmentOrigin civilization) {
            return new Definition(relic, textureVariant, stationTextureVariant,
                    textureAlias, textureFrameHeight, minimumPieces, maximumPieces, civilization);
        }

        public Identifier relicId() {
            return BuiltInRegistries.ITEM.getKey(relic.get());
        }

        public Identifier textureId() {
            return textureId(textureVariant);
        }

        public Identifier stationTextureId() {
            return textureId(stationTextureVariant);
        }

        public Identifier previewTextureId() {
            return stationTextureId();
        }

        public Identifier inventoryTextureId() {
            Identifier relicId = relicId();
            String baseName = textureAlias == null ? relicId.getPath() : textureAlias;
            return Identifier.fromNamespaceAndPath(relicId.getNamespace(),
                    "item/" + baseName + textureVariant.inventorySuffix);
        }

        private Identifier textureId(TextureVariant variant) {
            Identifier relicId = relicId();
            String baseName = textureAlias == null ? relicId.getPath() : textureAlias;
            return Identifier.fromNamespaceAndPath(relicId.getNamespace(),
                    "item/" + baseName + variant.suffix);
        }
    }

    public enum TextureVariant {
        STANDARD("", ""),
        WEAPON("_weapon", "_item"),
        HELD("_held", ""),
        DISPLAY("_display", "_item");

        private final String suffix;
        private final String inventorySuffix;

        TextureVariant(String suffix, String inventorySuffix) {
            this.suffix = suffix;
            this.inventorySuffix = inventorySuffix;
        }
    }
}
