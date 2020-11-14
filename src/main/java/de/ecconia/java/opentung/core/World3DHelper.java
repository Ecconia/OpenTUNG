package de.ecconia.java.opentung.core;

import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.components.meta.Part;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.GenericVAO;
import de.ecconia.java.opentung.util.math.Quaternion;
import de.ecconia.java.opentung.util.math.Vector3;

public class World3DHelper
{
	public static void drawStencilComponent(ShaderProgram invisibleCubeShader, GenericVAO invisibleCube, Component component, float[] view)
	{
		invisibleCubeShader.use();
		invisibleCubeShader.setUniformM4(1, view);
		invisibleCubeShader.setUniformV4(3, new float[]{0, 0, 0, 0});
		Matrix matrix = new Matrix();
		Vector3 placementOffset = component.getModelHolder().getPlacementOffset();
		for(Meshable meshable : component.getModelHolder().getColorables())
		{
			drawCubeFull(invisibleCubeShader, invisibleCube, (CubeFull) meshable, component, placementOffset, matrix);
		}
		for(Meshable meshable : component.getModelHolder().getSolid())
		{
			drawCubeFull(invisibleCubeShader, invisibleCube, (CubeFull) meshable, component, placementOffset, matrix);
		}
		for(Meshable meshable : component.getModelHolder().getConductors())
		{
			drawCubeFull(invisibleCubeShader, invisibleCube, (CubeFull) meshable, component, placementOffset, matrix);
		}
	}
	
	public static void drawModel(ShaderProgram visibleCubeShader, GenericVAO visibleCube, ModelHolder model, Vector3 position, Quaternion quaternion, float[] view)
	{
		visibleCubeShader.use();
		visibleCubeShader.setUniformM4(1, view);
		Matrix matrix = new Matrix();
		Matrix rotation = new Matrix(quaternion.createMatrix());
		Vector3 placementOffset = model.getPlacementOffset();
		for(Meshable meshable : model.getColorables())
		{
			visibleCubeShader.setUniformV4(3, ((CubeFull) meshable).getColorArray());
			drawCubeFull(visibleCubeShader, visibleCube, (CubeFull) meshable, position, rotation, placementOffset, matrix);
		}
		for(Meshable meshable : model.getSolid())
		{
			visibleCubeShader.setUniformV4(3, ((CubeFull) meshable).getColorArray());
			drawCubeFull(visibleCubeShader, visibleCube, (CubeFull) meshable, position, rotation, placementOffset, matrix);
		}
		for(Meshable meshable : model.getConductors())
		{
			visibleCubeShader.setUniformV4(3, ((CubeFull) meshable).getColorArray());
			drawCubeFull(visibleCubeShader, visibleCube, (CubeFull) meshable, position, rotation, placementOffset, matrix);
		}
	}
	
	public static void drawCubeFull(ShaderProgram visibleCubeShader, GenericVAO visibleCube, CubeFull cube, Vector3 position, Matrix rotation, Vector3 placementOffset, Matrix matrix)
	{
		//TBI: maybe optimize this a bit more, its quite a lot annoying matrix operations.
		matrix.identity();
		matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
		matrix.multiply(rotation);
		matrix.translate((float) placementOffset.getX(), (float) placementOffset.getY(), (float) placementOffset.getZ());
		if(cube instanceof CubeOpenRotated)
		{
			matrix.multiply(new Matrix(((CubeOpenRotated) cube).getRotation().inverse().createMatrix()));
		}
		Vector3 cubePosition = cube.getPosition();
		matrix.translate((float) cubePosition.getX(), (float) cubePosition.getY(), (float) cubePosition.getZ());
		Vector3 size = cube.getSize();
		matrix.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
		visibleCubeShader.setUniformM4(2, matrix.getMat());
		
		visibleCube.use();
		visibleCube.draw();
	}
	
	public static void drawCubeFull(ShaderProgram invisibleCubeShader, GenericVAO invisibleCube, CubeFull cube, Part part, Vector3 placementOffset, Matrix matrix)
	{
		//TBI: maybe optimize this a bit more, its quite a lot annoying matrix operations.
		matrix.identity();
		Vector3 position = part.getPosition();
		matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
		Matrix rotMat = new Matrix(part.getRotation().createMatrix());
		matrix.multiply(rotMat);
		Vector3 size = cube.getSize();
		if(cube.getMapper() != null)
		{
			size = cube.getMapper().getMappedSize(size, part);
		}
		matrix.translate((float) placementOffset.getX(), (float) placementOffset.getY(), (float) placementOffset.getZ());
		if(cube instanceof CubeOpenRotated)
		{
			matrix.multiply(new Matrix(((CubeOpenRotated) cube).getRotation().inverse().createMatrix()));
		}
		position = cube.getPosition();
		matrix.translate((float) position.getX(), (float) position.getY(), (float) position.getZ());
		matrix.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
		invisibleCubeShader.setUniformM4(2, matrix.getMat());
		
		invisibleCube.use();
		invisibleCube.draw();
	}
}
