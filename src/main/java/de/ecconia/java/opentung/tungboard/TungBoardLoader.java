package de.ecconia.java.opentung.tungboard;

import de.ecconia.java.opentung.settings.Settings;
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
import de.ecconia.java.opentung.components.conductor.CompWireRaw;
import de.ecconia.java.opentung.components.fragments.Color;
import de.ecconia.java.opentung.components.meta.CompContainer;
import de.ecconia.java.opentung.components.meta.Component;
import de.ecconia.java.opentung.math.Quaternion;
import de.ecconia.java.opentung.math.Vector3;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBlotter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;
import de.ecconia.java.opentung.tungboard.tungobjects.TungButton;
import de.ecconia.java.opentung.tungboard.tungobjects.TungColorDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungDelayer;
import de.ecconia.java.opentung.tungboard.tungobjects.TungDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungInverter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungLabel;
import de.ecconia.java.opentung.tungboard.tungobjects.TungMount;
import de.ecconia.java.opentung.tungboard.tungobjects.TungNoisemaker;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelButton;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelColorDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelDisplay;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelLabel;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPanelSwitch;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungSnappingPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungSwitch;
import de.ecconia.java.opentung.tungboard.tungobjects.TungThroughBlotter;
import de.ecconia.java.opentung.tungboard.tungobjects.TungThroughPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungWire;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungAngles;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColor;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColorEnum;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungPosition;
import java.io.File;

public class TungBoardLoader
{
	private static CompBoard convertTungBoard(File file)
	{
		System.out.println("[BoardImport] Started reading file.");
		TungBoard importedBoard = PrimitiveParser.importTungBoard(file);
		System.out.println("[BoardImport] Started converting board.");
		importedBoard.setPosition(new TungPosition(0, 0, 0));
		importedBoard.setAngles(new TungAngles(Settings.rootBoardAngleX, Settings.rootBoardAngleY, Settings.rootBoardAngleZ));
		return (CompBoard) importChild(null, importedBoard, new Vector3(Settings.rootBoardOffsetX, Settings.rootBoardOffsetY, Settings.rootBoardOffsetZ), Quaternion.angleAxis(0, Vector3.yp));
	}
	
	public static CompBoard importTungBoard(File file)
	{
		CompBoard board = convertTungBoard(file);
		
		System.out.println("[BoardImport] Initializing components.");
		init(board);
		
		return board;
	}
	
	private static void init(CompContainer container)
	{
		container.init();
		
		for(Component component : container.getChildren())
		{
			if(component instanceof CompContainer)
			{
				init((CompContainer) component);
				continue;
			}
			component.init();
		}
	}
	
	private static Component importChild(CompContainer parent, TungObject object, Vector3 parentPosition, Quaternion parentRotation)
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
			TungColor componentColor = tungBoard.getColor();
			board.setColor(Color.fromComponent(componentColor.getR(), componentColor.getG(), componentColor.getB()));
			board.setPosition(globalPosition.add(rotatedFixPoint));
			board.setRotation(globalRotation);
			
			for(TungObject tungChild : tungBoard.getChildren())
			{
				Component child = importChild(board, tungChild, globalPosition, globalRotation);
				if(child != null)
				{
					board.addChild(child);
				}
			}
			
