package net.turtleboi.noblephantasms.client.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.turtleboi.noblephantasms.NoblePhantasms;

final class RelicAnimationStorage {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Map<AnimationKey, RelicAnimation> ANIMATIONS = new LinkedHashMap<>();
    private static final Map<ModelTransformKey, RelicTransform> MODEL_TRANSFORMS = new LinkedHashMap<>();
    private static boolean loaded;

    static void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        Path path = path();
        if (!Files.isRegularFile(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            for (JsonElement element : root.getAsJsonArray("animations")) {
                JsonObject json = element.getAsJsonObject();
                AnimationKey key = new AnimationKey(
                        Identifier.parse(json.get("item").getAsString()),
                        json.get("animation").getAsString());
                RelicAnimation animation = readAnimation(json);
                ANIMATIONS.put(key, animation);
                RelicAnimator.applyStoredAnimation(key.itemId(), key.animationId(), animation);
            }
            for (JsonElement element : root.getAsJsonArray("model_transforms")) {
                JsonObject json = element.getAsJsonObject();
                ModelTransformKey key = new ModelTransformKey(
                        Identifier.parse(json.get("item").getAsString()),
                        ItemDisplayContext.valueOf(json.get("display_context").getAsString()));
                MODEL_TRANSFORMS.put(key, readTransform(json.getAsJsonObject("transform")));
            }
        } catch (Exception exception) {
            NoblePhantasms.LOGGER.error("Unable to load saved relic animations from {}", path, exception);
        }
    }

    static SaveResult save(ItemPoseEditor.Session session) throws IOException {
        Set<AnimationKey> savedAnimations = new HashSet<>();
        Set<ModelTransformKey> savedModelTransforms = new HashSet<>();
        for (ItemPoseEditor.Target target : session.targets()) {
            for (String pose : target.poses()) {
                String animationId = RelicAnimator.getEditorAnimationId(
                        target.key().itemId(), target.key().cameraType(), pose);
                if (animationId != null) {
                    AnimationKey key = new AnimationKey(target.key().itemId(), animationId);
                    ANIMATIONS.put(key, target.animations().get(pose).copy());
                    savedAnimations.add(key);
                    continue;
                }
                if (!pose.equals("held")) {
                    continue;
                }
                RelicAnimationClip clip = target.animations().get(pose)
                        .channel(RelicAnimation.Channel.ITEM);
                if (clip == null || clip.keyframes().isEmpty()
                        || !clip.keyframes().getFirst().transform().usesModelDisplay()) {
                    continue;
                }
                ModelTransformKey key = new ModelTransformKey(
                        target.key().itemId(), target.key().displayContext());
                MODEL_TRANSFORMS.put(key, clip.keyframes().getFirst().transform().copy());
                savedModelTransforms.add(key);
            }
        }

        Path path = path();
        Files.createDirectories(path.getParent());
        Files.writeString(path, GSON.toJson(writeRoot()), StandardCharsets.UTF_8);
        return new SaveResult(path.toAbsolutePath(), savedAnimations.size(), savedModelTransforms.size());
    }

    static RelicTransform getModelTransform(Identifier itemId, ItemDisplayContext displayContext) {
        RelicTransform transform = MODEL_TRANSFORMS.get(new ModelTransformKey(itemId, displayContext));
        return transform == null ? null : transform.copy();
    }

    static RelicAnimation getStoredAnimation(Identifier itemId, String animationId) {
        return ANIMATIONS.get(new AnimationKey(itemId, animationId));
    }

    private static JsonObject writeRoot() {
        JsonObject root = new JsonObject();
        JsonArray animations = new JsonArray();
        ANIMATIONS.forEach((key, animation) -> {
            JsonObject json = writeAnimation(animation);
            json.addProperty("item", key.itemId().toString());
            json.addProperty("animation", key.animationId());
            animations.add(json);
        });
        root.add("animations", animations);

        JsonArray modelTransforms = new JsonArray();
        MODEL_TRANSFORMS.forEach((key, transform) -> {
            JsonObject json = new JsonObject();
            json.addProperty("item", key.itemId().toString());
            json.addProperty("display_context", key.displayContext().name());
            json.add("transform", writeTransform(transform));
            modelTransforms.add(json);
        });
        root.add("model_transforms", modelTransforms);
        return root;
    }

