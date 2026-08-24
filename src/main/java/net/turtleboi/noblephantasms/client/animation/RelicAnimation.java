package net.turtleboi.noblephantasms.client.animation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RelicAnimation {
    private final Map<Channel, RelicAnimationClip> channels = new EnumMap<>(Channel.class);
    private final Map<Channel, BlendMode> blendModes = new EnumMap<>(Channel.class);

    public RelicAnimation channel(Channel channel, RelicAnimationClip animation) {
        return channel(channel, animation, BlendMode.ADDITIVE);
    }

    public RelicAnimation channel(Channel channel, RelicAnimationClip animation, BlendMode blendMode) {
        channels.put(channel, animation);
        blendModes.put(channel, blendMode);
        return this;
    }

    public RelicAnimationClip channel(Channel channel) {
        return channels.get(channel);
    }

    public BlendMode blendMode(Channel channel) {
        return blendModes.getOrDefault(channel, BlendMode.ADDITIVE);
    }

    public List<Channel> channels() {
        return List.copyOf(channels.keySet());
    }

    public RelicAnimation copy() {
        RelicAnimation copy = new RelicAnimation();
        channels.forEach((channel, animation) ->
                copy.channel(channel, animation.copy(), blendMode(channel)));
        return copy;
    }

    public enum Channel {
        ITEM,
        MAIN_ARM,
        OFF_ARM,
        BODY
    }

    public enum BlendMode {
        ADDITIVE,
        REPLACE
    }
}
