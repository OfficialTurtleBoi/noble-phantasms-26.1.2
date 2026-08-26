#version 330

uniform sampler2D DilatedSampler;
uniform sampler2D DepthDilatedSampler;
uniform sampler2D MaskSampler;
uniform sampler2D SceneDepthSampler;

layout(std140) uniform OutlineConfig {
    vec4 LayerColors[4];
    vec4 LayerRadii;
    vec4 LayerAlphas;
    vec4 OutlineOptions;
};

in vec2 texCoord;

out vec4 fragColor;

const int MaxRadius = 48;

float unpackDepth(vec3 encodedDepth) {
    return dot(encodedDepth, vec3(1.0, 1.0 / 255.0, 1.0 / 65025.0));
}

void main() {
    float centerAlpha = texture(MaskSampler, texCoord).a;
    if (centerAlpha > 0.0) {
        discard;
    }

    vec2 oneTexel = 1.0 / vec2(textureSize(DilatedSampler, 0));
    vec4 layerAlpha = vec4(0.0);
    bool visibleThroughObjects = OutlineOptions.x > 0.5;
    float sceneDepth = visibleThroughObjects ? 1.0 : texture(SceneDepthSampler, texCoord).r;
    int maximumRadius = min(MaxRadius, int(ceil(max(max(LayerRadii.x, LayerRadii.y), max(LayerRadii.z, LayerRadii.w)))));

    for (int offset = -maximumRadius; offset <= maximumRadius; offset++) {
        float distance = abs(float(offset));
        vec4 sampleAlpha = texture(DilatedSampler, texCoord + vec2(0.0, float(offset) * oneTexel.y));
        bool visible = visibleThroughObjects;
        if (!visibleThroughObjects) {
            vec4 packedDepth = texture(DepthDilatedSampler, texCoord + vec2(0.0, float(offset) * oneTexel.y));
            visible = unpackDepth(packedDepth.rgb) <= sceneDepth + 0.00001;
        }
        if (distance <= LayerRadii.x && visible) {
            layerAlpha.x = max(layerAlpha.x, sampleAlpha.x);
        }
        if (distance <= LayerRadii.y && visible) {
            layerAlpha.y = max(layerAlpha.y, sampleAlpha.y);
        }
        if (distance <= LayerRadii.z && visible) {
            layerAlpha.z = max(layerAlpha.z, sampleAlpha.z);
        }
        if (distance <= LayerRadii.w && visible) {
            layerAlpha.w = max(layerAlpha.w, sampleAlpha.w);
        }
    }

    layerAlpha *= LayerAlphas;
    vec3 premultiplied = vec3(0.0);
    float combinedAlpha = 0.0;
    for (int index = 0; index < 4; index++) {
        float alpha = layerAlpha[index] * LayerColors[index].a;
        premultiplied = LayerColors[index].rgb * alpha + premultiplied * (1.0 - alpha);
        combinedAlpha = alpha + combinedAlpha * (1.0 - alpha);
    }
    if (combinedAlpha <= 0.001) {
        discard;
    }
    fragColor = vec4(premultiplied / combinedAlpha, combinedAlpha);
}
