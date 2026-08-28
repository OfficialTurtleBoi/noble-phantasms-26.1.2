#version 330

#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:dynamictransforms.glsl>

uniform sampler2D Sampler0;

in float sphericalVertexDistance;
in float cylindricalVertexDistance;
in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    vec4 sampled = texture(Sampler0, texCoord0);
    if (sampled.a <= 0.0) {
        discard;
    }

    float luminance = dot(sampled.rgb, vec3(0.2126, 0.7152, 0.0722));
    vec3 tint = vec3(0.4, 0.76862746, 1.0);
    vec3 energyColor = tint * mix(0.35, 1.0, luminance);
    vec4 color = vec4(energyColor * vertexColor.rgb,
            sampled.a * vertexColor.a * ColorModulator.a * 0.6);
    fragColor = apply_fog(
        color,
        sphericalVertexDistance,
        cylindricalVertexDistance,
        FogEnvironmentalStart,
        FogEnvironmentalEnd,
        FogRenderDistanceStart,
        FogRenderDistanceEnd,
        FogColor
    );
}
