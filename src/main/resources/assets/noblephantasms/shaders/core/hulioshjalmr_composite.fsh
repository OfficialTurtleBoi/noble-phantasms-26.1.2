#version 330

uniform sampler2D ConcealmentSampler;
uniform sampler2D ConcealmentDepthSampler;
uniform sampler2D SceneDepthSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(ConcealmentSampler, texCoord);
    if (color.a <= 0.001) {
        discard;
    }
    float concealmentDepth = texture(ConcealmentDepthSampler, texCoord).r;
    float sceneDepth = texture(SceneDepthSampler, texCoord).r;
    if (concealmentDepth > sceneDepth + 0.00001) {
        discard;
    }
    fragColor = color;
}
