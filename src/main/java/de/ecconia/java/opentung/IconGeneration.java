package de.ecconia.java.opentung;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompButton;
import de.ecconia.java.opentung.components.CompColorDisplay;
import de.ecconia.java.opentung.components.CompDelayer;
import de.ecconia.java.opentung.components.CompDisplay;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompMount;
import de.ecconia.java.opentung.components.CompNoisemaker;
import de.ecconia.java.opentung.components.CompPanelButton;
import de.ecconia.java.opentung.components.CompPanelColorDisplay;
import de.ecconia.java.opentung.components.CompPanelDisplay;
import de.ecconia.java.opentung.components.CompPanelLabel;
import de.ecconia.java.opentung.components.CompPanelSwitch;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompSwitch;
import de.ecconia.java.opentung.components.CompThroughBlotter;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.fragments.CubeFull;
import de.ecconia.java.opentung.components.fragments.CubeOpenRotated;
import de.ecconia.java.opentung.components.fragments.Meshable;
import de.ecconia.java.opentung.components.meta.ModelHolder;
import de.ecconia.java.opentung.libwrap.Matrix;
import de.ecconia.java.opentung.libwrap.ShaderProgram;
import de.ecconia.java.opentung.libwrap.vaos.VisualShapeVAO;
import de.ecconia.java.opentung.math.Vector2;
import de.ecconia.java.opentung.math.Vector3;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GL30;

public class IconGeneration
{
	private static final PlaceableInfo[] placeableInfos = new PlaceableInfo[]{
			CompBoard.info, //null
			CompPeg.info,
			CompInverter.info,
			CompBlotter.info,
			CompDelayer.info,
			CompSwitch.info,
			CompButton.info,
			CompPanelSwitch.info,
			CompPanelButton.info,
			CompNoisemaker.info,
			CompMount.info,
			CompSnappingPeg.info,
			CompThroughBlotter.info,
			CompThroughPeg.info,
			CompDisplay.info,
			CompPanelDisplay.info,
			CompColorDisplay.info,
			CompPanelColorDisplay.info,
			CompLabel.info,
			CompPanelLabel.info,
	};
	
