#version 330

#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 sampled = texture(Sampler0, texCoord0) * vertexColor;
    if (sampled.a <= 0.001) {
        discard;
    }

    float luminance = dot(sampled.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 darkSepia = vec3(0.466667, 0.407843, 0.325490);
    vec3 midSepia = vec3(0.674510, 0.615686, 0.529412);
    vec3 lightSepia = vec3(0.870588, 0.819608, 0.729412);
    vec3 sepia = luminance < 0.5
            ? mix(darkSepia, midSepia, luminance * 2.0)
            : mix(midSepia, lightSepia, (luminance - 0.5) * 2.0);
    fragColor = vec4(sepia * ColorModulator.rgb, sampled.a * ColorModulator.a);
}
