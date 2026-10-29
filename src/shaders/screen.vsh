HAZE_VERTEX_HEADER

IN(0) vec3 Position;

OUT(0) vec2 screen;
OUT(1) vec4 unproject0;
OUT(2) vec4 unproject1;
OUT(3) vec4 unproject2;
OUT(4) vec4 unproject3;
OUT(5) float farFlag;

float deviceOf(float ndcZ) {
#ifdef RENDERPEARL_DEPTH_IS_ZERO_TO_ONE
    return ndcZ;
#else
    return ndcZ * 0.5 + 0.5;
#endif
}

void main() {
    gl_Position = vec4(Position.xy, 0.0, 1.0);
    screen = Position.xy;
    mat4 unproject = inverse(ProjMat);
    unproject0 = unproject[0];
    unproject1 = unproject[1];
    unproject2 = unproject[2];
    unproject3 = unproject[3];
    vec4 near = ProjMat * vec4(0.0, 0.0, -1.0, 1.0);
    vec4 farPoint = ProjMat * vec4(0.0, 0.0, -100.0, 1.0);
    farFlag = deviceOf(near.z / near.w) > deviceOf(farPoint.z / farPoint.w) ? 0.0 : 1.0;
}
