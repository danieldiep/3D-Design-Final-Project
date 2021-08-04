package a4;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Scanner;
import java.util.Vector;
import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import static a4.ErrorHandling.*;
import static com.jogamp.opengl.GL4.*;

import javax.imageio.ImageIO;

//Util code from Book
public class Utils {

	public Utils() {}

	public static int createShaderProgram(String vS, String fS) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] vertCompiled = new int[1];
		int[] fragCompiled = new int[1];

		String[] vShaderSource = prepareShader(GL_VERTEX_SHADER, vS);
		String[] fShaderSource = prepareShader(GL_FRAGMENT_SHADER, fS);

		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		gl.glShaderSource(vShader, vShaderSource.length, vShaderSource, null, 0);
		gl.glCompileShader(vShader);

		checkOpenGLError();

		gl.glGetShaderiv(vShader, GL_COMPILE_STATUS, vertCompiled, 0);
		if (vertCompiled[0] != 1) {
			System.out.println("vertex shader compilation failed");
			printShaderLog(vShader);
		}

		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);
		gl.glShaderSource(fShader, fShaderSource.length, fShaderSource, null, 0);
		gl.glCompileShader(fShader);

		checkOpenGLError();  // can use returned boolean if desired
		gl.glGetShaderiv(fShader, GL_COMPILE_STATUS, fragCompiled, 0);
		if (fragCompiled[0] != 1) {
			System.out.println("fragment shader compilation failed");
			printShaderLog(fShader);
		}


		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		finalizeProgram(vfprogram);

		gl.glDeleteShader(vShader);
		gl.glDeleteShader(fShader);

		return vfprogram;
	}

	public static int createShaderProgram(String vS, String tCS, String tES, String fS) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int vShader  = prepareShader2(GL_VERTEX_SHADER, vS);
		int tcShader = prepareShader2(GL_TESS_CONTROL_SHADER, tCS);
		int teShader = prepareShader2(GL_TESS_EVALUATION_SHADER, tES);
		int fShader  = prepareShader2(GL_FRAGMENT_SHADER, fS);
		int vtfprogram = gl.glCreateProgram();
		gl.glAttachShader(vtfprogram, vShader);
		gl.glAttachShader(vtfprogram, tcShader);
		gl.glAttachShader(vtfprogram, teShader);
		gl.glAttachShader(vtfprogram, fShader);
		finalizeProgram(vtfprogram);
		return vtfprogram;
	}

	// Program 2.6 from Book
	public static void finalizeProgram(int sprogram) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] linked = new int[1];
		gl.glLinkProgram(sprogram);
		checkOpenGLError();
		gl.glGetProgramiv(sprogram, GL_LINK_STATUS, linked, 0);
		if (linked[0] != 1) {
			System.out.println("linking failed");
			printProgramLog(sprogram);
		}
	}

	public static int createShaderProgram(String vS, String gS, String fS) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int vShader  = prepareShader2(GL_VERTEX_SHADER, vS);
		int gShader = prepareShader2(GL_GEOMETRY_SHADER, gS);
		int fShader  = prepareShader2(GL_FRAGMENT_SHADER, fS);
		int vgfprogram = gl.glCreateProgram();
		gl.glAttachShader(vgfprogram, vShader);
		gl.glAttachShader(vgfprogram, gShader);
		gl.glAttachShader(vgfprogram, fShader);
		finalizeProgram(vgfprogram);
		return vgfprogram;
	}

	//Program 2.6 from Book
	private static String[] prepareShader(int shaderTYPE, String shader) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] shaderCompiled = new int[1];
		String shaderSource[] = readShaderSource(shader);
		int shaderRef = gl.glCreateShader(shaderTYPE);
		gl.glShaderSource(shaderRef, shaderSource.length, shaderSource, null, 0);
		gl.glCompileShader(shaderRef);
		checkOpenGLError();
		gl.glGetShaderiv(shaderRef, GL_COMPILE_STATUS, shaderCompiled, 0);
		if (shaderCompiled[0] != 1) {
			if (shaderTYPE == GL_VERTEX_SHADER) System.out.print("Vertex ");
			if (shaderTYPE == GL_TESS_CONTROL_SHADER) System.out.print("Tess Control ");
			if (shaderTYPE == GL_TESS_EVALUATION_SHADER) System.out.print("Tess Eval ");
			if (shaderTYPE == GL_GEOMETRY_SHADER) System.out.print("Geometry ");
			if (shaderTYPE == GL_FRAGMENT_SHADER) System.out.print("Fragment ");
			System.out.println("shader compilation error.");
			printShaderLog(shaderRef);
		}
		return shaderSource;
	}

	private static int prepareShader2(int shaderTYPE, String shader) {

		GL4 gl = (GL4) GLContext.getCurrentGL();
		int[] shaderCompiled = new int[1];
		String shaderSource[] = readShaderSource(shader);
		int shaderRef = gl.glCreateShader(shaderTYPE);
		gl.glShaderSource(shaderRef, shaderSource.length, shaderSource, null, 0);
		gl.glCompileShader(shaderRef);
		checkOpenGLError();
		gl.glGetShaderiv(shaderRef, GL_COMPILE_STATUS, shaderCompiled, 0);
		if (shaderCompiled[0] != 1)
		{	if (shaderTYPE == GL_VERTEX_SHADER) System.out.print("Vertex ");
			if (shaderTYPE == GL_TESS_CONTROL_SHADER) System.out.print("Tess Control ");
			if (shaderTYPE == GL_TESS_EVALUATION_SHADER) System.out.print("Tess Eval ");
			if (shaderTYPE == GL_GEOMETRY_SHADER) System.out.print("Geometry ");
			if (shaderTYPE == GL_FRAGMENT_SHADER) System.out.print("Fragment ");
			System.out.println("shader compilation error.");
			printShaderLog(shaderRef);
		}
		return shaderRef;
	}

	// Program 2.6 from Book
	private static String[] readShaderSource(String filename) {
		Vector<String> lines = new Vector<String>();
		Scanner sc;
		String[] program;
		try {
			String tmp = new File("").getAbsolutePath();
			filename = tmp + filename;
			sc = new Scanner(new File(filename));
			while (sc.hasNext()) {
				lines.addElement(sc.nextLine());
			}
			program = new String[lines.size()];
			for (int i = 0; i < lines.size(); i++) {
				program[i] = (String) lines.elementAt(i) + "\n";
			}
		}
		catch (IOException e) {
			System.err.println("IOException reading file: " + e);
			return null;
		}
		return program;
	}

	public static int loadTexture(String textureFileName) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		int finalTextureRef;
		Texture tex = null;
		String tmp = new File("").getAbsolutePath();
		textureFileName = tmp + textureFileName;
		try { tex = TextureIO.newTexture(new File(textureFileName), false); }
		catch (Exception e) { e.printStackTrace(); }
		finalTextureRef = tex.getTextureObject();

		//For mipmap and using of anisotropic filtering
		gl.glBindTexture(GL_TEXTURE_2D, finalTextureRef);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL.GL_TEXTURE_2D);
		if (gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic"))
		{	float anisoset[] = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, anisoset, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoset[0]);
		}
		return finalTextureRef;
	}


	public static int loadCubeMap(String dirName) {
		GL4 gl = (GL4) GLContext.getCurrentGL();

		String topFile = dirName + File.separator + "yp.png";
		String leftFile = dirName + File.separator + "xn.png";
		String backFile = dirName + File.separator + "zn.png";
		String rightFile = dirName + File.separator + "xp.png";
		String frontFile = dirName + File.separator + "zp.png";
		String bottomFile = dirName + File.separator + "yn.png";

		BufferedImage topImage = getBufferedImage(topFile);
		BufferedImage leftImage = getBufferedImage(leftFile);
		BufferedImage frontImage = getBufferedImage(frontFile);
		BufferedImage rightImage = getBufferedImage(rightFile);
		BufferedImage backImage = getBufferedImage(backFile);
		BufferedImage bottomImage = getBufferedImage(bottomFile);

		byte[] topRGBA = getRGBAPixelData(topImage, false);
		byte[] leftRGBA = getRGBAPixelData(leftImage, false);
		byte[] frontRGBA = getRGBAPixelData(frontImage, false);
		byte[] rightRGBA = getRGBAPixelData(rightImage, false);
		byte[] backRGBA = getRGBAPixelData(backImage, false);
		byte[] bottomRGBA = getRGBAPixelData(bottomImage, false);

		ByteBuffer topWrappedRGBA = ByteBuffer.wrap(topRGBA);
		ByteBuffer leftWrappedRGBA = ByteBuffer.wrap(leftRGBA);
		ByteBuffer frontWrappedRGBA = ByteBuffer.wrap(frontRGBA);
		ByteBuffer rightWrappedRGBA = ByteBuffer.wrap(rightRGBA);
		ByteBuffer backWrappedRGBA = ByteBuffer.wrap(backRGBA);
		ByteBuffer bottomWrappedRGBA = ByteBuffer.wrap(bottomRGBA);

		int[] textureIDs = new int[1];
		gl.glGenTextures(1, textureIDs, 0);
		int textureID = textureIDs[0];

		checkOpenGLError();

		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, textureID);
		gl.glTexStorage2D(GL_TEXTURE_CUBE_MAP, 1, GL_RGBA8, 1024, 1024);

		// attach the image texture to each face of the currently active OpenGL texture ID
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X, 0, 0, 0, 1024, 1024,
				GL_RGBA, GL.GL_UNSIGNED_BYTE, rightWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_X, 0, 0, 0, 1024, 1024,
				GL_RGBA, GL.GL_UNSIGNED_BYTE, leftWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Y, 0, 0, 0, 1024, 1024,
				GL_RGBA, GL.GL_UNSIGNED_BYTE, bottomWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Y, 0, 0, 0, 1024, 1024,
				GL_RGBA, GL.GL_UNSIGNED_BYTE, topWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_Z, 0, 0, 0, 1024, 1024,
				GL_RGBA, GL.GL_UNSIGNED_BYTE, frontWrappedRGBA);
		gl.glTexSubImage2D(GL_TEXTURE_CUBE_MAP_NEGATIVE_Z, 0, 0, 0, 1024, 1024,
				GL_RGBA, GL.GL_UNSIGNED_BYTE, backWrappedRGBA);

		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);

		checkOpenGLError();
		return textureID;
	}

	private static BufferedImage getBufferedImage(String fileName) {
		BufferedImage img;
		try {
			img = ImageIO.read(new File(fileName));	// assumes GIF, JPG, PNG, BMP
		} catch (IOException e) {
			System.err.println("Error reading '" + fileName + '"');
			throw new RuntimeException(e);
		}
		return img;
	}

	private static byte[] getRGBAPixelData(BufferedImage img, boolean flip) {
		int height = img.getHeight(null);
		int width = img.getWidth(null);

		WritableRaster raster = Raster.createInterleavedRaster(
				DataBuffer.TYPE_BYTE, width, height, 4, null);

		ComponentColorModel colorModel = new ComponentColorModel(
				ColorSpace.getInstance(ColorSpace.CS_sRGB), new int[] { 8, 8, 8, 8 }, // bits
				true,  // hasAlpha
				false,
				ComponentColorModel.TRANSLUCENT,
				DataBuffer.TYPE_BYTE);

		BufferedImage newImage = new BufferedImage(colorModel, raster, false, null);
		Graphics2D g = newImage.createGraphics();

		if (flip)	// flip image vertically
		{	AffineTransform gt = new AffineTransform();
			gt.translate(0, height);
			gt.scale(1, -1d);
			g.transform(gt);
		}
		g.drawImage(img, null, null);
		g.dispose();

		// now retrieve the underlying byte array from the raster data buffer
		DataBufferByte dataBuf = (DataBufferByte) raster.getDataBuffer();
		return dataBuf.getData();
	}

	// GOLD material - ambient, diffuse, specular, and shininess
	public static float[] goldAmbient()  { return (new float [] {0.2473f,  0.1995f, 0.0745f, 1} ); }
	public static float[] goldDiffuse()  { return (new float [] {0.7516f,  0.6065f, 0.2265f, 1} ); }
	public static float[] goldSpecular() { return (new float [] {0.6283f,  0.5559f, 0.3661f, 1} ); }
	public static float goldShininess()  { return 51.2f; }

	// SILVER material - ambient, diffuse, specular, and shininess
	public static float[] silverAmbient()  { return (new float [] {0.1923f,  0.1923f,  0.1923f, 1} ); }
	public static float[] silverDiffuse()  { return (new float [] {0.5075f,  0.5075f,  0.5075f, 1} ); }
	public static float[] silverSpecular() { return (new float [] {0.5083f,  0.5083f,  0.5083f, 1} ); }
	public static float silverShininess()  { return 51.2f; }


	// BRONZE material - ambient, diffuse, specular, and shininess
	public static float[] bronzeAmbient()  { return (new float [] {0.2125f,  0.1275f, 0.0540f, 1} ); }
	public static float[] bronzeDiffuse()  { return (new float [] {0.7140f,  0.4284f, 0.1814f, 1} ); }
	public static float[] bronzeSpecular() { return (new float [] {0.3936f,  0.2719f, 0.1667f, 1} ); }
	public static float bronzeShininess()  { return 25.6f; }
}