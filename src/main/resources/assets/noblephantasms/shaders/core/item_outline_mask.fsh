#version 330

uniform sampler2D Sampler0;
uniform sampler2D MaskSampler;

in vec4 vertexColor;
in vec2 texCoord0;
in vec2 maskTexCoord;

out vec4 fragColor;

vec3 packDepth(float depth) {
    vec3 encodedDepth = fract(depth * vec3(1.0, 255.0, 65025.0));
    encodedDepth -= encodedDepth.yzz * vec3(1.0 / 255.0, 1.0 / 255.0, 0.0);
    return encodedDepth;
}

void main() {
    float textureAlpha = texture(Sampler0, texCoord0).a;
    float maskAlpha = texture(MaskSampler, maskTexCoord).a;
    float combinedAlpha = min(textureAlpha, maskAlpha);
    if (combinedAlpha <= 0.0) {
        discard;
    }

    fragColor = vec4(packDepth(gl_FragCoord.z), vertexColor.a * combinedAlpha);
}
