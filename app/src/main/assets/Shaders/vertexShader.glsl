#version 300 es
precision highp float;

layout(location = 0) in vec4 vertexPosition;
layout(location = 1) in vec3 vertexNormal;

uniform mat4 uViewProjectionMatrix;
uniform mat4 uModelMatrix;
uniform mat3 uNormalMatrix;

out vec3 vPosition;
out vec3 vNormal;

void main() {
    vec4 worldPosition = uModelMatrix * vertexPosition;
    gl_Position = uViewProjectionMatrix * worldPosition;

    vPosition = worldPosition.xyz;
    vNormal = uNormalMatrix * vertexNormal;
}