#version 430

out vec4 color;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

vec4 vertices[6] = vec4[6](
vec4(0.0,0.0,0.0, 1.0),
vec4( 20.0,0.0,0.0, 1.0),
vec4( 0.0,0.0,0.0, 1.0),
vec4( 0.0,20.0,0.0, 1.0),
vec4(0.0,0.0,0.0, 1.0),
vec4( 0.0,0.0,20.0, 1.0));

void main(void){

	if(gl_VertexID == 0 || gl_VertexID == 1){
		color = vec4(1.0, 0.0, 0.0, 1.0);
	}
	else if(gl_VertexID == 2 || gl_VertexID == 3){
		color = vec4(0.0, 1.0, 0.0, 1.0);
	}
	else{ color = vec4(0.0, 0.0, 1.0, 1.0); }
	gl_Position = proj_matrix * mv_matrix * vertices[gl_VertexID];
} 
