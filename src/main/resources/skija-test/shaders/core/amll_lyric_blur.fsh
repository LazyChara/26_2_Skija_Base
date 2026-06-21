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

const int RADIUS_TAPS = 4;
const float MAX_AMLL_BLUR_PX = 5.0;

float gaussianWeight(float x, float sigma) {
    return exp(-(x * x) / (2.0 * sigma * sigma));
}

vec4 samplePremultiplied(vec2 uv) {
    vec4 c = texture(Sampler0, uv);
    c.rgb *= c.a;
    return c;
}

void main() {
    float guiScale = vertexColor.g * 25.5;
    float radiusPx = vertexColor.r * MAX_AMLL_BLUR_PX * max(1.0, guiScale);
    float globalAlpha = vertexColor.a;

    if (radiusPx <= 0.05) {
        vec4 c = texture(Sampler0, texCoord0);
        c.a *= globalAlpha;
        fragColor = c * ColorModulator;
        return;
    }

    vec2 texel = 1.0 / vec2(textureSize(Sampler0, 0));
    float sigma = max(radiusPx * 0.55, 0.001);
    vec4 sum = vec4(0.0);
    float weightSum = 0.0;

    for (int y = -RADIUS_TAPS; y <= RADIUS_TAPS; y++) {
        for (int x = -RADIUS_TAPS; x <= RADIUS_TAPS; x++) {
            vec2 tap = vec2(float(x), float(y));
            float d = length(tap);
            float w = gaussianWeight(d * radiusPx / float(RADIUS_TAPS), sigma);
            vec2 uv = texCoord0 + tap * texel * (radiusPx / float(RADIUS_TAPS));
            vec4 c = samplePremultiplied(uv);
            sum += c * w;
            weightSum += w;
        }
    }

    vec4 result = sum / max(weightSum, 0.0001);
    if (result.a > 0.0001) {
        result.rgb /= result.a;
    }
    result.a *= globalAlpha;
    fragColor = result * ColorModulator;
}