	public static void render(ShaderProgram shader, VisualShapeVAO visualShape)
	{
		//TODO: Set canvas to 200² and prepare/restore
		
		int side = 200;
		
		GL30.glViewport(0, 0, side, side);
		GL30.glClearColor(1, 1, 0, 1);
		
		visualShape.use();
		for(PlaceableInfo info : placeableInfos)
		{
			System.out.println("Rendering: " + info.getName());
			
			OpenTUNG.clear();
			
			{
				ModelHolder infoModel = info.getModel();
				
				shader.use();
				
				Matrix viewMatrix = new Matrix();
				viewMatrix.translate(0.0f, 0.0f, 0);
				viewMatrix.rotate(-20, 1, 0, 0);
				viewMatrix.rotate(-45, 0, 1, 0);
				shader.setUniform(1, viewMatrix.getMat());
				
				List<Meshable> meshes = new ArrayList<>();
				meshes.addAll(infoModel.getSolid());
				meshes.addAll(infoModel.getPegModels());
				meshes.addAll(infoModel.getBlotModels());
				meshes.addAll(infoModel.getColorables());

				Matrix pv = new Matrix(); //pMatCopy
				pv.orthoMatrix(side, side);
				pv.multiply(viewMatrix);
				
				double maxX = Double.MIN_VALUE;
				double minX = Double.MAX_VALUE;
				double maxY = Double.MIN_VALUE;
				double minY = Double.MAX_VALUE;
				Vector3[] edges = new Vector3[]{
						new Vector3(-1, -1, -1),
						new Vector3(-1, -1, +1),
						new Vector3(-1, +1, -1),
						new Vector3(-1, +1, +1),
						new Vector3(+1, -1, -1),
						new Vector3(+1, -1, +1),
						new Vector3(+1, +1, -1),
						new Vector3(+1, +1, +1),
				};
				for(Meshable meshable : meshes)
				{
					CubeFull cast = (CubeFull) meshable;
					Vector3 relPos = cast.getPosition();
					Vector3 size = cast.getSize();
					if(info == CompBoard.info)
					{
						//TODO: Make generic (Not generic cause not worth the effort right now).
						size = new Vector3(0.15, size.getY(), 0.15);
					}
					
					for(Vector3 edge : edges)
					{
						Vector3 modelPos = edge.multiply(size).add(relPos);//.add(infoModel.getPlacementOffset());
						if(meshable instanceof CubeOpenRotated)
						{
							modelPos = ((CubeOpenRotated) meshable).getRotation().multiply(modelPos);
						}
						Vector2 mapped = pv.getMapped(modelPos);
						if(mapped.getX() > maxX)
						{
							maxX = mapped.getX();
						}
						if(mapped.getX() < minX)
						{
							minX = mapped.getX();
						}
						if(mapped.getY() > maxY)
						{
							maxY = mapped.getY();
						}
						if(mapped.getY() < minY)
						{
							minY = mapped.getY();
						}
					}
				}

				double dx = Math.abs(maxX) + Math.abs(minX);
				double dy = Math.abs(maxY) + Math.abs(minY);
				
				double scale;
				if(dx > dy)
				{
					scale = 2.0 / dx;
				}
				else
				{
					scale = 2.0 / dy;
				}
				
				//Move them to where they would be after scaling.
				maxX *= scale;
				minX *= scale;
				maxY *= scale;
				minY *= scale;
				
				double ty;
				if(maxY > 1)
				{
					ty = -(maxY - 1);
					minY += ty;
					ty -= ((minY + 1) / 2.0);
				}
				else if(minY < -1)
				{
					ty = -(minY + 1);
					maxY += ty;
					ty -= ((maxY - 1) / 2.0);
				}
				else
				{
					double maxIssue = 1.0 - maxY;
					double minIssue = -(-1.0 - minY);
					if(maxIssue > minIssue)
					{
						ty = (maxIssue - minIssue) / 2.0;
					}
					else
					{
						ty = -(minIssue - maxIssue) / 2.0;
					}
				}
				
				double tx;
				if(maxX > 1)
				{
					tx = -(maxX - 1);
					minX += tx;
					tx -= ((minX + 1) / 2.0);
				}
				else if(minX < -1)
				{
					tx = -(minX + 1);
					maxX += tx;
					tx -= ((maxX - 1) / 2.0);
				}
				else
				{
					double maxIssue = 1.0 - maxX;
					double minIssue = -(-1.0 - minX);
					if(maxIssue > minIssue)
					{
						tx = (maxIssue - minIssue) / 2.0;
					}
					else
					{
						tx = -(minIssue - maxIssue) / 2.0;
					}
				}
				
				Matrix scaleMat = new Matrix();
				scaleMat.scale((float) scale, (float) scale, 1);
				Matrix transMat = new Matrix();
				transMat.translate((float) (tx), (float) (ty), 0);
				
				transMat.multiply(scaleMat);
				
				Matrix projection = new Matrix();
				projection.orthoMatrix(side, side);
				transMat.multiply(projection);
				shader.setUniform(0, transMat.getMat());
				
				for(Meshable meshable : meshes)
				{
					CubeFull cast = (CubeFull) meshable;
					
					Vector3 relPos = cast.getPosition();
					Vector3 size = cast.getSize();
					if(info == CompBoard.info)
					{
						//TODO: Make generic (Not generic cause not worth the effort right now).
						size = new Vector3(0.15, size.getY(), 0.15);
					}
					Matrix model = new Matrix();
					model.identity();
					if(meshable instanceof CubeOpenRotated)
					{
						model.multiply(new Matrix(((CubeOpenRotated) meshable).getRotation().inverse().createMatrix()));
					}
					model.translate((float) relPos.getX(), (float) relPos.getY(), (float) relPos.getZ());
					model.scale((float) size.getX(), (float) size.getY(), (float) size.getZ());
					shader.setUniform(2, model.getMat());
					
					shader.setUniformV4(3, cast.getColorArray());
					
					visualShape.draw();
				}
			}
			
			GL30.glFlush();
			GL30.glFinish();
			GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, 1);
			
			float[] values = new float[side * side * 3];
			GL30.glReadPixels(0, 0, side, side, GL30.GL_RGB, GL30.GL_FLOAT, values);
			
			BufferedImage image = new BufferedImage(side, side, BufferedImage.TYPE_4BYTE_ABGR);
			int i = 0;
			for(int y = side - 1; y >= 0; y--)
			{
				for(int x = 0; x < side; x++)
				{
					float rf = values[i++];
					float gf = values[i++];
					float bf = values[i++];
					int r = (int) (rf * 255.0) & 255;
					int g = (int) (gf * 255.0) & 255;
					int b = (int) (bf * 255.0) & 255;
					
					if(r == 255 && g == 255 && b == 0)
					{
						image.setRGB(x, y, 0);
					}
					else
					{
						image.setRGB(x, y, (255 << 24) | (r << 16) | (g << 8) | (b));
					}
				}
			}
			
//			try
//			{
//				ImageIO.write(image, "PNG", new File(info.getName() + ".png"));
//			}
//			catch(IOException e)
//			{
//				e.printStackTrace();
//			}
		}
	}
}
