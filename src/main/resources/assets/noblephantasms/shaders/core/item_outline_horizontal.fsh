#version 330

uniform sampler2D InSampler;

layout(std140) uniform OutlineConfig {
    vec4 LayerColors[4];
    vec4 LayerRadii;
    vec4 LayerAlphas;
    vec4 OutlineOptions;
};

in vec2 texCoord;

out vec4 fragColor;

const int MaxRadius = 48;

void main() {
    vec2 oneTexel = 1.0 / vec2(textureSize(InSampler, 0));
    vec4 layerAlpha = vec4(0.0);
    float maximumRadius = max(max(LayerRadii.x, LayerRadii.y), max(LayerRadii.z, LayerRadii.w));

    for (int offset = -MaxRadius; offset <= MaxRadius; offset++) {
        float distance = abs(float(offset));
        if (distance > maximumRadius) {
            continue;
        }
        float sampleAlpha = texture(InSampler, texCoord + vec2(float(offset) * oneTexel.x, 0.0)).a;
        if (distance <= LayerRadii.x) {
            layerAlpha.x = max(layerAlpha.x, sampleAlpha);
        }
        if (distance <= LayerRadii.y) {
            layerAlpha.y = max(layerAlpha.y, sampleAlpha);
        }
        if (distance <= LayerRadii.z) {
            layerAlpha.z = max(layerAlpha.z, sampleAlpha);
        }
        if (distance <= LayerRadii.w) {
            layerAlpha.w = max(layerAlpha.w, sampleAlpha);
        }
    }

    fragColor = layerAlpha;
}
