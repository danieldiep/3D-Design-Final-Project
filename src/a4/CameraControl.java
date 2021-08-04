package a4;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class CameraControl {
    private Vector4f startingPos;
    private Vector4f camera;
    private Vector4f uVector = new Vector4f(1.0f, 0.0f, 0.0f, 0.0f);
    private Vector4f vVector = new Vector4f(0.0f, 1.0f, 0.0f, 0.0f);
    private Vector4f nVector = new Vector4f(0.0f, 0.0f, 1.0f, 0.0f);
    private float angle = 0.1f;
    private Matrix4f viewMatrix;

    public CameraControl(float x, float y, float z){
        startingPos = new Vector4f(x, y, z, 1.0f);
        camera = new Vector4f(x, y, z, 1.0f);
        viewMatrix = new Matrix4f();
        cameraView();
    }

    public void cameraView() {
        Matrix4f rMat = new Matrix4f(
                uVector.x, vVector.x, nVector.x, 0.0f,
                uVector.y, vVector.y, nVector.y, 0.0f,
                uVector.z, vVector.z, nVector.z, 0.0f,
                0.0f, 0.0f, 0.0f, 1.0f
        );

        Matrix4f tMat = new Matrix4f(
          1.0f, 0.0f, 0.0f,0.0f,
          0.0f, 1.0f, 0.0f, 0.0f,
          0.0f, 0.0f, 1.0f, 0.0f,
                -camera.x, -camera.y, -camera.z, 1.0f
        );
        viewMatrix.identity();
        viewMatrix.mul(rMat);
        viewMatrix.mul(tMat);
    }
    public void forwardKey(){
        camera.sub(nVector);
        cameraView();
    }
    public void backwardKey(){
        camera.add(nVector);
        cameraView();
    }
    public void leftKey(){
        camera.sub(uVector);
        cameraView();
    }
    public void rightKey(){
        camera.add(uVector);
        cameraView();
    }
    public void downKey(){
        camera.sub(vVector);
        cameraView();
    }
    public void upKey(){
        camera.add(vVector);
        cameraView();
    }
    public void panLeftKey(){
        nVector.rotateAbout(angle, vVector.x, vVector.y, vVector.z);
        uVector.rotateAbout(angle, vVector.x, vVector.y, vVector.z);
        cameraView();
    }
    public void panRightKey(){
        nVector.rotateAbout(angle, -vVector.x, -vVector.y, -vVector.z);
        uVector.rotateAbout(angle, -vVector.x, -vVector.y, -vVector.z);
        cameraView();
    }
    public void pitchDownKey(){
        nVector.rotateAbout(angle, -uVector.x, -uVector.y, -uVector.z);
        vVector.rotateAbout(angle, -uVector.x, -uVector.y, -uVector.z);
        cameraView();
    }
    public void pitchUpKey(){
        nVector.rotateAbout(angle, uVector.x, uVector.y, uVector.z);
        vVector.rotateAbout(angle, uVector.x, uVector.y, uVector.z);
        cameraView();
    }
    public Vector4f getLoc() {return camera;}
    public Matrix4f getView(){ return viewMatrix;}

}
