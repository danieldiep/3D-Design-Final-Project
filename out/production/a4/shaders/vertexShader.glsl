#version 430

layout (location=0) in vec3 vertPos;
layout (location=1) in vec2 texCoord;
layout (location=2) in vec3 vertNormal;
layout (location=3) in vec3 vertTangent;
out vec2 tc;
out vec3 eyeSpacePos;
out vec3 originalVertex;

out vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec, varyingTangent;
out vec4 shadow_coord;

struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};
struct Material
{	vec4 ambient, diffuse, specular;
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform float fogAmount;
uniform int map;
uniform float alpha;
uniform float flipNormal;
layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D samp;

void main(void)
{	//output the vertex position to the rasterizer for interpolation
	varyingVertPos = (mv_matrix * vec4(vertPos,1.0)).xyz;

	//get a vector from the vertex to the light and output it to the rasterizer for interpolation
	varyingLightDir = light.position - varyingVertPos;

	originalVertex = vertPos;
	tc = texCoord;
	//get a vertex normal vector in eye space and output it to the rasterizer for interpolation
	varyingNormal = (norm_matrix * vec4(vertNormal,1.0)).xyz;
	varyingTangent = (norm_matrix * vec4(vertTangent,1.0)).xyz;

	varyingHalfVec = normalize(normalize(varyingLightDir) + normalize(-varyingVertPos)).xyz;

	shadow_coord = shadowMVP * vec4(vertPos,1.0);
	if (flipNormal < 0) varyingNormal = -varyingNormal;
	eyeSpacePos = (mv_matrix * vec4(vertPos,1.0)).xyz; // for fog
	gl_Position = proj_matrix * mv_matrix * vec4(vertPos,1.0);

}
