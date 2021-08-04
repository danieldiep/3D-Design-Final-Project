#version 430

in vec2 tes_out;
in vec4 shadow_coord;
out vec4 color;
uniform mat4 mvp;

layout (binding = 0) uniform sampler2DShadow shadowTex;
layout (binding = 1) uniform sampler2D tex_height;
layout (binding = 2) uniform sampler2D tex_normal;

/* ---- for lighting ---- */
in vec3 varyingVertPos;
in vec3 varyingLightDir;
struct PositionalLight
{	vec4 ambient; vec4 diffuse; vec4 specular; vec3 position; };
struct Material
{	vec4 ambient; vec4 diffuse; vec4 specular; float shininess; };
uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform float fogAmount;
uniform int isAbove;
/* ---------------------- */

float lookup(float x, float y){
	float t = textureProj(shadowTex, shadow_coord + vec4(x * 0.001 * shadow_coord.w,
	y * 0.001 * shadow_coord.w, -0.01, 0.0));
	return t;
}

vec3 calcNewNormal()
{
	vec3 normal = vec3(0,1,0);
	vec3 tangent = vec3(1,0,0);
	vec3 bitangent = cross(tangent, normal);
	mat3 tbn = mat3(tangent, bitangent, normal);
	vec3 retrievedNormal = texture(tex_normal,tes_out).xyz;
	retrievedNormal = retrievedNormal * 2.0 - 1.0;
	vec3 newNormal = tbn * retrievedNormal;
	newNormal = normalize(newNormal);
	return newNormal;
}

void main(void)
{
	float shadowFactor=0.0;

	float swidth = 2.5;
	vec2 o = mod(floor(gl_FragCoord.xy), 2.0) * swidth;
	shadowFactor += lookup(-1.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup(-1.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor = shadowFactor / 4.0;

	float notInShadow = textureProj(shadowTex, shadow_coord);


	vec3 L = normalize(varyingLightDir);
	vec3 V = normalize(-varyingVertPos);

	vec3 N = calcNewNormal();

	vec3 R = normalize(reflect(-L, N));
	float cosTheta = dot(L,N);
	float cosPhi = dot(V,R);

	vec4 fogColor = vec4(0.8, 0.8, 0.8, 1.0);
	float dist = length(varyingVertPos);

	float fog = fogAmount;
	if(fog < 0.0){fog = 0.0;}
	float fogFactor = dist * fog;

	if(fogFactor > 1.0){fogFactor = 1.0;}

	vec4 shadowColor = globalAmbient * material.ambient
	+ light.ambient * material.ambient;

	vec4 lightedColor = 0.5 *
	( globalAmbient * material.ambient  +  light.ambient * material.ambient
	+ light.diffuse * material.diffuse * max(cosTheta,0.0)
	+ light.specular * material.specular * pow(max(cosPhi,0.0), material.shininess) + 0.5);


	vec4 brown = vec4(0.8, 0.52, 0.24, 1.0);
	vec4 green = vec4(0.13, 0.52, 0.13, 1.0);

	vec4 c = mix(brown, green, texture(tex_height,tes_out).y);
	lightedColor = lightedColor * c;



	color = mix(vec4((shadowColor.xyz*shadowFactor + lightedColor.xyz*shadowFactor), 1.0), fogColor, fogFactor);

	if(isAbove == 1){

	}
	else{color = mix(color, vec4(0.0, 0.05, 0.4, 1.0), 0.6); }// blue tint

}