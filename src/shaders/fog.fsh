HAZE_VERTEX_HEADER
HAZE_NOISE

uniform sampler2D SceneDepth;
uniform sampler2D SwampMask;

IN(0) vec2 screen;
IN(1) vec4 unproject0;
IN(2) vec4 unproject1;
IN(3) vec4 unproject2;
IN(4) vec4 unproject3;
IN(5) float farFlag;

OUT(0) vec4 fragColor;

float ndcOf(float device) {
#ifdef RENDERPEARL_DEPTH_IS_ZERO_TO_ONE
    return device;
#else
    return device * 2.0 - 1.0;
#endif
}

float integral(float k, float h0, float scale, float dy, float len) {
    float lowest = -1.5 * scale;
    float h1 = h0 + dy * len;
    if (abs(dy * len) < 1e-3 * scale) {
        return k * len * exp(-max(0.5 * (h0 + h1), lowest) / scale);
    }
    float cut = clamp((lowest - h0) / dy, 0.0, len);
    float low = dy > 0.0 ? cut : len - cut;
    float high = scale * (exp(-max(h0, lowest) / scale) - exp(-max(h1, lowest) / scale)) / dy;
    return k * (exp(1.5) * low + max(high, 0.0));
}

float column(float k, float h0, float scale, float dy, float len) {
    float top = 3.0 * scale;
    float ta = 0.0;
    float tb = len;
    if (abs(dy) < 1e-6) {
        tb = h0 < top ? len : 0.0;
    } else if (dy > 0.0) {
        tb = min(len, (top - h0) / dy);
    } else {
        ta = max(0.0, (top - h0) / dy);
    }
    if (tb <= ta) {
        return 0.0;
    }
    return max(integral(k, h0 + dy * ta, scale, dy, tb - ta) - k * exp(-3.0) * (tb - ta), 0.0);
}