    private static JsonObject writeAnimation(RelicAnimation animation) {
        JsonObject json = new JsonObject();
        JsonArray channels = new JsonArray();
        for (RelicAnimation.Channel channel : animation.channels()) {
            RelicAnimationClip clip = animation.channel(channel);
            JsonObject channelJson = new JsonObject();
            channelJson.addProperty("channel", channel.name());
            channelJson.addProperty("blend", animation.blendMode(channel).name());
            channelJson.addProperty("duration_ticks", clip.durationTicks());
            JsonArray keyframes = new JsonArray();
            for (RelicAnimationClip.Keyframe keyframe : clip.keyframes()) {
                JsonObject keyframeJson = new JsonObject();
                keyframeJson.addProperty("tick", keyframe.tick());
                keyframeJson.addProperty("easing", keyframe.easing().name());
                keyframeJson.add("transform", writeTransform(keyframe.transform()));
                keyframes.add(keyframeJson);
            }
            channelJson.add("keyframes", keyframes);
            channels.add(channelJson);
        }
        json.add("channels", channels);
        return json;
    }

    private static RelicAnimation readAnimation(JsonObject json) {
        RelicAnimation animation = new RelicAnimation();
        for (JsonElement element : json.getAsJsonArray("channels")) {
            JsonObject channelJson = element.getAsJsonObject();
            RelicAnimation.Channel channel = RelicAnimation.Channel.valueOf(
                    channelJson.get("channel").getAsString());
            RelicAnimation.BlendMode blendMode = RelicAnimation.BlendMode.valueOf(
                    channelJson.get("blend").getAsString());
            RelicAnimationClip clip = new RelicAnimationClip(
                    channelJson.get("duration_ticks").getAsFloat());
            for (JsonElement keyframeElement : channelJson.getAsJsonArray("keyframes")) {
                JsonObject keyframeJson = keyframeElement.getAsJsonObject();
                clip.keyframe(keyframeJson.get("tick").getAsFloat(),
                        readTransform(keyframeJson.getAsJsonObject("transform")),
                        RelicAnimationClip.Easing.valueOf(
                                keyframeJson.get("easing").getAsString()));
            }
            animation.channel(channel, clip, blendMode);
        }
        return animation;
    }

    private static JsonObject writeTransform(RelicTransform transform) {
        JsonObject json = new JsonObject();
        json.addProperty("space", transform.spaceName());
        json.add("translation", vector(transform.translationX, transform.translationY, transform.translationZ));
        json.add("rotation", vector(transform.rotationX, transform.rotationY, transform.rotationZ));
        json.add("scale", vector(transform.scaleX, transform.scaleY, transform.scaleZ));
        json.add("anchor", vector(transform.anchorX, transform.anchorY, transform.anchorZ));
        return json;
    }

    private static RelicTransform readTransform(JsonObject json) {
        float[] translation = vector(json.getAsJsonArray("translation"));
        float[] rotation = vector(json.getAsJsonArray("rotation"));
        float[] scale = vector(json.getAsJsonArray("scale"));
        float[] anchor = vector(json.getAsJsonArray("anchor"));
        RelicTransform transform = json.get("space").getAsString().equals("model_display")
                ? RelicTransform.modelDisplay()
                : RelicTransform.poseStack(translation[0], translation[1], translation[2],
                        rotation[0], rotation[1], rotation[2], scale[0], scale[1], scale[2]);
        if (transform.usesModelDisplay()) {
            transform.initialize(translation[0], translation[1], translation[2],
                    rotation[0], rotation[1], rotation[2], scale[0], scale[1], scale[2]);
        }
        transform.anchor(anchor[0], anchor[1], anchor[2]);
        return transform;
    }

    private static JsonArray vector(float x, float y, float z) {
        JsonArray vector = new JsonArray();
        vector.add(x);
        vector.add(y);
        vector.add(z);
        return vector;
    }

    private static float[] vector(JsonArray json) {
        return new float[]{json.get(0).getAsFloat(), json.get(1).getAsFloat(), json.get(2).getAsFloat()};
    }

    private static Path path() {
        return Minecraft.getInstance().gameDirectory.toPath()
                .resolve("config").resolve("noblephantasms-item-animations.json");
    }

    record SaveResult(Path path, int animationCount, int modelTransformCount) {
    }

    private record AnimationKey(Identifier itemId, String animationId) {
    }

    private record ModelTransformKey(Identifier itemId, ItemDisplayContext displayContext) {
    }
}
