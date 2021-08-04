package a4;

import com.jogamp.opengl.*;
import com.jogamp.common.nio.Buffers;
import org.joml.Vector3f;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.Random;
import static com.jogamp.opengl.GL4.*;

public class Water{
    private int noiseHeight = 256;
    private int noiseWidth = 256;
    private int noiseDepth = 256;
    private float surfaceLocX = 0f;
    private float surfaceLocY = 0f;
    private float surfaceLocZ = 0f;
    private int[] bufferId = new int[1];
    private int[] vbo = new int[4];
    private int reflectionTexture;
    private int refractionTexture;
    private int reflectionFrameBuffer;
    private int refractionFrameBuffer;
    private float size = 70f;

    private double[][][] noise = new double[noiseWidth][noiseHeight][noiseDepth];
    private Random random = new Random(5);
    private double PI = 3.1415926535;

    public Vector3f getPosition(){return new Vector3f(surfaceLocX, surfaceLocY, surfaceLocZ);}

    private double smooth(double zoom, double x1, double y1, double z1)
    {
        double fractionalX = x1 - (int) x1;
        double fractionalY = y1 - (int) y1;
        double fractionalZ = z1 - (int) z1;

        double x2 = x1 - 1; if (x2<0) x2 = ((int)(noiseHeight / zoom)) + x2;
        double y2 = y1 - 1; if (y2<0) y2 = ((int)(noiseWidth / zoom)) + y2;
        double z2 = z1 - 1; if (z2<0) z2 = ((int)(noiseDepth / zoom)) + z2;
        double value = 0.0;

        value += fractionalX     * fractionalY     * fractionalZ     * noise[(int)x1][(int)y1][(int)z1];
        value += fractionalX     * (1-fractionalY) * fractionalZ     * noise[(int)x1][(int)y2][(int)z1];
        value += (1-fractionalX) * fractionalY     * fractionalZ     * noise[(int)x2][(int)y1][(int)z1];
        value += (1-fractionalX) * (1-fractionalY) * fractionalZ     * noise[(int)x2][(int)y2][(int)z1];

        value += fractionalX     * fractionalY     * (1-fractionalZ) * noise[(int)x1][(int)y1][(int)z2];
        value += fractionalX     * (1-fractionalY) * (1-fractionalZ) * noise[(int)x1][(int)y2][(int)z2];
        value += (1-fractionalX) * fractionalY     * (1-fractionalZ) * noise[(int)x2][(int)y1][(int)z2];
        value += (1-fractionalX) * (1-fractionalY) * (1-fractionalZ) * noise[(int)x2][(int)y2][(int)z2];

        return value;
    }
    private void fillDataArray(byte data[])
    {	double maxZoom = 32.0;
        for (int i=0; i<noiseWidth; i++)
        {	for (int j=0; j<noiseHeight; j++)
        {	for (int k=0; k<noiseDepth; k++)
        {	noise[i][j][k] = random.nextDouble();
        }	}	}
        for (int i = 0; i<noiseHeight; i++)
        {	for (int j = 0; j<noiseWidth; j++)
        {	for (int k = 0; k<noiseDepth; k++)
        {	data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+0] = (byte)turbulence(i,j,k,maxZoom);
            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+1] = (byte)turbulence(i,j,k,maxZoom);
            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+2] = (byte)turbulence(i,j,k,maxZoom);
            data[i*(noiseWidth*noiseHeight*4)+j*(noiseHeight*4)+k*4+3] = (byte)255;
        }	}	}	}

    public int loadNoiseTexture()
    {	GL4 gl = (GL4) GLContext.getCurrentGL();

        byte[] data = new byte[noiseWidth*noiseHeight*noiseDepth*4];

        fillDataArray(data);

        ByteBuffer bb = Buffers.newDirectByteBuffer(data);

        int[] textureIDs = new int[1];
        gl.glGenTextures(1, textureIDs, 0);
        int textureID = textureIDs[0];

        gl.glBindTexture(GL_TEXTURE_3D, textureID);

        gl.glTexStorage3D(GL_TEXTURE_3D, 1, GL_RGBA8, noiseWidth, noiseHeight, noiseDepth);
        gl.glTexSubImage3D(GL_TEXTURE_3D, 0, 0, 0, 0,
                noiseWidth, noiseHeight, noiseDepth, GL_RGBA, GL_UNSIGNED_BYTE, bb);

        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_3D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        return textureID;
    }
    private double turbulence(double x, double y, double z, double maxZoom)
    {	double sum = 0.0, zoom = maxZoom;

        sum = (Math.sin((1.0/512.0)*(8*PI)*(x+z-4*y)) + 1) * 8.0;
        while(zoom >= 0.9)
        {	sum = sum + smooth(zoom, x/zoom, y/zoom, z/zoom) * zoom;
            zoom = zoom / 2.0;
        }
        sum = 128.0 * sum/maxZoom;
        return sum;
    }
    public void buffers(int canvasWidth, int canvasHeight) {
        GL4 gl = (GL4) GLContext.getCurrentGL();

        //Reflect Framebuffer
        gl.glGenFramebuffers(1, bufferId, 0);
        reflectionFrameBuffer = bufferId[0];
        gl.glBindFramebuffer(GL_FRAMEBUFFER, reflectionFrameBuffer);
        gl.glGenTextures(1, bufferId, 0);
        reflectionTexture = bufferId[0];
        gl.glBindTexture(GL_TEXTURE_2D, reflectionTexture);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, canvasWidth, canvasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, reflectionTexture, 0);
        gl.glDrawBuffer(GL_COLOR_ATTACHMENT0);
        gl.glGenTextures(1, bufferId, 0);
        gl.glBindTexture(GL_TEXTURE_2D, bufferId[0]);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, canvasWidth, canvasHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, bufferId[0], 0);

        //Refract Framebuffer
        gl.glGenFramebuffers(1, bufferId, 0);
        refractionFrameBuffer = bufferId[0];
        gl.glBindFramebuffer(GL_FRAMEBUFFER, refractionFrameBuffer);
        gl.glGenTextures(1, bufferId, 0);
        refractionTexture = bufferId[0];
        gl.glBindTexture(GL_TEXTURE_2D, refractionTexture);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, canvasWidth, canvasHeight, 0, GL_RGBA, GL_UNSIGNED_BYTE, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, refractionTexture, 0);
        gl.glDrawBuffer(GL_COLOR_ATTACHMENT0);
        gl.glGenTextures(1, bufferId, 0);
        gl.glBindTexture(GL_TEXTURE_2D, bufferId[0]);
        gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, canvasWidth, canvasHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        gl.glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, bufferId[0], 0);
    }

    public int getRefractionTexture(){return refractionTexture;}
    public int getRefractionFrameBuffer(){return refractionFrameBuffer;}

    public int getReflectionTexture(){return reflectionTexture;}
    public int getReflectionFrameBuffer(){return reflectionFrameBuffer;}

    public void setupVertices()
    {	GL4 gl = (GL4) GLContext.getCurrentGL();

        float[] pvalues = {
                -size, 0.0f, -size,  -size, 0.0f, size,  size, 0.0f, -size,
                size, 0.0f, -size,  -size, 0.0f, size,  size, 0.0f, size
        };
        float[] tvalues = {
                0.0f, 0.0f,  0.0f, 1.0f,  1.0f, 0.0f,
                1.0f, 0.0f,  0.0f, 1.0f,  1.0f, 1.0f
        };
        float[] nvalues = {
                0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,
                0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f,  0.0f, 1.0f, 0.0f
        };

        gl.glGenBuffers(vbo.length, vbo, 0);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
        FloatBuffer pBuf = Buffers.newDirectFloatBuffer(pvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, pBuf.limit()*4, pBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
        FloatBuffer texBuf = Buffers.newDirectFloatBuffer(tvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, texBuf.limit()*4, texBuf, GL_STATIC_DRAW);

        gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
        FloatBuffer norBuf = Buffers.newDirectFloatBuffer(nvalues);
        gl.glBufferData(GL_ARRAY_BUFFER, norBuf.limit()*4, norBuf, GL_STATIC_DRAW);
    }
    public int[] getVBO(){return vbo;}

}
