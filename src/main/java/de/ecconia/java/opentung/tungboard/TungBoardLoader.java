package de.ecconia.java.opentung.tungboard;

import de.ecconia.java.opentung.components.CompBlotter;
import de.ecconia.java.opentung.components.CompBoard;
import de.ecconia.java.opentung.components.CompContainer;
import de.ecconia.java.opentung.components.CompGeneric;
import de.ecconia.java.opentung.components.CompInverter;
import de.ecconia.java.opentung.components.CompLabel;
import de.ecconia.java.opentung.components.CompPeg;
import de.ecconia.java.opentung.components.CompSnappingPeg;
import de.ecconia.java.opentung.components.CompThroughPeg;
import de.ecconia.java.opentung.components.CompWireRaw;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBlotter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;
import de.ecconia.java.opentung.tungboard.tungobjects.TungInverter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungLabel;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungSnappingPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungThroughPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungWire;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungAngles;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungPosition;
import java.nio.file.NoSuchFileException;

public class TungBoardLoader
{
	public static CompBoard convert(String filename)
	{
		try
		{
			TungBoard importedBoard = PrimitiveParser.importTungBoard("boards/" + filename + ".tungboard");
			importedBoard.setPosition(new TungPosition(0, 0, 0));
			importedBoard.setAngles(new TungAngles(180, 0, 0)); //Adjust this depending on how you want to import the board.
			return (CompBoard) importChild(null, importedBoard, new Vector3(-20, 0, 0), Quaternion.angleAxis(0, Vector3.yp));
		}
		catch(RuntimeException e)
		{
			if(e.getCause() != null && e.getCause() instanceof NoSuchFileException)
			{
				System.out.println("###########################################");
				System.out.println("Couldn't find tungboard file to display, you can download a nice one here: https://discordapp.com/channels/401255675264761866/588822987331993602/684761768144142337");
				System.out.println("But for gods sake, rename 'CLE' to 'Parallel-CLA'");
				System.out.println("Once you inserted a tungboard, or that one. Change the filename in " + TungBoardLoader.class.getName() + ".");
				System.out.println("###########################################");
				return null;
			}
			else
			{
				throw e;
			}
		}
	}
	
	private static CompGeneric importChild(CompContainer parent, TungObject object, Vector3 parentPosition, Quaternion parentRotation)
	{
		//TODO: Now that it works, optimize. Make it one non-obsolete calculation method.
		Quaternion qx = Quaternion.angleAxis(object.getAngles().getX(), Vector3.xn); //Has to be negative, cause unity *shrug*
		Quaternion qy = Quaternion.angleAxis(-object.getAngles().getY(), Vector3.yp);
		Quaternion qz = Quaternion.angleAxis(-object.getAngles().getZ(), Vector3.zp);
		Quaternion localRotation = qz.multiply(qx).multiply(qy);
		Quaternion globalRotation = localRotation.multiply(parentRotation);
		
		Vector3 localPosition = new Vector3(object.getPosition().getX(), object.getPosition().getY(), object.getPosition().getZ());
		Vector3 rotatedPosition = parentRotation.inverse().multiply(localPosition);
		Vector3 globalPosition = parentPosition.add(rotatedPosition);
		
		if(object instanceof TungBoard)
		{
			TungBoard tungBoard = (TungBoard) object;
			int x = tungBoard.getX();
			int z = tungBoard.getZ();
			
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.15f * (float) x, 0, 0.15f * (float) z));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompBoard board = new CompBoard(parent, x, z);
			board.setColor(new Vector3(tungBoard.getColor().getR(), tungBoard.getColor().getG(), tungBoard.getColor().getB()));
			board.setPosition(globalPosition.add(rotatedFixPoint));
			board.setRotation(globalRotation);
			
			for(TungObject tungChild : tungBoard.getChildren())
			{
				CompGeneric child = importChild(board, tungChild, globalPosition, globalRotation);
				if(child != null)
				{
					board.addChild(child);
				}
			}
			
			return board;
		}
		else if(object instanceof TungPeg)
		{
			CompPeg peg = new CompPeg(parent);
			peg.setPosition(globalPosition);
			peg.setRotation(globalRotation);
			
			return peg;
		}
		else if(object instanceof TungInverter)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompInverter inverter = new CompInverter(parent);
			inverter.setPosition(globalPosition.add(rotatedFixPoint));
			inverter.setRotation(globalRotation);
			
			return inverter;
		}
		else if(object instanceof TungBlotter)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, 0.15f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompBlotter blotter = new CompBlotter(parent);
			blotter.setPosition(globalPosition.add(rotatedFixPoint));
			blotter.setRotation(globalRotation);
			
			return blotter;
		}
		else if(object instanceof TungThroughPeg)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompThroughPeg throughPeg = new CompThroughPeg(parent);
			throughPeg.setPosition(globalPosition.add(rotatedFixPoint));
			throughPeg.setRotation(globalRotation);
			
			return throughPeg;
		}
		else if(object instanceof TungSnappingPeg)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, 0.0f, -0.06f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompSnappingPeg snappingPeg = new CompSnappingPeg(parent);
			snappingPeg.setPosition(globalPosition.add(rotatedFixPoint));
			snappingPeg.setRotation(globalRotation);
			
			return snappingPeg;
		}
		else if(object instanceof TungWire)
		{
			TungWire tungWire = (TungWire) object;
			
			CompWireRaw wire = new CompWireRaw(parent);
			wire.setPosition(globalPosition);
			wire.setRotation(globalRotation);
			wire.setLength(tungWire.getLength());
			wire.setPowered(true);
			
			return wire;
		}
		else if(object instanceof TungLabel)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompLabel label = new CompLabel(parent);
			label.setPosition(globalPosition.add(rotatedFixPoint));
			label.setRotation(globalRotation);
			
			TungLabel t = (TungLabel) object;
			label.setFontSize(t.getFontSize());
			label.setText(t.getText());
			
			return label;
		}
		else
		{
			System.out.println("Implement: " + object.getClass().getSimpleName());
			return null;
		}
	}
}