void main() {
    vec3 tint = ColorModulator.rgb;
    float k = ColorModulator.w * 0.06;
    float h0 = ModelOffset.x;
    float scale = max(ModelOffset.y, 0.5);
    float warmth = max(ModelOffset.z, 0.0);
    float water = max(-ModelOffset.z, 0.0);
    vec2 sun = vec2(TextureMat[0].x, TextureMat[0].y);
    vec2 maskOffset = TextureMat[0].zw;
    float swampH0 = TextureMat[1].x;
    float wisps = TextureMat[1].w > 0.0 ? 1.0 : 0.0;
    float swampK = (abs(TextureMat[1].w) - 1.0) * 0.06;
    float seconds = TextureMat[1].y;
    vec2 cameraXZ = vec2(TextureMat[2].x, TextureMat[2].y);
    float cameraY = TextureMat[1].z;
    float cave = TextureMat[2].z;
    vec3 caveTint = TextureMat[3].xyz;
    float caveK = TextureMat[3].w;
    float lod = TextureMat[2].w;

    float far = farFlag > 0.5 ? 1.0 : 0.0;
    mat4 unproject = mat4(unproject0, unproject1, unproject2, unproject3);
    vec4 front = unproject * vec4(screen, ndcOf(0.25), 1.0);
    vec4 back = unproject * vec4(screen, ndcOf(0.75), 1.0);
    vec3 dir = normalize(transpose(mat3(ModelViewMat)) * normalize(back.xyz / back.w - front.xyz / front.w));

    ivec2 pixel = ivec2(gl_FragCoord.xy);
    ivec2 limit = textureSize(SceneDepth, 0) - 1;
    vec2 texel = 2.0 / vec2(limit + 1);
    float amount = 0.0;
    float surface = 0.0;
    float under = 0.0;
    float len = 100000.0;
    bool sky = false;
    for (int ox = -1; ox <= 1; ox++) {
        for (int oy = -1; oy <= 1; oy++) {
            float device = texelFetch(SceneDepth, clamp(pixel + ivec2(ox, oy), ivec2(0), limit), 0).r;
            float reach = 100000.0;
            if (abs(device - far) >= 1e-7) {
                vec4 point = unproject * vec4(screen + vec2(ox, oy) * texel, ndcOf(device), 1.0);
                reach = length(point.xyz / point.w);
            }
            if (lod > 0.0 && device != far) {
                reach = abs(device - far) < 1e-5 ? 4.0 * lod : min(reach, 4.0 * lod);
                reach = reach > lod ? lod + (reach - lod) * 0.15 : reach;
            }
            float w = (ox == 0 ? 2.0 : 1.0) * (oy == 0 ? 2.0 : 1.0) / 16.0;
            float s = column(k, h0, scale, dir.y, reach) * (1.0 - cave);
            float u = caveK * cave * min(reach, 256.0);
            amount += w * (1.0 - exp(-(s + u)));
            surface += w * s;
            under += w * u;
            if (ox == 0 && oy == 0) {
                len = reach;
                sky = abs(device - far) < 1e-5;
            }
        }
    }
    if (swampK > 0.0 && water < 0.5 && !sky) {
        float dist = min(len, 640.0);
        float t0 = 0.0;
        float t1 = dist;
        if (abs(dir.y) > 1e-4) {
            float a = (-2.0 - swampH0) / dir.y;
            float b = (9.0 - swampH0) / dir.y;
            t0 = max(min(a, b), 0.0);
            t1 = min(max(a, b), dist);
        } else if (swampH0 < -2.0 || swampH0 > 9.0) {
            t1 = 0.0;
        }
        if (t1 > t0) {
            vec3 origin = vec3(cameraXZ.x, cameraY, cameraXZ.y);
            vec3 drift = vec3(seconds * 0.4, 0.0, seconds * 0.25);
            float sum = 0.0;
            float jitter = hazeDither(gl_FragCoord.xy) / 255.0;
            for (int i = 0; i < 32; i++) {
                float u = (float(i) + jitter) / 32.0;
                float t = t0 + (t1 - t0) * u * u;
                float dt = (t1 - t0) * 2.0 * u / 32.0;
                vec3 at = dir * t;
                vec2 uv = (maskOffset + at.xz) / 2048.0;
                vec2 edge = step(vec2(1.0 / 128.0), uv) * step(uv, vec2(127.0 / 128.0));
                float y = swampH0 + at.y;
                float profile = smoothstep(-2.0, 0.0, y) * exp(-max(y, 0.0) / 2.0) * (1.0 - smoothstep(5.0, 9.0, y));
                vec3 world = origin + at + drift;
                float blot = smoothstep(0.22, 0.55, hazeFbm3(world * vec3(0.018, 0.06, 0.018), 2));
                sum += texture(SwampMask, uv).r * edge.x * edge.y * profile * blot * dt;
            }
            float sw = swampK * 0.7 * sum * (1.0 - cave);
            amount = 1.0 - (1.0 - amount) * exp(-sw);
            surface += sw;
        }
    }
    float depth = surface + under;
    float share = depth > 1e-5 ? under / depth : 0.0;
    if (wisps > 0.5 && water < 0.5) {
        vec3 origin = vec3(cameraXZ.x, cameraY, cameraXZ.y);
        vec3 drift = vec3(seconds * 0.35, seconds * 0.05, seconds * 0.2) * 0.08;
        float base = cameraY - h0;
        float reach = min(len, 64.0);
        float step = reach / 12.0;
        float sum = 0.0;
        float weight = 0.0;
        for (int i = 0; i < 12; i++) {
            vec3 at = origin + dir * (step * (float(i) + 0.5));
            float w = mix(exp(-clamp((at.y - base) / scale, -1.5, 60.0)), 1.0, cave);
            sum += w * hazeFbm3(at * 0.05 + drift, 2);
            weight += w;
        }
        float n = weight > 1e-4 ? sum / weight : 0.5;
        float local = column(k, h0, scale, dir.y, reach) * (1.0 - cave) + caveK * cave * reach;
        amount = 1.0 - (1.0 - amount) * exp(-local * (0.6 * n - 0.3));
    }
    amount = clamp(amount, 0.0, 0.97);

    float facing = max(dot(dir, vec3(sun, 0.0)), 0.0);
    float glow = warmth * pow(facing, 3.0) * (1.0 - share);
    vec3 warm = vec3(1.0, 0.78, 0.52);
    vec3 color = mix(mix(tint, caveTint, share), warm * max(tint.r, 0.6), glow * 0.7);
    color += hazeDither(gl_FragCoord.xy) / 255.0;

    fragColor = vec4(color * amount, amount);
}
