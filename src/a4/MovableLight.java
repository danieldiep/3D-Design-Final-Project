package a4;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import graphicslib3D.Material;

public class MovableLight extends Light implements MouseMotionListener, MouseWheelListener {
    private float[] globalAmbient = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
    private float[] lightAmbient = new float[] { 0.5f, 0.5f, 0.5f, 1.0f };
    private float[] lightDiffuse = new float[] { 0.8f, 0.8f, 0.8f, 1.0f };
    private float[] lightSpecular = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };

    private boolean listening = false;
    private ObjectManager lightObject;
    private Matrix4f viewMatrix;
    private float[] lightPos = new float[3];
    private Vector3f initialLoc = new Vector3f(0.0f, 15.0f, 0.0f);
    private Vector3f currentLoc = new Vector3f();
    private DecimalFormat format = new DecimalFormat("###,###.##");

    private float lastX = 0.0f;
    private float lastY = 0.0f;

    private float width;
    private float height;

    public MovableLight(Matrix4f v, ObjectManager o){
        viewMatrix = v;
        lightObject = o;
        lightObject.setupVertices();
        lightObject.setScale(0.5f);
        super.setSceneObject(lightObject);
    }

    public void toggleMobileLight(){
        listening = !listening;
        currentLoc.set(initialLoc);
        lightPos[0] = initialLoc.x; lightPos[1] = initialLoc.y; lightPos[2] = initialLoc.z;
    }

    //@Override
    public void installLights(int renderingProgram, Matrix4f vMatrix, Material objMat) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        // get the locations of the light and material fields in the shader
        int globalAmbLoc = gl.glGetUniformLocation(renderingProgram, "globalAmbient");
        int ambLoc = gl.glGetUniformLocation(renderingProgram, "light.ambient");
        int diffLoc = gl.glGetUniformLocation(renderingProgram, "light.diffuse");
        int specLoc = gl.glGetUniformLocation(renderingProgram, "light.specular");
        int posLoc = gl.glGetUniformLocation(renderingProgram, "light.position");
        int mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
        int mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
        int mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
        int mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");

        //  set the uniform light and material values in the shader
        gl.glProgramUniform4fv(renderingProgram, globalAmbLoc, 1, globalAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, ambLoc, 1, lightAmbient, 0);
        gl.glProgramUniform4fv(renderingProgram, diffLoc, 1, lightDiffuse, 0);
        gl.glProgramUniform4fv(renderingProgram, specLoc, 1, lightSpecular, 0);
        gl.glProgramUniform3fv(renderingProgram, posLoc, 1, lightPos, 0);
        gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, objMat.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, objMat.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, objMat.getSpecular(), 0);
        gl.glProgramUniform1f(renderingProgram, mshiLoc, objMat.getShininess());


        lightObject.setPosition(new Vector4f(lightPos[0], lightPos[1], lightPos[2], 1.0f));
    }
    @Override
    public ObjectManager getLightObject(){return lightObject;}

    @Override
    public Vector3f getLightPosition(){return new Vector3f(lightPos[0], lightPos[1], lightPos[2]);}

    public boolean listening(){return listening;}
    public void setViewMatrix(Matrix4f v){viewMatrix = v;}

    @Override
    public void mouseDragged(MouseEvent e) {
        if(listening){
            float xDiff =  Math.abs(e.getX() - lastX);
            float yDiff =  Math.abs(e.getY() - lastY);
            if(xDiff > 20.0f){xDiff = 10.0f;}
            if(yDiff > 20.0f){yDiff = 10.0f;}
            float xInterval = 0.01f;
            float yInterval = 0.01f;

            xInterval = xInterval * xDiff;
            if(e.getX() < lastX){
                xInterval = -xInterval;
            }

            yInterval = yInterval * yDiff;
            if(e.getY() > lastY){
                yInterval = -yInterval;
            }


            currentLoc.x += xInterval;
            currentLoc.y += yInterval;
            updateLoc();

            lastX = e.getX();
            lastY = e.getY();
        }
    }

    private void updateLoc(){lightPos[0] = currentLoc.x; lightPos[1] = currentLoc.y; lightPos[2] = currentLoc.z;}

    public void setWindowSize(float w, float h){width = w; height = h;}

    @Override
    public void mouseMoved(MouseEvent e) {

    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if(listening){
            currentLoc.z += e.getWheelRotation() * 0.5f;
            updateLoc();
        }
    }
}
