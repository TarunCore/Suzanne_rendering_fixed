#version 300 es
precision highp float;

const float PI = 3.14159265359;
const float MAX_TRACE_DISTANCE = 25.0;
const int NUM_TRACE_STEPS = 50;
const float INTERSECTION_PRECISION = 0.00001;
const float THICKNESS_MAX_DISTANCE = 1.0;
const int NUM_THICKNESS_SAMPLES = 64;
const float NUM_SAMPLES_INV = 1.0 / float(NUM_THICKNESS_SAMPLES);

in vec3 vPosition;
in vec3 vNormal;

out vec4 fragColor;

// Function Declarations
vec2 Scene(vec3 p);
float Hash(float n);
vec3 RandomSphereDir(vec2 rnd);
vec3 RandomHemisphereDir(vec3 dir, float i);

float CalculateThickness(vec3 p, vec3 n, float maxDist) {
    float thickness = 0.0;

    for (int i = 0; i < NUM_THICKNESS_SAMPLES; i++) {
        // Randomly sample along the hemisphere inside the surface
        // To sample inside the surface, flip the normal
        float l = Hash(float(i)) * maxDist;
        vec3 rd = normalize(-n + RandomHemisphereDir(-n, l)) * l;

        // Accumulate
        thickness += l + Scene(p + rd).x;
    }

    return clamp(thickness * NUM_SAMPLES_INV, 0.0, 1.0);
}

float sdPlane(vec3 p) {
    return p.y;
}

float sdCapsule(vec3 p, vec3 a, vec3 b, float r) {
    vec3 pa = p - a, ba = b - a;
    float h = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
    return length(pa - ba * h) - r;
}

float smin(float a, float b, float k) {
    float res = exp(-k * a) + exp(-k * b);
    return -log(res) / k;
}

vec2 opU(vec2 d1, vec2 d2) {
    return (d1.x < d2.x) ? d1 : d2;
}

float opBlend(vec2 d1, vec2 d2) {
    return smin(d1.x, d2.x, 8.0);
}

float Hash(float n) {
    return fract(sin(n) * 3538.5453);
}

vec3 RandomSphereDir(vec2 rnd) {
    float s = rnd.x * PI * 2.0;
    float t = rnd.y * 2.0 - 1.0;
    return vec3(sin(s), cos(s), t) / sqrt(1.0 + t * t);
}

vec3 RandomHemisphereDir(vec3 dir, float i) {
    vec3 v = RandomSphereDir(vec2(Hash(i + 1.0), Hash(i + 2.0)));
    return v * sign(dot(v, dir));
}

vec2 Scene(vec3 p) {
    float time = 195.0 + 7.0 + iTime;
    vec2 res = vec2(sdPlane(p - vec3(0.0, -1.0, 0.0)), 0.0);

    for (int i = 0; i < 15; i++) {
        vec3 sp = texture(iChannel0, vec2(float(i) / 15.0, 0.2 + sin(time * 0.00001) * 0.1)).xyz;
        vec3 ep = texture(iChannel0, vec2(float(i) / 15.0, 0.4 + sin(time * 0.00001) * 0.1)).xyz;

        sp.x = Hash(sp.xy);
        sp.y = Hash(sp.zy);
        sp.z = Hash(sp.xz);
        sp = sp * 2.0 - 1.0;

        ep.x = Hash(ep.xy);
        ep.y = Hash(ep.zy);
        ep.z = Hash(ep.xz);

        res.x = opBlend(res, vec2(sdCapsule(p, sp * 1.5, ep * 1.5, 0.20), float(i) + 1.));
    }

    return res;
}

void main() {
    vec2 res = Raymarch(vPosition, vNormal);

    vec3 col = vec3(0.0);

    if (res.y > -0.5) {
        col = vec3(CalculateThickness(vPosition, vNormal, THICKNESS_MAX_DISTANCE));
        col = pow(col * col * col * 7.0 + 0.0125, vec3(1.0 / 2.2));
    }

    fragColor = vec4(col, 1.0);
}