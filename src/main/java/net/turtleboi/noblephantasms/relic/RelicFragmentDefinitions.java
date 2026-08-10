package net.turtleboi.noblephantasms.relic;

import java.util.List;
import java.util.function.Supplier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.turtleboi.noblephantasms.item.ModItems;

public final class RelicFragmentDefinitions {
    private static final List<Definition> DEFINITIONS = List.of(
            standard(ModItems.EYE_OF_HORUS, 6, 6),
            standard(ModItems.ANKH, 5),
            standard(ModItems.KHEPER_SCARAB, 5),
            standard(ModItems.SCALES_OF_MAAT, 6),
            weapon(ModItems.WEBEN, 8),
            standard(ModItems.HEKA, 4),
            standard(ModItems.NEKHAKHA, 4),
            standard(ModItems.MEDJU_NETJER, 6),
            standard(ModItems.HOLY_GRAIL, 7),
            standard(ModItems.SMOKING_MIRROR, 7),
            standard(ModItems.SCABBARD, 6),
            standard(ModItems.CARNWENNAN, 6),
            weapon(ModItems.BERTILAK, 8),
            weapon(ModItems.EXCALIBUR, 9),
            weapon(ModItems.RHONGOMYNIAD, 9),
            weapon(ModItems.GUNGNIR, 10),
            weapon(ModItems.GRAM, 9).withStationTexture(TextureVariant.DISPLAY),
            standard(ModItems.GJALLARHORN, 6),
            standard(ModItems.HULIOSHJALMR, 6),
            standard(ModItems.UCHIDE_NO_KOZUCHI, 6),
            weapon(ModItems.YAMAWARI, 8),
            standard(ModItems.ANDVARANAUT, 4),
            standard(ModItems.DRAUPNIR, 4),
            standard(ModItems.MEGINGJORD, 5),
            weapon(ModItems.KUSANAGI_NO_TSURUGI, 8),
            held(ModItems.KAZAGURUMA, 8),
            standard(ModItems.EAGLE_KNIGHT_TALONS, 6),
            standard(ModItems.MACUAHUITL, 7));

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

    private static Definition standard(Supplier<? extends Item> relic, int maximumPieces) {
        return standard(relic, 4, maximumPieces);
    }

    private static Definition standard(Supplier<? extends Item> relic, int minimumPieces, int maximumPieces) {
        return new Definition(relic, TextureVariant.STANDARD, TextureVariant.STANDARD,
                null, minimumPieces, maximumPieces);
    }

    private static Definition weapon(Supplier<? extends Item> relic, int maximumPieces) {
        return new Definition(relic, TextureVariant.WEAPON, TextureVariant.WEAPON,
                null, 4, maximumPieces);
    }

    private static Definition held(Supplier<? extends Item> relic, int maximumPieces) {
        return new Definition(relic, TextureVariant.HELD, TextureVariant.HELD,
                null, 4, maximumPieces);
    }

    public record Definition(Supplier<? extends Item> relic, TextureVariant textureVariant,
                             TextureVariant stationTextureVariant, String textureAlias,
                             int minimumPieces, int maximumPieces) {
        public Definition withTextureAlias(String textureAlias) {
            return new Definition(relic, textureVariant, stationTextureVariant,
                    textureAlias, minimumPieces, maximumPieces);
        }

        public Definition withStationTexture(TextureVariant stationTextureVariant) {
            return new Definition(relic, textureVariant, stationTextureVariant,
                    textureAlias, minimumPieces, maximumPieces);
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

        private Identifier textureId(TextureVariant variant) {
            Identifier relicId = relicId();
            String baseName = textureAlias == null ? relicId.getPath() : textureAlias;
            return Identifier.fromNamespaceAndPath(relicId.getNamespace(),
                    "item/" + baseName + variant.suffix);
        }
    }

    public enum TextureVariant {
        STANDARD(""),
        WEAPON("_weapon"),
        HELD("_held"),
        DISPLAY("_display");

        private final String suffix;

        TextureVariant(String suffix) {
            this.suffix = suffix;
        }
    }
}
