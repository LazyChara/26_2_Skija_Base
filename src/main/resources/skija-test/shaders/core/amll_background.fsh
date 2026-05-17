#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

uniform sampler2D Sampler0;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

const float INV_255 = 1.0 / 255.0;
const float HALF_INV_255 = 0.5 / 255.0;
const float GRADIENT_NOISE_A = 52.9829189;
const vec2 GRADIENT_NOISE_B = vec2(0.06711056, 0.00583715);

float gradientNoise(vec2 uv) {
    return fract(GRADIENT_NOISE_A * fract(dot(uv, GRADIENT_NOISE_B)));
}

vec2 mirroredRepeat(vec2 uv) {
    vec2 m = mod(uv, vec2(2.0));
    return mix(m, vec2(2.0) - m, step(vec2(1.0), m));
}

void main() {
    vec4 result = texture(Sampler0, mirroredRepeat(texCoord0));
    if (result.a == 0.0) {
        discard;
    }

    float alphaVolumeFactor = vertexColor.b;
    float dither = INV_255 * gradientNoise(gl_FragCoord.xy) - HALF_INV_255;

    result.rgb *= alphaVolumeFactor;
    result.a *= alphaVolumeFactor;
    result.rgb += vec3(dither);

    float dist = distance(vertexColor.rg, vec2(0.5));
    float vignette = smoothstep(0.8, 0.3, dist);
    float mask = 0.6 + vignette * 0.4;
    result.rgb *= mask;

    fragColor = result * ColorModulator;
}
