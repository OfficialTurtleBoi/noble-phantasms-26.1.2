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

vec3 packDepth(float depth) {
    vec3 encodedDepth = fract(depth * vec3(1.0, 255.0, 65025.0));
    encodedDepth -= encodedDepth.yzz * vec3(1.0 / 255.0, 1.0 / 255.0, 0.0);
    return encodedDepth;
}

float unpackDepth(vec3 encodedDepth) {
    return dot(encodedDepth, vec3(1.0, 1.0 / 255.0, 1.0 / 65025.0));
}

void main() {
    vec2 oneTexel = 1.0 / vec2(textureSize(InSampler, 0));
    float bestAlpha = 0.0;
    float bestDepth = 1.0;
    float bestDistance = float(MaxRadius + 1);
    int maximumRadius = min(MaxRadius, int(ceil(max(max(LayerRadii.x, LayerRadii.y), max(LayerRadii.z, LayerRadii.w)))));

    for (int offset = -maximumRadius; offset <= maximumRadius; offset++) {
        float distance = abs(float(offset));
        vec4 sampleValue = texture(InSampler, texCoord + vec2(float(offset) * oneTexel.x, 0.0));
        if (sampleValue.a > bestAlpha || sampleValue.a == bestAlpha && sampleValue.a > 0.0 && distance < bestDistance) {
            bestAlpha = sampleValue.a;
            bestDepth = unpackDepth(sampleValue.rgb);
            bestDistance = distance;
        }
    }

    fragColor = vec4(packDepth(bestDepth), bestAlpha);
}
