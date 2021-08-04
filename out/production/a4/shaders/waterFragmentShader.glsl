#version 430

in vec3 varyingNormal;
in vec3 varyingLightDir;
in vec3 varyingVertPos;
in vec2 tc;
in vec4 glp;
in vec4 shadow_coord;
out vec4 color;

layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D reflectTex;
layout (binding=2) uniform sampler2D refractTex;
layout (binding=3) uniform sampler3D noiseTex;

struct PositionalLight
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	vec3 position;
};

struct Material
{	vec4 ambient;  
	vec4 diffuse;  
	vec4 specular;  
	float shininess;
};

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;	 
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform int isAbove;
uniform float depthOffset;
uniform float fogAmount;

float lookup(float x, float y){
	float t = textureProj(shadowTex, shadow_coord + vec4(x * 0.001 * shadow_coord.w,
	y * 0.001 * shadow_coord.w, -0.01, 0.0));
	return t;
}

vec3 estimateWaveNormal(float ofs, float wScale, float hScale)
{	// estimate the normal using the noise texture
	// by looking up three height values around this vertex
	float h1 = (texture(noiseTex, vec3(((tc.s)    )*wScale, depthOffset, ((tc.t)+ofs)*wScale))).r * hScale;
	float h2 = (texture(noiseTex, vec3(((tc.s)-ofs)*wScale, depthOffset, ((tc.t)-ofs)*wScale))).r * hScale;
	float h3 = (texture(noiseTex, vec3(((tc.s)+ofs)*wScale, depthOffset, ((tc.t)-ofs)*wScale))).r * hScale;
	vec3 v1 = vec3(0, h1, -1);
	vec3 v2 = vec3(-1, h2, 1);
	vec3 v3 = vec3(1, h3, 1);
	vec3 v4 = v2-v1;
	vec3 v5 = v3-v1;
	vec3 normEst = normalize(cross(v4,v5));
	return normEst;
}

void main(void)
{	vec4 fogColor = vec4(0.8, 0.8, 0.8, 1.0);
	float dist = length(varyingVertPos);

	float fog = fogAmount;
	if(fog < 0.0){fog = 0.0;}
	float fogFactor = dist * fog;

	if(fogFactor > 1.0){fogFactor = 1.0;}

	// normalize the light, normal, and view vectors:
	vec3 L = normalize(varyingLightDir);
	vec3 V = normalize(-varyingVertPos);
	vec3 N = estimateWaveNormal(.0002, 32.0, 16.0);
	vec3 Nfres = normalize(varyingNormal); // for fresnel effect

	float shadowFactor=0.0;

	float swidth = 2.5;
	vec2 o = mod(floor(gl_FragCoord.xy), 2.0) * swidth;
	shadowFactor += lookup(-1.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup(-1.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor = shadowFactor / 4.0;

	float notInShadow = textureProj(shadowTex, shadow_coord);

	// get the angle between the light and surface normal:
	float cosTheta = dot(L,N);
	
	// compute light reflection vector, with respect N:
	vec3 R = normalize(reflect(-L, N));
	
	// angle between the view vector and reflected light:
	float cosPhi = dot(V,R);
	
	// angle between normal vector and view vector (for fresnel effect)
	float cosFres = dot(V,Nfres);

	// compute ADS contributions (per pixel):
	vec3 ambient = ((globalAmbient * material.ambient) + (light.ambient * material.ambient)).xyz;
	vec3 diffuse = light.diffuse.xyz * material.diffuse.xyz * max(cosTheta,0.0);
	vec3 specular = light.specular.xyz * material.specular.xyz * pow(max(cosPhi,0.0), material.shininess);
	
	vec4 mixColor, reflectColor, refractColor, blueColor;
	blueColor = vec4(0.0, 0.1, 0.8, 1.0);

	vec4 shadowColor = (globalAmbient * material.ambient
	+ light.ambient * material.ambient);

	if (isAbove == 1)
	{	float fresnel = acos(cosFres);
		fresnel = pow(clamp(fresnel-0.3, 0.0, 1.0), 3);
		refractColor = texture(refractTex, (vec2(glp.x,glp.y))/(2.0*glp.w)+0.5);
		reflectColor = texture(reflectTex, (vec2(glp.x,-glp.y))/(2.0*glp.w)+0.5);
		reflectColor = vec4((reflectColor.xyz * (ambient + diffuse) + 0.75*specular), 1.0);
		color = mix(mix(blueColor, mix(refractColor, reflectColor, fresnel), 0.8), fogColor, fogFactor);
	}
	else
	{	refractColor = texture(refractTex, (vec2(glp.x,glp.y))/(2.0*glp.w)+0.5);
		mixColor = (0.5 * blueColor) + (0.6 * refractColor);
		color = vec4((mixColor.xyz * (ambient + diffuse) + 0.75*specular), 1.0);
	}
	shadowFactor = shadowFactor * 0.4; // make shadows less visible on water since that's how they are IRL
	color = mix(color + (shadowColor*shadowFactor), fogColor, fogFactor);
	if (isAbove != 1) color = mix(color, fogColor, fogFactor);

	if(notInShadow == 1){
		color += ((globalAmbient * material.ambient) * shadowFactor) * (1.0 - fogFactor);
	}
}
