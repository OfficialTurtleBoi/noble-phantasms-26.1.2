#version 330

uniform sampler2D ExpandedSampler;
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

float unpackDepth(vec3 encodedDepth) {
    return dot(encodedDepth, vec3(1.0, 1.0 / 255.0, 1.0 / 65025.0));
}

void main() {
    if (texture(MaskSampler, texCoord).a > 0.0) {
        discard;
    }

    vec4 expanded = texture(ExpandedSampler, texCoord);
    float alpha = expanded.a * LayerAlphas.x * LayerColors[0].a;
    if (alpha <= 0.001) {
        discard;
    }

    if (OutlineOptions.x <= 0.5) {
        float outlineDepth = unpackDepth(expanded.rgb);
        float sceneDepth = texture(SceneDepthSampler, texCoord).r;
        if (outlineDepth > sceneDepth + 0.00001) {
            discard;
        }
    }

    fragColor = vec4(LayerColors[0].rgb, alpha);
}
