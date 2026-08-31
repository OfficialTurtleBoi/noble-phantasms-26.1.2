#version 330

uniform sampler2D EntitySampler;
uniform sampler2D EntityDepthSampler;
uniform sampler2D SceneDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(EntitySampler, texCoord);
    if (color.a <= 0.001) {
        discard;
    }
    float entityDepth = texture(EntityDepthSampler, texCoord).r;
    float sceneDepth = texture(SceneDepthSampler, texCoord).r;
    if (entityDepth > sceneDepth + 0.00001) {
        discard;
    }
    fragColor = color;
}
