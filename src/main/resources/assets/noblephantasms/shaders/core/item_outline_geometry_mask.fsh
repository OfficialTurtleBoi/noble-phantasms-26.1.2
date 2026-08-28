#version 330

in vec4 vertexColor;

out vec4 fragColor;

vec3 packDepth(float depth) {
    vec3 encodedDepth = fract(depth * vec3(1.0, 255.0, 65025.0));
    encodedDepth -= encodedDepth.yzz * vec3(1.0 / 255.0, 1.0 / 255.0, 0.0);
    return encodedDepth;
}

void main() {
    fragColor = vec4(packDepth(gl_FragCoord.z), vertexColor.a);
}
