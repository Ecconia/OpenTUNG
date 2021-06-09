package de.ecconia.java.opentung.core.data;

import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.TextureWrapper;
import de.ecconia.java.opentung.libwrap.vaos.FlatPlaneVAO;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.libwrap.vaos.InvisibleCubeVAO;
import de.ecconia.java.opentung.libwrap.vaos.LineVAO;
import de.ecconia.java.opentung.libwrap.vaos.VisibleCubeVAO;
import de.ecconia.java.opentung.settings.Settings;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import org.lwjgl.opengl.GL30;

public class ShaderStorage
{
	//2D:
	private final Matrix interfaceProjectionMatrix;
	
	private final ShaderProgram interfaceShader;
	private final ShaderProgram flatTextShader;
	
	private final ShaderProgram flatTextureShader;
	private final GenericVAO flatTexturePlane;
	
	private final ShaderProgram flatPlaneShader;
	private final FlatPlaneVAO flatPlane;
	
	//3D:
	private final Matrix perspectiveProjectionMatrix;
	private float[] perspectiveProjection;
	
	private final ShaderProgram lineShader;
	private final ShaderProgram invisibleCubeShader;
	private final ShaderProgram visibleCubeShader;
	private final ShaderProgram textureCubeShader;
	private final ShaderProgram sdfShader;
	
	private final LineVAO crossyIndicator;
	private final LineVAO axisIndicator;
	private final InvisibleCubeVAO invisibleCube;
	private final VisibleCubeVAO visibleOpTexCube;
	
	private final ShaderProgram skyboxShader;
	
	private final ShaderProgram resizeShader;
	private final GenericVAO resizeSurface;
	private final GenericVAO resizeBorder;
	
	//Textures:
	private final TextureWrapper boardTexture;
	
	//Meshes:
	
	private final ShaderProgram meshBoardShader;
	private final ShaderProgram meshSolidShader;
	private final ShaderProgram meshColorShader;
	private final ShaderProgram meshConductorShader;
	
	//Other:
	
	private int width = 0;
	private int height = 0;
	
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
		perspectiveProjectionMatrix = new Matrix();
		
		sdfShader = new ShaderProgram("sdfLabel");
		
		invisibleCubeShader = new ShaderProgram("invisibleCubeShader");
		invisibleCube = InvisibleCubeVAO.generateInvisibleCube();
		
		visibleCubeShader = new ShaderProgram("visibleCubeShader");
		textureCubeShader = new ShaderProgram("textureCubeShader");
		visibleOpTexCube = VisibleCubeVAO.generateVisibleCube();
		
		lineShader = new ShaderProgram("lineShader");
		crossyIndicator = LineVAO.generateCrossyIndicator();
		axisIndicator = LineVAO.generateAxisIndicator();
		
		skyboxShader = new ShaderProgram("skybox/skybox");
		
		resizeShader = new ShaderProgram("resize/surface");
		resizeSurface = new GenericVAO(new float[]{
				-0.5f, 0, -0.5f,
				-0.5f, 0, +0.5f,
				+0.5f, 0, -0.5f,
				+0.5f, 0, +0.5f,
		}, new short[]{
				0, 1, 2,
				1, 3, 2,
		})
		{
			@Override
			protected void init()
			{
				//Position:
				GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 3 * Float.BYTES, 0);
				GL30.glEnableVertexAttribArray(0);
			}
		};
		resizeBorder = new LineVAO(new float[]{
				-0.5f, 0, -0.5f,
				-0.5f, 0, +0.5f,
				+0.5f, 0, -0.5f,
				+0.5f, 0, +0.5f,
		}, new short[]{
				0, 1,
				1, 3,
				3, 2,
				2, 0
		})
		{
			@Override
			protected void init()
			{
				//Position:
				GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 3 * Float.BYTES, 0);
				GL30.glEnableVertexAttribArray(0);
			}
		};
		
		//Textures:
		{
			int side = 16;
			BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = image.createGraphics();
			g.setColor(java.awt.Color.white);
			g.fillRect(0, 0, side - 1, side - 1);
			g.setColor(new java.awt.Color(0x777777));
			g.drawRect(0, 0, side - 1, side - 1);
			g.dispose();
			boardTexture = TextureWrapper.createBoardTexture(image);
		}
		
		//3D/Meshes:
		this.meshBoardShader = new ShaderProgram("mesh/meshBoard");
		this.meshSolidShader = new ShaderProgram("mesh/meshSolid");
		this.meshColorShader = new ShaderProgram("mesh/meshColor");
		this.meshConductorShader = new ShaderProgram("mesh/meshConductor");
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
		perspectiveProjectionMatrix.perspective(Settings.fov, (float) width / (float) height, 0.1f, 100000f);
		float[] projection = perspectiveProjectionMatrix.getMat();
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
		
		skyboxShader.use();
		skyboxShader.setUniformM4(0, projection);
		
		resizeShader.use();
		resizeShader.setUniformM4(0, projection);
		
		//3D/Meshes:
		meshBoardShader.use();
		meshBoardShader.setUniformM4(0, projection);
		meshSolidShader.use();
		meshSolidShader.setUniformM4(0, projection);
		meshColorShader.use();
		meshColorShader.setUniformM4(0, projection);
		meshConductorShader.use();
		meshConductorShader.setUniformM4(0, projection);
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
	
	public ShaderProgram getSkyboxShader()
	{
		return skyboxShader;
	}
	
	public ShaderProgram getResizeShader()
	{
		return resizeShader;
	}
	
	public GenericVAO getResizeSurface()
	{
		return resizeSurface;
	}
	
	public GenericVAO getResizeBorder()
	{
		return resizeBorder;
	}
	
	//3D/Meshes:
	
	public ShaderProgram getMeshBoardShader()
	{
		return meshBoardShader;
	}
	
	public ShaderProgram getMeshSolidShader()
	{
		return meshSolidShader;
	}
	
	public ShaderProgram getMeshColorShader()
	{
		return meshColorShader;
	}
	
	public ShaderProgram getMeshConductorShader()
	{
		return meshConductorShader;
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
	
	public TextureWrapper getBoardTexture()
	{
		return boardTexture;
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
