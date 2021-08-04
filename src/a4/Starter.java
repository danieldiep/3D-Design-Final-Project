package a4;

import javax.swing.*;
import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.*;
import graphicslib3D.Material;
import org.joml.*;
import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.Math;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import static com.jogamp.opengl.GL4.*;

public class Starter extends JFrame implements GLEventListener, MouseWheelListener {

	private GLCanvas myCanvas;
	private GL4 gl;
	private int[] vao = new int[1];
	private int[] vboSkyBox = new int[2];
	private int [] shadow_tex = new int[1];
	private int [] shadow_buffer = new int[1];
	private FloatBuffer vals = Buffers.newDirectFloatBuffer(16);
	private Matrix4f mvMat = new Matrix4f();
	private Matrix4f vMat = new Matrix4f();
	private Matrix4f mMat = new Matrix4f();
	private Matrix4f pMat = new Matrix4f();
	private Matrix4f mvpMat = new Matrix4f();
	private Matrix4f invTrMat = new Matrix4f();
	private Matrix4f lightVmat = new Matrix4f();
	private Matrix4f lightPmat = new Matrix4f();
	private Matrix4f shadowMVP1 = new Matrix4f();
	private Matrix4f shadowMVP2 = new Matrix4f();
	private Matrix4f b = new Matrix4f();
	private Vector3f origin = new Vector3f(0.0f, 0.0f, 0.0f);
	private Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);

	private Water water = new Water();
	private CameraControl camera;
	private Cube cube = new Cube();
	private float fogAmount = 0.01f;
	private int screenSizeX, screenSizeY;
	private boolean fogToggle = false;
	private boolean worldAxesToggle = false;

	private ArrayList<ObjectManager> objectManagers = new ArrayList<>();
	private ObjectManager terrainObject;

	private MovableLight movableLight;
	private boolean controlMobileLight = false;

	private int mvLoc;
	private int projLoc;
	private int nLoc;
	private int sLoc;
	private int vLoc;
	private int mvpLoc;
	private float aspect;

	private int shadowProgram;
	private int renderingProgram;
	private int skyBoxProgram;
	private int worldAxesProgram;
	private int terrainProgram;
	private int waterProgram;
	private int waterFloorProgram;
	private int geometryProgram;


	private int skyboxTexture;
	private int noiseTexture;
	private int terrainHeightTexture;
	private int terrainNormalTexture;
	private int tealTexture;
	private int dolphinTexture;
	private int giraffeTexture;
	private int leafTexture;
	private int leafTexture2;
	private int barkTexture;
	private int floorTexture;

	private Material defaultMaterial;
	private Material greenMaterial;
	private Material goldMaterial;

	private float depthLookup;
	private int depthOffset;
	private long lastTime = System.currentTimeMillis();
	private Light mainLight;
	private Light activeLight;

	public Starter() {
		setTitle("CSC155 - Assignment 4");
		setSize(1000, 1000);
		camera = new CameraControl(5f, 10.0f, 40.0f);

		myCanvas = new GLCanvas();
		myCanvas.addGLEventListener(this);


		this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

		defaultMaterial = new Material();
		defaultMaterial.setAmbient(Material.SILVER.getAmbient());
		defaultMaterial.setDiffuse(Material.SILVER.getDiffuse());
		defaultMaterial.setSpecular(Material.SILVER.getSpecular());
		defaultMaterial.setShininess(Material.SILVER.getShininess());

		goldMaterial = new Material();
		goldMaterial.setAmbient(Material.GOLD.getAmbient());
		goldMaterial.setDiffuse(Material.GOLD.getDiffuse());
		goldMaterial.setSpecular(Material.GOLD.getSpecular());
		goldMaterial.setShininess(Material.GOLD.getShininess());

		greenMaterial = new Material();
		float[] grassAmbient = new float[] {0.1f, 0.35f, 0.1f, 1};
		float[] grassDiffuse = new float[] {0.1f, 0.35f, 0.1f, 1};
		float[] grassSpecular = new float[] {0.45f, 0.55f, .45f, 1};
		greenMaterial.setAmbient(grassAmbient );
		greenMaterial.setDiffuse(grassDiffuse);
		greenMaterial.setSpecular(grassSpecular);
		greenMaterial.setShininess(90);

		this.setLayout(new BorderLayout());
		this.add(myCanvas, BorderLayout.CENTER);
		setupKeyBindings();

		this.setVisible(true);
		Animator animator = new Animator(myCanvas);
		animator.start();
	}
	public void display(GLAutoDrawable drawable) {
		gl = (GL4) GLContext.getCurrentGL();
		gl.glClear(GL_COLOR_BUFFER_BIT);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		vMat = camera.getView();

		renderEnvironment();


		gl.glBindFramebuffer(GL_FRAMEBUFFER, shadow_buffer[0]);
		gl.glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadow_tex[0], 0);


		gl.glDrawBuffer(GL_NONE);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_POLYGON_OFFSET_FILL);
		gl.glPolygonOffset(3.0f, 5.0f);

		lightVmat.identity().setLookAt(activeLight.getLightPosition(), origin, up);
		lightPmat.identity().setPerspective((float) Math.toRadians(120.0f), aspect, 0.1f, 1000.0f);
		shadowPass();

		gl = (GL4) GLContext.getCurrentGL();

		gl.glDisable(GL_POLYGON_OFFSET_FILL);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);

		gl.glDrawBuffer(GL_FRONT);

		gl.glClear(GL_DEPTH_BUFFER_BIT);

		generateTerrain();
		renderWaterAndFloor();
		mainPass();

		if(worldAxesToggle){
			worldAxes();
		}
	}

	// renders skybox and floorPrep for water
	private void renderEnvironment(){
		Vector4f cameraPos = camera.getLoc();
		Vector3f waterPos = water.getPosition();

		long currentTime = System.currentTimeMillis();
		long elapsedTime = currentTime - lastTime;
		lastTime = currentTime;

		depthLookup += (float)elapsedTime * .0001f;

		//renderer for the refraction scene into the buffer
		if (camera.getLoc().y() > water.getPosition().y()) {
			gl.glBindFramebuffer(GL_FRAMEBUFFER, water.getReflectionFrameBuffer());
			gl.glClear(GL_DEPTH_BUFFER_BIT);
			gl.glClear(GL_COLOR_BUFFER_BIT);
			renderSkyBoxPrep();
			gl.glEnable(GL_CULL_FACE);
			gl.glFrontFace(GL_CCW);
			gl.glDisable(GL_DEPTH_TEST);
			gl.glDrawArrays(GL_TRIANGLES, 0, 36);
			gl.glEnable(GL_DEPTH_TEST);
		}
		gl.glBindFramebuffer(GL_FRAMEBUFFER, water.getRefractionFrameBuffer());
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);

		renderSkyBoxPrep();
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);

		renderFloorPrep();
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);

		gl.glBindFramebuffer(GL_FRAMEBUFFER, 0);
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);

		// draw cube map
		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL_COLOR_BUFFER_BIT);
		renderSkyBoxPrep();
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glDisable(GL_DEPTH_TEST);
		gl.glDrawArrays(GL_TRIANGLES, 0, 36);
		gl.glEnable(GL_DEPTH_TEST);
	}

	private void shadowPass(){
		gl = (GL4) GLContext.getCurrentGL();
		sLoc = gl.glGetUniformLocation(shadowProgram, "shadowMVP");
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(shadowProgram);

		drawSceneObjectShadow(activeLight.getLightObject());
		for (ObjectManager objectManager : objectManagers) {
			drawSceneObjectShadow(objectManager);
		}
	}

	private void drawSceneObjectShadow(ObjectManager object){
		gl = (GL4) GLContext.getCurrentGL();

		mMat.identity();
		mMat.translate(object.getPosition().x, object.getPosition().y, object.getPosition().z);
		mMat.scale(object.getScale());

		shadowMVP1.identity();
		shadowMVP1.mul(lightPmat);
		shadowMVP1.mul(lightVmat);
		shadowMVP1.mul(mMat);

		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP1.get(vals));

		int[] vbo = object.getVBO();


		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glDrawArrays(GL_TRIANGLES, 0, object.getNumVerts());
	}

	private void mainPass(){

		vMat = camera.getView();

		for (ObjectManager objectManager : objectManagers) {
			drawSceneObject(objectManager, renderingProgram);
		}
		drawSceneObject(activeLight.getLightObject(), geometryProgram);
	}

	private void generateTerrain(){
		gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(terrainProgram);

		mvpLoc = gl.glGetUniformLocation(terrainProgram, "mvp");
		mvLoc = gl.glGetUniformLocation(terrainProgram, "mv_matrix");
		projLoc = gl.glGetUniformLocation(terrainProgram, "proj_matrix");
		nLoc = gl.glGetUniformLocation(terrainProgram, "norm_matrix");
		sLoc = gl.glGetUniformLocation(terrainProgram, "shadowMVP");
		int fog = gl.glGetUniformLocation(terrainProgram, "fogAmount");
		int aboveLocation = gl.glGetUniformLocation(terrainProgram, "isAbove");

		mMat.identity();
		mMat.translate(terrainObject.getPosition().x, terrainObject.getPosition().y, terrainObject.getPosition().z);
		float scale = terrainObject.getScale();
		mMat.scale(scale, scale*10.0f, scale);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		mvpMat.identity();
		mvpMat.mul(pMat);
		mvpMat.mul(vMat);
		mvpMat.mul(mMat);

		activeLight.installLights(terrainProgram, vMat, terrainObject.getMaterial());

		gl.glUniformMatrix4fv(mvpLoc, 1, false, mvpMat.get(vals));
		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		gl.glUniform1f(fog, fogAmount);

		if (camera.getLoc().y() > water.getPosition().y())
			gl.glUniform1i(aboveLocation, 1);
		else {
			gl.glUniform1i(aboveLocation, 0);
		}

		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, terrainHeightTexture);
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_2D, terrainNormalTexture);

		gl.glClear(GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CW);

		gl.glPatchParameteri(GL_PATCH_VERTICES, 4);
		gl.glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
		gl.glDrawArraysInstanced(GL_PATCHES, 0, 4, 64*64);
	}
	// Draws a SceneObject using its position and scale
	private void drawSceneObject(ObjectManager object, int program){

		gl.glUseProgram(program);

		mvLoc = gl.glGetUniformLocation(program, "mv_matrix");
		projLoc = gl.glGetUniformLocation(program, "proj_matrix");
		nLoc = gl.glGetUniformLocation(program, "norm_matrix");
		sLoc = gl.glGetUniformLocation(program, "shadowMVP");

		mMat.identity();
		mMat.translate(object.getPosition().x, object.getPosition().y, object.getPosition().z);
		mMat.scale(object.getScale());

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		activeLight.installLights(program, vMat, object.getMaterial());

		gl = (GL4) GLContext.getCurrentGL();
		int[] vbo = object.getVBO();


		int fog = gl.glGetUniformLocation(program, "fogAmount");
		int objectOnTop = gl.glGetUniformLocation(program, "isAbove");

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		int alphaLoc = gl.glGetUniformLocation(program, "alpha");
		int flipLoc = gl.glGetUniformLocation(program, "flipNormal");

		gl.glProgramUniform1f(program, alphaLoc, 1.0f);
		gl.glProgramUniform1f(program, flipLoc, 1.0f);
		gl.glUniform1f(fog, fogAmount);

		if (camera.getLoc().y() > water.getPosition().y())
			gl.glUniform1i(objectOnTop, 1);
		else {
			gl.glUniform1i(objectOnTop, 0);
		}

		int toMap = object.mapType();
		int mapLoc = gl.glGetUniformLocation(program, "map");
		gl.glUniform1i(mapLoc, toMap);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		bindTexture(object.getTexture());
		gl.glEnable(GL_CULL_FACE);
		gl.glFrontFace(GL_CCW);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		if(object.isTransparant()){
			gl.glEnable(GL_BLEND);
			gl.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
			gl.glBlendEquation(GL_FUNC_ADD);

			gl.glEnable(GL_CULL_FACE);

			gl.glCullFace(GL_FRONT);
			gl.glProgramUniform1f(program, alphaLoc, object.getAlpha());
			gl.glProgramUniform1f(program, flipLoc, -1.0f);
			gl.glDrawArrays(GL_TRIANGLES, 0, object.getNumVerts());

			float alphaBack = object.getAlpha() * 1.5f;
			if( alphaBack >= 1.0f){alphaBack = 1.0f - (object.getAlpha() / 2);}
			gl.glCullFace(GL_BACK);
			gl.glProgramUniform1f(program, alphaLoc, alphaBack);
			gl.glProgramUniform1f(program, flipLoc, 1.0f);
			gl.glDrawArrays(GL_TRIANGLES, 0, object.getNumVerts());

			gl.glDisable(GL_BLEND);
		}

		else{gl.glDrawArrays(GL_TRIANGLES, 0, object.getNumVerts());}

	}

	private void renderSkyBoxPrep() {
		gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(skyBoxProgram);

		vLoc = gl.glGetUniformLocation(skyBoxProgram, "v_matrix");
		projLoc = gl.glGetUniformLocation(skyBoxProgram, "p_matrix");
		int aboveLoc = gl.glGetUniformLocation(skyBoxProgram, "isAbove");
		int fogLocation = gl.glGetUniformLocation(skyBoxProgram, "fogAmount");

		gl.glUniformMatrix4fv(vLoc, 1, false, vMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniform1f(fogLocation, fogAmount);

		if (camera.getLoc().y() > water.getPosition().y())
			gl.glUniform1i(aboveLoc, 1);
		else
			gl.glUniform1i(aboveLoc, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vboSkyBox[0]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_CUBE_MAP, skyboxTexture);
	}

	private void renderWaterAndFloor(){
		renderWater();

		renderFloorPrep();
		gl.glActiveTexture(GL_TEXTURE0);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);
		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
	}

	// renders the water over every other object in the scene
	private void renderWater(){
		gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(waterProgram);

		mvLoc = gl.glGetUniformLocation(waterProgram, "mv_matrix");
		projLoc = gl.glGetUniformLocation(waterProgram, "proj_matrix");
		nLoc = gl.glGetUniformLocation(waterProgram, "norm_matrix");
		sLoc = gl.glGetUniformLocation(waterProgram, "shadowMVP");
		int aboveLoc = gl.glGetUniformLocation(waterProgram, "isAbove");
		int dOffsetLoc = gl.glGetUniformLocation(waterProgram, "depthOffset");
		int fog = gl.glGetUniformLocation(waterProgram, "fogAmount");

		Vector3f waterPos = water.getPosition();

		mMat.translation(waterPos.x(), waterPos.y(), waterPos.z());

		vMat = camera.getView();

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		shadowMVP2.identity();
		shadowMVP2.mul(b);
		shadowMVP2.mul(lightPmat);
		shadowMVP2.mul(lightVmat);
		shadowMVP2.mul(mMat);

		activeLight.installLights(waterProgram, vMat, defaultMaterial);


		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniformMatrix4fv(sLoc, 1, false, shadowMVP2.get(vals));
		gl.glUniform1f(fog, fogAmount);

		if (camera.getLoc().y() > waterPos.y())
			gl.glUniform1i(aboveLoc, 1);
		else {
			gl.glUniform1i(aboveLoc, 0);
		}
		gl.glUniform1f(dOffsetLoc, depthLookup);

		int[] vbo = water.getVBO();

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);

		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, water.getReflectionTexture());
		gl.glActiveTexture(GL_TEXTURE2);
		gl.glBindTexture(GL_TEXTURE_2D, water.getRefractionTexture());
		gl.glActiveTexture(GL_TEXTURE3);
		gl.glBindTexture(GL_TEXTURE_3D, noiseTexture);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		gl.glFrontFace(GL_CW);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
		gl.glFrontFace(GL_CCW);
		gl.glDrawArrays(GL_TRIANGLES, 0, 6);
	}

	private void renderFloorPrep()
	{	GL4 gl = (GL4) GLContext.getCurrentGL();

		gl.glUseProgram(waterFloorProgram);

		mvLoc = gl.glGetUniformLocation(waterFloorProgram, "mv_matrix");
		projLoc = gl.glGetUniformLocation(waterFloorProgram, "proj_matrix");
		nLoc = gl.glGetUniformLocation(waterFloorProgram, "norm_matrix");
		int aboveLocation = gl.glGetUniformLocation(waterFloorProgram, "isAbove");
		int fogLocation = gl.glGetUniformLocation(waterFloorProgram, "fogAmount");
		depthOffset = gl.glGetUniformLocation(waterFloorProgram, "depthOffset");

		mMat.translation(terrainObject.getPosition().x, terrainObject.getPosition().y, terrainObject.getPosition().z);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);
		mvMat.invert(invTrMat);
		invTrMat.transpose(invTrMat);

		activeLight.installLights(waterFloorProgram, vMat, terrainObject.getMaterial());

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glUniformMatrix4fv(nLoc, 1, false, invTrMat.get(vals));
		gl.glUniform1f(fogLocation, fogAmount);

		if (camera.getLoc().y > water.getPosition().y)
			gl.glUniform1i(aboveLocation, 1);
		else
			gl.glUniform1i(aboveLocation, 0);

		gl.glUniform1f(depthOffset, depthLookup);

		int[] vbo = water.getVBO();
		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[1]);
		gl.glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[2]);
		gl.glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(1);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[3]);
		gl.glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
		gl.glEnableVertexAttribArray(2);
	}
	private void worldAxes(){
		gl = (GL4) GLContext.getCurrentGL();
		gl = (GL4) GLContext.getCurrentGL();
		gl.glUseProgram(worldAxesProgram);

		mvLoc = gl.glGetUniformLocation(worldAxesProgram, "mv_matrix");
		projLoc = gl.glGetUniformLocation(worldAxesProgram, "proj_matrix");

		mMat.identity();
		mMat.translate(0.0f, 3f, 0.0f);

		mvMat.identity();
		mvMat.mul(vMat);
		mvMat.mul(mMat);

		gl.glUniformMatrix4fv(mvLoc, 1, false, mvMat.get(vals));
		gl.glUniformMatrix4fv(projLoc, 1, false, pMat.get(vals));
		gl.glDrawArrays(GL_LINES, 0, 6);
	}

	public void init(GLAutoDrawable drawable) {
		gl = (GL4) GLContext.getCurrentGL();
		aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		pMat.identity().setPerspective((float) Math.toRadians(60.0f), aspect, 0.1f, 1000.0f);

		renderingProgram = Utils.createShaderProgram("\\shaders\\vertexShader.glsl", "\\shaders\\fragmentShader.glsl");
		shadowProgram = Utils.createShaderProgram("\\shaders\\shadowVertexShader.glsl", "\\shaders\\shadowFragmentShader.glsl");
		skyBoxProgram = Utils.createShaderProgram("\\shaders\\skyBoxVertexShader.glsl", "\\shaders\\skyBoxFragmentShader.glsl");
		worldAxesProgram = Utils.createShaderProgram("\\shaders\\worldAxesVertexShader.glsl", "\\shaders\\worldAxesFragmentShader.glsl");
		terrainProgram = Utils.createShaderProgram("\\shaders\\terrainVertexShader.glsl", "\\shaders\\tessCShader.glsl", "\\shaders\\tessEShader.glsl", "\\shaders\\terrainFragmentShader.glsl");
		waterProgram = Utils.createShaderProgram("\\shaders\\waterVertexShader.glsl", "\\shaders\\waterFragmentShader.glsl");
		waterFloorProgram = Utils.createShaderProgram("\\shaders\\floorNormalVertexShader.glsl", "\\shaders\\floorNormalFragmentShader.glsl");
		geometryProgram = Utils.createShaderProgram("\\shaders\\geomVertexShader.glsl", "\\shaders\\geomShader.glsl", "\\shaders\\geomFragmentShader.glsl");

		tealTexture = Utils.loadTexture("\\textures\\teal.jpg");
		skyboxTexture = Utils.loadCubeMap("cubeMap");
		terrainHeightTexture = Utils.loadTexture("\\textures\\height1.png");
		terrainNormalTexture = Utils.loadTexture("\\textures\\bump1.jpg");
		barkTexture = Utils.loadTexture("\\textures\\bark.jpg");
		floorTexture = Utils.loadTexture("\\textures\\terrainFloor.jpg");
		dolphinTexture = Utils.loadTexture("\\textures\\dolphin.png");
		giraffeTexture = Utils.loadTexture("\\textures\\giraffe.jpg");
		leafTexture = Utils.loadTexture("\\textures\\leaf.jpg");
		leafTexture2 = Utils.loadTexture("\\textures\\autumn.jpg");

		b.set(
				0.5f, 0.0f, 0.0f, 0.0f,
				0.0f, 0.5f, 0.0f, 0.0f,
				0.0f, 0.0f, 0.5f, 0.0f,
				0.5f, 0.5f, 0.5f, 1.0f);

		setupVertices();
		gl.glEnable(GL_TEXTURE_CUBE_MAP_SEAMLESS);

		//The Sphere is transparent
		ImportedModel temp = new ImportedModel("\\objects\\sphere.obj");
		mainLight = new Light(new ObjectManager(temp, tealTexture, defaultMaterial, new Vector4f(1.5f, 10f, 1.5f, 1f)));
		mainLight.getLightObject().setTransparant(0.1f);
		activeLight = mainLight;
		movableLight = new MovableLight(camera.getView(), new ObjectManager(temp, tealTexture, defaultMaterial, new Vector4f(1.5f, 10f, 1.5f, 1f)));
		myCanvas.addMouseMotionListener(movableLight);
		myCanvas.addMouseWheelListener(movableLight);
		myCanvas.addMouseWheelListener(this);
		movableLight.setWindowSize(myCanvas.getWidth(), myCanvas.getHeight());
	}

	private void addNewSceneObject(ImportedModel obj, Vector4f location, float scale, int texture, Material mat){
		ObjectManager object = new ObjectManager(obj, texture, mat, location);
		object.setScale(scale);
		objectManagers.add(object);
	}

	private void setupVertices() {
		gl = (GL4) GLContext.getCurrentGL();
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		water.setupVertices();

		water.buffers(myCanvas.getWidth(), myCanvas.getHeight());

		noiseTexture = water.loadNoiseTexture();

		ImportedModel terrain = new ImportedModel("\\objects\\terrain.obj");
		ImportedModel giraffeModel = new ImportedModel("\\objects\\giraffe.obj");
		ImportedModel dolphinModel = new ImportedModel("\\objects\\dolphin.obj");
		ImportedModel tree = new ImportedModel("\\objects\\tree.obj");
		ImportedModel leaf = new ImportedModel("\\objects\\leaf.obj");
		ImportedModel tree2 = new ImportedModel("\\objects\\tree2.obj");
		ImportedModel leaf2 = new ImportedModel("\\objects\\leaf2.obj");
		ImportedModel tree3 = new ImportedModel("\\objects\\tree3.obj");
		ImportedModel leaf3 = new ImportedModel("\\objects\\leaf3.obj");

		terrainObject = new ObjectManager(terrain, floorTexture, defaultMaterial, new Vector4f(0, -3.8f, 0, 1.0f));
		terrainObject.setScale(48f);

		treeObject(tree, leaf, new Vector4f(8f, 1.3f, 0, 1f), 1f);
		treeObject(tree, leaf, new Vector4f(-5f, 1.8f, 6f, 1f), 1.3f);
		treeObject(tree, leaf, new Vector4f(2f, 3.8f, -2f, 1f), 1f);

		treeObject2(tree2, leaf2, new Vector4f(-2f, 3f, 8f, 1f), 1.5f);
		treeObject2(tree2, leaf2, new Vector4f(-6f, 1f, 9f, 1f), 1.5f);
		treeObject2(tree2, leaf2, new Vector4f(14f, 1f, 2f, 1f), 1.5f);
		treeObject2(tree3, leaf3, new Vector4f(12f, 0.7f, 11f, 1f), 2.2f);
		treeObject2(tree3, leaf3, new Vector4f(-4f, 0.7f, 18f, 1f), 2.2f);

		dolphinObject(dolphinModel, new Vector4f(18f, 1f, 6.0f, 1f), 4f);
		dolphinObject(dolphinModel, new Vector4f(20f, 2f, 7.0f, 1f), 4f);
		dolphinObject(dolphinModel, new Vector4f(22f, 1f, 8.0f, 1f), 4f);

		giraffeObject(giraffeModel, new Vector4f(3f, 1.6f, 12.5f, 1.0f), 2f);

		for(int i = 0; i < objectManagers.size(); i++){
			objectManagers.get(i).setupVertices();
		}
		setupSkyBox();
	}
	private void treeObject(ImportedModel tree, ImportedModel leaf, Vector4f location, float scale){
		addNewSceneObject(tree, location, scale, barkTexture, defaultMaterial);
		objectManagers.get(objectManagers.size() - 1).applyMapping(0);
		addNewSceneObject(leaf, location, scale, leafTexture, greenMaterial);
	}
	private void treeObject2(ImportedModel tree, ImportedModel leaf, Vector4f location, float scale){
		addNewSceneObject(tree, location, scale, barkTexture, defaultMaterial);
		objectManagers.get(objectManagers.size() - 1).applyMapping(0);
		addNewSceneObject(leaf, location, scale, leafTexture2, goldMaterial);
	}
	private void dolphinObject(ImportedModel animal, Vector4f location, float scale) {
		addNewSceneObject(animal, location, scale, dolphinTexture, defaultMaterial);
		objectManagers.get(objectManagers.size() - 1).applyMapping(0);
	}
	private void giraffeObject(ImportedModel animal, Vector4f location, float scale) {
		addNewSceneObject(animal, location, scale, giraffeTexture, goldMaterial);
		objectManagers.get(objectManagers.size() - 1).applyMapping(0);
	}
	private void setupShadowBuffers()
	{
		GL4 gl = (GL4) GLContext.getCurrentGL();
		screenSizeX = myCanvas.getWidth();
		screenSizeY = myCanvas.getHeight();

		gl.glGenFramebuffers(1, shadow_buffer, 0);

		gl.glGenTextures(1, shadow_tex, 0);
		gl.glBindTexture(GL_TEXTURE_2D, shadow_tex[0]);
		gl.glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT32, screenSizeX, screenSizeY, 0, GL_DEPTH_COMPONENT, GL_FLOAT, null);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_MODE, GL_COMPARE_REF_TO_TEXTURE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_COMPARE_FUNC, GL_LEQUAL);

		// may reduce shadow border artifacts
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
	}

	private void setupSkyBox(){
		gl.glGenBuffers(vboSkyBox.length, vboSkyBox, 0);
		gl.glBindBuffer(GL_ARRAY_BUFFER, vboSkyBox[0]);
		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(cube.getPositions());
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);
	}

	private void bindTexture(int tex){
		gl.glActiveTexture(GL_TEXTURE1);
		gl.glBindTexture(GL_TEXTURE_2D, tex);

		//mip-mapping
		gl.glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
		gl.glGenerateMipmap(GL_TEXTURE_2D);

		// Anisotropic filtering
		if(gl.isExtensionAvailable("GL_EXT_texture_filter_anisotropic")){
			float[] anisoSetting = new float[1];
			gl.glGetFloatv(GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT, anisoSetting, 0);
			gl.glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_MAX_ANISOTROPY_EXT, anisoSetting[0]);
		}
	}
	public static void main(String[] args) { new Starter(); }
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

		setupShadowBuffers();
	}
	public void dispose(GLAutoDrawable drawable) {}

	private void setupKeyBindings(){
		JPanel contentPane = (JPanel) this.getContentPane();

		int mapName = JComponent.WHEN_IN_FOCUSED_WINDOW;
		InputMap imap = contentPane.getInputMap(mapName);
		ActionMap amap = contentPane.getActionMap();

		KeyStroke w = KeyStroke.getKeyStroke('w');
		imap.put(w, "Forward");
		amap.put("Forward", new ForwardKey(camera));

		KeyStroke s = KeyStroke.getKeyStroke('s');
		imap.put(s, "Backward");
		amap.put("Backward", new BackwardKey(camera));

		KeyStroke a = KeyStroke.getKeyStroke('a');
		imap.put(a, "Left");
		amap.put("Left", new LeftKey(camera));

		KeyStroke d = KeyStroke.getKeyStroke('d');
		imap.put(d, "Right");
		amap.put("Right", new RightKey(camera));

		KeyStroke e = KeyStroke.getKeyStroke('e');
		imap.put(e, "Down");
		amap.put("Down", new DownKey(camera));

		KeyStroke q = KeyStroke.getKeyStroke('q');
		imap.put(q, "Up");
		amap.put("Up", new UpKey(camera));

		KeyStroke panLeft = KeyStroke.getKeyStroke("LEFT");
		imap.put(panLeft, "PanLeft");
		amap.put("PanLeft", new PanLeftKey(camera));

		KeyStroke panRight = KeyStroke.getKeyStroke("RIGHT");
		imap.put(panRight, "PanRight");
		amap.put("PanRight", new PanRightKey(camera));

		KeyStroke pitchUp = KeyStroke.getKeyStroke("UP");
		imap.put(pitchUp, "PitchUp");
		amap.put("PitchUp", new PitchUpKey(camera));

		KeyStroke pitchDown = KeyStroke.getKeyStroke("DOWN");
		imap.put(pitchDown, "PitchDown");
		amap.put("PitchDown", new PitchDownKey(camera));

		KeyStroke worldAxes = KeyStroke.getKeyStroke("SPACE");
		imap.put(worldAxes, "Axes");
		amap.put("Axes", new WorldAxesKey(this));

		KeyStroke light = KeyStroke.getKeyStroke("G");
		imap.put(light, "Light");
		amap.put("Light", new LightKey(this));

		KeyStroke fogKey = KeyStroke.getKeyStroke("F");
		imap.put(fogKey, "Fog");
		amap.put("Fog", new FogKey(this));

	}
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		if(fogToggle){
			fogAmount += e.getWheelRotation() * 0.001f;
			if(fogAmount < 0){
				fogAmount = 0;
			}
		}
	}
	public void axesToggle(){
		worldAxesToggle = !worldAxesToggle;
	}

	public void fogToggle(){
		if(controlMobileLight){
			movableLightToggle();
		}
		fogToggle = !fogToggle;
	}
	public void movableLightToggle(){
		if(fogToggle) {
			fogToggle();
		}
		controlMobileLight = !controlMobileLight;
		movableLight.toggleMobileLight();
		if(controlMobileLight){activeLight = movableLight;}
		else {activeLight = mainLight;}
	}

}