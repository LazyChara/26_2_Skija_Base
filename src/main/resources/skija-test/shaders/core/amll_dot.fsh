#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

void main() {
    float dist = distance(texCoord0, vec2(0.5));
    float aa = fwidth(dist);
    float alpha = 1.0 - smoothstep(0.5 - aa, 0.5, dist);

    if (alpha <= 0.0) {
        discard;
    }

    fragColor = vec4(vertexColor.rgb, alpha * vertexColor.a) * ColorModulator;
}
