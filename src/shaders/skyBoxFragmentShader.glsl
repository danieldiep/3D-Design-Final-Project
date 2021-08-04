#version 430

in vec3 tc;
in float altitude;
out vec4 fragColor;

layout (binding = 0) uniform samplerCube samp;
uniform mat4 v_matrix;
uniform mat4 p_matrix;
uniform int isAbove;
uniform float fogAmount;

void main(void){

    vec4 fogColor = vec4(0.8, 0.8, 0.8, 1.0);

    float fog = fogAmount;
    if(fog <= 0.0){fog = 0.0;}
    else{
        fog *= 40;
        fog += 1.0 - (altitude * 2);
    }

    if(fog > 1.0){fog = 1.0;}


    if ((altitude < .47) && (isAbove == 0)) fragColor = mix(vec4(0,0,.2,1), fogColor, fog);
    else fragColor = mix(texture(samp,tc), fogColor, fog );
}
