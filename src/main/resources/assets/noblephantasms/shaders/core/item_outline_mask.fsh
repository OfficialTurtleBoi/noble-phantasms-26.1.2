#version 330

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

vec3 packDepth(float depth) {
    vec3 encodedDepth = fract(depth * vec3(1.0, 255.0, 65025.0));
    encodedDepth -= encodedDepth.yzz * vec3(1.0 / 255.0, 1.0 / 255.0, 0.0);
    return encodedDepth;
}

void main() {
    float textureAlpha = texture(Sampler0, texCoord0).a;
    if (textureAlpha < 0.1) {
        discard;
    }

    fragColor = vec4(packDepth(gl_FragCoord.z), vertexColor.a * textureAlpha);
}