			return board;
		}
		else if(object instanceof TungMount)
		{
			TungMount tungMount = (TungMount) object;
			
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0, -0.075, -0.15));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompMount mount = new CompMount(parent);
			mount.setPosition(globalPosition.add(rotatedFixPoint));
			mount.setRotation(globalRotation);
			
			for(TungObject tungChild : tungMount.getChildren())
			{
				Component child = importChild(mount, tungChild, globalPosition, globalRotation);
				if(child != null)
				{
					mount.addChild(child);
				}
			}
			
			return mount;
		}
		else if(object instanceof TungPeg)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompPeg peg = new CompPeg(parent);
			peg.setPosition(globalPosition.add(rotatedFixPoint));
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
			inverter.setPowered(((TungInverter) object).isOutputOn());
			
			return inverter;
		}
		else if(object instanceof TungBlotter)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompBlotter blotter = new CompBlotter(parent);
			blotter.setPosition(globalPosition.add(rotatedFixPoint));
			blotter.setRotation(globalRotation);
			blotter.setPowered(((TungBlotter) object).isOutputOn());
			
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
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, -0.06f));
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
		else if(object instanceof TungPanelLabel)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompPanelLabel label = new CompPanelLabel(parent);
			label.setPosition(globalPosition.add(rotatedFixPoint));
			label.setRotation(globalRotation);
			
			TungPanelLabel t = (TungPanelLabel) object;
			label.setFontSize(t.getFontSize());
			label.setText(t.getText());
			
			return label;
		}
		else if(object instanceof TungDisplay)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompDisplay display = new CompDisplay(parent);
			display.setPosition(globalPosition.add(rotatedFixPoint));
			display.setRotation(globalRotation);
			TungColorEnum c = ((TungDisplay) object).getColor();
			display.setColorRaw(new Color(c.getR(), c.getG(), c.getB()));
			
			return display;
		}
		else if(object instanceof TungSwitch)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompSwitch toggle = new CompSwitch(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			toggle.setPowered(((TungSwitch) object).isOn());
			
			return toggle;
		}
		else if(object instanceof TungButton)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompButton toggle = new CompButton(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			
			return toggle;
		}
		else if(object instanceof TungPanelSwitch)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompPanelSwitch toggle = new CompPanelSwitch(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			toggle.setPowered(((TungPanelSwitch) object).isOn());
			
			return toggle;
		}
		else if(object instanceof TungPanelDisplay)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompPanelDisplay display = new CompPanelDisplay(parent);
			display.setPosition(globalPosition.add(rotatedFixPoint));
			display.setRotation(globalRotation);
			TungColorEnum c = ((TungPanelDisplay) object).getColor();
			display.setColorRaw(new Color(c.getR(), c.getG(), c.getB()));
			
			return display;
		}
		else if(object instanceof TungThroughBlotter)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompThroughBlotter blotter = new CompThroughBlotter(parent);
			blotter.setPosition(globalPosition.add(rotatedFixPoint));
			blotter.setRotation(globalRotation);
			blotter.setPowered(((TungThroughBlotter) object).isOutputOn());
			
			return blotter;
		}
		else if(object instanceof TungPanelButton)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompPanelButton toggle = new CompPanelButton(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			
			return toggle;
		}
		else if(object instanceof TungColorDisplay)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompColorDisplay toggle = new CompColorDisplay(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			
			return toggle;
		}
		else if(object instanceof TungPanelColorDisplay)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompPanelColorDisplay toggle = new CompPanelColorDisplay(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			
			return toggle;
		}
		else if(object instanceof TungNoisemaker)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0.0f, -0.075f, 0.0f));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompNoisemaker toggle = new CompNoisemaker(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			toggle.setFrequency(((TungNoisemaker) object).getFrequency());
			
			return toggle;
		}
		else if(object instanceof TungDelayer)
		{
			Vector3 fixPoint = localRotation.inverse().multiply(new Vector3(0, -0.075, 0));
			Vector3 rotatedFixPoint = parentRotation.inverse().multiply(fixPoint);
			
			CompDelayer toggle = new CompDelayer(parent);
			toggle.setPosition(globalPosition.add(rotatedFixPoint));
			toggle.setRotation(globalRotation);
			toggle.setPowered(((TungDelayer) object).isOutputOn());
			toggle.setDelayCount(((TungDelayer) object).getDelayCount());
			
			return toggle;
		}
		else
		{
			System.out.println("Implement: " + object.getClass().getSimpleName());
			return null;
		}
	}
}
