package a4;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLContext;
import graphicslib3D.Material;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.nio.FloatBuffer;
import static com.jogamp.opengl.GL.*;


//Used for managing all of the object files
public class ObjectManager {

    private ImportedModel model;

    private int texture;
    private float scale = 1.0f;
    private int mapType = -1;


    private Material material = new Material();

    private Vector4f position;

    private boolean transparant = false;
    private float alpha = 1.0f;

    private int[] vbo = new int[4];

    public ObjectManager(){}

    public ObjectManager(ImportedModel o, Vector4f pos){
        model = o;
        position = pos;
    }

    public ObjectManager(ImportedModel o, int tex, Material mat, Vector4f pos){
        model = o;
        material = mat;
        position = pos;
        texture = tex;
    }

    public void setLighting(int renderingProgram){
        GL4 gl = (GL4) GLContext.getCurrentGL();

        int mambLoc = gl.glGetUniformLocation(renderingProgram, "material.ambient");
        int mdiffLoc = gl.glGetUniformLocation(renderingProgram, "material.diffuse");
        int mspecLoc = gl.glGetUniformLocation(renderingProgram, "material.specular");
        int mshiLoc = gl.glGetUniformLocation(renderingProgram, "material.shininess");

        gl.glProgramUniform4fv(renderingProgram, mambLoc, 1, material.getAmbient(), 0);
        gl.glProgramUniform4fv(renderingProgram, mdiffLoc, 1, material.getDiffuse(), 0);
        gl.glProgramUniform4fv(renderingProgram, mspecLoc, 1, material.getSpecular(), 0);
        gl.glProgramUniform1f(renderingProgram, mshiLoc, material.getShininess());
    }

    public void setTransparant(float t){transparant = true; alpha = t;}
    public boolean isTransparant(){return transparant;}
    public float getAlpha(){return alpha;}

    public int getNumVerts(){return model.getNumVertices(); }
    public Vector4f getPosition(){return position;}

    public void setupVertices(){
        GL4 gl = (GL4) GLContext.getCurrentGL();
        int numVertices = model.getNumVertices();
        Vector3f[] vertices = model.getVertices();
        Vector2f[] texCoords = model.getTexCoords();
        Vector3f[] normals = model.getNormals();

        float[] pvalues = new float[numVertices*3];
        float[] tvalues = new float[numVertices*2];
        float[] nvalues = new float[numVertices*3];

        for (int i=0; i<numVertices; i++)
        {	pvalues[i*3]   = (float) (vertices[i]).x();
            pvalues[i*3+1] = (float) (vertices[i]).y();
            pvalues[i*3+2] = (float) (vertices[i]).z();
            tvalues[i*2]   = (float) (texCoords[i]).x();
            tvalues[i*2+1] = (float) (texCoords[i]).y();
            nvalues[i*3]   = (float) (normals[i]).x();
            nvalues[i*3+1] = (float) (normals[i]).y();
            nvalues[i*3+2] = (float) (normals[i]).z();

        }

        gl.glGenBuffers(vbo.length, vbo, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
        FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4,norBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, vbo[3]);
        gl.glBufferData(GL_ELEMENT_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

    }

    public void applyMapping(int t){mapType = t;}
    public int mapType(){return mapType;}

    public Vector3f[] getVerts(){return model.getVertices();}
    public Vector2f[] getTexCoords(){return model.getTexCoords();}
    public Vector3f[] getNormals(){return model.getNormals();}


    public int[] getVBO(){
        return vbo;
    }

    public void setPosition(Vector4f newPos){
        position = newPos;
    }

    public int getTexture(){
        return texture;
    }

    public void setScale(float s) {
        scale = s;
    }
    public float getScale() {
        return scale;
    }

   public void setMat(Material mat){
        material = mat;
    }

    public Material getMaterial(){return material;}
}
