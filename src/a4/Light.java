package a4;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import graphicslib3D.Material;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Light {

    private ObjectManager lightObject;

    // white light Material
    private float[] globalAmbient = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
    private float[] lightAmbient = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
    private float[] lightDiffuse = new float[] { 0.8f, 0.8f, 0.8f, 1.0f };
    private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    private Vector3f initialLightLoc = new Vector3f(-8.0f, 15.0f, 5.0f);

    private float[] lightPosition = new float[3];

    public Light(){}

    public Light(ObjectManager o){
        lightObject = o;
        lightObject.setupVertices();
        lightObject.setScale(0.5f);

        lightPosition[0]=initialLightLoc.x();
        lightPosition[1]=initialLightLoc.y();
        lightPosition[2]=initialLightLoc.z();
    }

    public void installLights(int renderingProgram, Matrix4f vMatrix, Material objMat) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        //locations of the light and material fields in the shader
        int globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
        int ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
        int posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
        int mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
        int mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
        int mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
        int mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");

        //set ight and material values in the shader
        gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
        gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
        gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, objMat.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, objMat.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, objMat.getSpecular(), 0);
        gl.glProgramUniform1f(renderingProgram, mshiLoc, objMat.getShininess());


        lightObject.setPosition(new Vector4f(getLightPosition(), 1.0f));
    }


    public void setSceneObject(ObjectManager o){lightObject = o;}
    public ObjectManager getLightObject(){return lightObject;}

    public Vector3f getLightPosition(){return new Vector3f(lightPosition[0], lightPosition[1], lightPosition[2]);}
}
