package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.FlatPlaneVAO;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.LineVAO;
import de.ecconia.java.opentung.libwrap.vaos.InvisibleCubeVAO;
import de.ecconia.java.opentung.libwrap.vaos.VisibleCubeVAO;
import de.ecconia.java.opentung.settings.Settings;
import org.lwjgl.opengl.GL30;

public class ShaderStorage
{
	//2D:
	private final ShaderProgram interfaceShader;
	private final ShaderProgram flatTextShader;
	
	private final ShaderProgram flatTextureShader;
	private final GenericVAO flatTexturePlane;
	
	private final ShaderProgram flatPlaneShader;
	private final FlatPlaneVAO flatPlane;
	
	private final Matrix interfaceProjectionMatrix;
	
	//3D:
	private final ShaderProgram lineShader;
	private final ShaderProgram invisibleCubeShader;
	private final ShaderProgram visibleCubeShader;
	private final ShaderProgram textureCubeShader;
	private final ShaderProgram sdfShader;
	
	private final LineVAO crossyIndicator;
	private final LineVAO axisIndicator;
	private final InvisibleCubeVAO invisibleCube;
	private final VisibleCubeVAO visibleOpTexCube;
	
	
	private int width = 0;
	private int height = 0;
	private float[] perspectiveProjection;
	
	public ShaderStorage()
	{
		//2D:
		interfaceProjectionMatrix = new Matrix();
		
		interfaceShader = new ShaderProgram("interfaces/interfaceShader");
		flatTextureShader = new ShaderProgram("interfaces/flatTextureShader");
		flatTextShader = new ShaderProgram("interfaces/flatTextShader");
		
		flatTexturePlane = new GenericVAO(new float[]{
				-1, -1, 0, 0, // L T
				-1, +1, 0, 1, // L B
				+1, -1, 1, 0, // R T
				+1, +1, 1, 1, // R B
		}, new short[]{
				0, 1, 2,
				1, 3, 2,
		})
		{
			@Override
			protected void init()
			{
				//Position:
				GL30.glVertexAttribPointer(0, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 0);
				GL30.glEnableVertexAttribArray(0);
				//TextureCoord:
				GL30.glVertexAttribPointer(1, 2, GL30.GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
				GL30.glEnableVertexAttribArray(1);
			}
		};
		
		flatPlaneShader = new ShaderProgram("outline/flatPlaneShader");
		flatPlane = FlatPlaneVAO.generateFullCanvasPlane();
		
		//3D:
		sdfShader = new ShaderProgram("sdfLabel");
		
		invisibleCubeShader = new ShaderProgram("invisibleCubeShader");
		invisibleCube = InvisibleCubeVAO.generateInvisibleCube();
		
		visibleCubeShader = new ShaderProgram("visibleCubeShader");
		textureCubeShader = new ShaderProgram("textureCubeShader");
		visibleOpTexCube = VisibleCubeVAO.generateVisibleCube();
		
		lineShader = new ShaderProgram("lineShader");
		crossyIndicator = LineVAO.generateCrossyIndicator();
		axisIndicator = LineVAO.generateAxisIndicator();
	}
	
	public void newSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		//2D section:
		interfaceProjectionMatrix.interfaceMatrix(width, height);
		float[] pM = interfaceProjectionMatrix.getMat();
		interfaceShader.use();
		interfaceShader.setUniformM4(0, pM);
		flatTextureShader.use();
		flatTextureShader.setUniformM4(0, pM);
		flatTextShader.use();
		flatTextShader.setUniformM4(0, pM);
		
		//3D section:
		Matrix p = new Matrix();
		p.perspective(Settings.fov, (float) width / (float) height, 0.1f, 100000f);
		float[] projection = p.getMat();
		perspectiveProjection = projection;
		
		textureCubeShader.use();
		textureCubeShader.setUniformM4(0, projection);
		visibleCubeShader.use();
		visibleCubeShader.setUniformM4(0, projection);
		sdfShader.use();
		sdfShader.setUniformM4(0, projection);
		lineShader.use();
		lineShader.setUniformM4(0, projection);
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(0, projection);
	}
	
	//2D Getters:
	
	public ShaderProgram getInterfaceShader()
	{
		return interfaceShader;
	}
	
	public ShaderProgram getFlatTextShader()
	{
		return flatTextShader;
	}
	
	public ShaderProgram getFlatTextureShader()
	{
		return flatTextureShader;
	}
	
	public GenericVAO getFlatTexturePlane()
	{
		return flatTexturePlane;
	}
	
	public ShaderProgram getFlatPlaneShader()
	{
		return flatPlaneShader;
	}
	
	public FlatPlaneVAO getFlatPlane()
	{
		return flatPlane;
	}
	
	//3D Getters:
	
	public ShaderProgram getLineShader()
	{
		return lineShader;
	}
	
	public GenericVAO getCrossyIndicator()
	{
		return crossyIndicator;
	}
	
	public GenericVAO getAxisIndicator()
	{
		return axisIndicator;
	}
	
	public ShaderProgram getInvisibleCubeShader()
	{
		return invisibleCubeShader;
	}
	
	public GenericVAO getInvisibleCube()
	{
		return invisibleCube;
	}
	
	public ShaderProgram getVisibleCubeShader()
	{
		return visibleCubeShader;
	}
	
	public ShaderProgram getTextureCubeShader()
	{
		return textureCubeShader;
	}
	
	public GenericVAO getVisibleOpTexCube()
	{
		return visibleOpTexCube;
	}
	
	public ShaderProgram getSdfShader()
	{
		return sdfShader;
	}
	
	//Other:
	
	public float[] getProjectionMatrix()
	{
		return perspectiveProjection;
	}
	
	public void resetViewportAndVisibleCubeShader()
	{
		visibleCubeShader.setUniformM4(0, perspectiveProjection);
		GL30.glViewport(0, 0, width, height);
	}
	
	public int getWidth()
	{
		return width;
	}
	
	public int getHeight()
	{
		return height;
	}
}
