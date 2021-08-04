#version 430

in vec4 color;
out vec4 fragColor;

uniform mat4 mv_matrix;
uniform mat4 proj_matrix;

void main(void)
{
	fragColor = color;
}
