#version 330

uniform sampler2D ConcealmentSampler;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 color = texture(ConcealmentSampler, texCoord);
    if (color.a <= 0.001) {
        discard;
    }
    fragColor = color;
}
