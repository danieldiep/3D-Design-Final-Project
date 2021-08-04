#version 430


in vec3 varyingNormal, varyingLightDir, varyingVertPos, varyingHalfVec, varyingTangent;
in vec4 shadow_coord;
in vec3 originalVertex;
in vec2 tc;
in vec3 eyeSpacePos;
out vec4 fragColor;



struct PositionalLight
{	vec4 ambient, diffuse, specular;
	vec3 position;
};

struct Material
{	vec4 ambient, diffuse, specular;
	float shininess;
};

int tree_bump = 0;

uniform vec4 globalAmbient;
uniform PositionalLight light;
uniform Material material;
uniform mat4 mv_matrix;
uniform mat4 proj_matrix;
uniform mat4 norm_matrix;
uniform mat4 shadowMVP;
uniform int isAbove;
uniform int map;
uniform float fogAmount;
uniform float alpha;
uniform float flipNormal;

layout (binding=0) uniform sampler2DShadow shadowTex;
layout (binding=1) uniform sampler2D samp;

float lookup(float x, float y){
	float t = textureProj(shadowTex, shadow_coord + vec4(x * 0.001 * shadow_coord.w,
	y * 0.001 * shadow_coord.w, -0.01, 0.0));
	return t;
}


void main(void)
{	float shadowFactor=0.0;

	vec3 L = normalize(varyingLightDir);
    vec3 V = normalize(-varyingVertPos);
    vec3 N = normalize(varyingNormal);
	vec3 H = normalize(varyingHalfVec);

	float swidth = 2.5;
	vec2 o = mod(floor(gl_FragCoord.xy), 2.0) * swidth;
	shadowFactor += lookup(-1.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup(-1.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x,  1.5*swidth - o.y);
	shadowFactor += lookup( 0.5*swidth + o.x, -0.5*swidth - o.y);
	shadowFactor = shadowFactor / 4.0;

	float notInShadow = textureProj(shadowTex, shadow_coord);
	// hi res PCF
	/*	float width = 2.5;
        float endp = width * 3.0 + width/2.0;
        for (float m=-endp ; m<=endp ; m=m+width)
        {	for (float n=-endp ; n<=endp ; n=n+width)
            {	shadowFactor += lookup(m,n);
        }	}
        shadowFactor = shadowFactor / 64.0;
    */
	// this would produce normal hard shadows
	//	shadowFactor = lookup(0.0, 0.0);


	float cosPhi = dot(H,N);
	if(map == 0){
		float a = 1;		// controls depth of bumps
		float b = 50;	// controls width of bumps
		float x = originalVertex.x;
		float y = originalVertex.y;
		float z = originalVertex.z;
		N.x = varyingNormal.x + a*cos(b*x);
		N.y = varyingNormal.y + a*cos(b*y);
		N.z = varyingNormal.z + a*cos(b*z);
		N = normalize(N);
		vec3 R = normalize(reflect(-L, N));
		cosPhi = dot(V,R);
	}

	vec4 tex = texture(samp, tc);

    float cosTheta = dot(L,N);

	vec4 shadowColor = globalAmbient * material.ambient
	+ light.ambient * material.ambient;

	vec4 lightedColor = tex * (light.diffuse * material.diffuse * max(cosTheta,0.0)
	+ light.specular * material.specular
	* pow(max(cosPhi,0.0),material.shininess*3.0));


	vec4 fogColor = vec4(0.8, 0.8, 0.8, 1.0);
	float dist = length(eyeSpacePos);

	float fog = fogAmount;
	if(fog < 0.0){fog = 0.0;}
	float fogFactor = dist * fog;

	if(fogFactor > 1.0){fogFactor = 1.0;}
	// not producing soft shadows for some reason, but weird shadow problem fixed
	fragColor = mix(vec4((shadowColor.xyz*shadowFactor + lightedColor.xyz*shadowFactor), 1.0), fogColor, fogFactor);

	if(notInShadow == 1.0){
		fragColor += ((globalAmbient * material.ambient) * shadowFactor) * (1.0 - fogFactor);
	}

	if(isAbove == 1){}
	else{fragColor = mix(fragColor, vec4(0.0, 0.05, 0.4, 1.0), 0.6); }// blue tint

	fragColor = vec4(fragColor.xyz, alpha);
}
