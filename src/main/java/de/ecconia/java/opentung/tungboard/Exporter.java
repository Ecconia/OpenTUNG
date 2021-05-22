package de.ecconia.java.opentung.tungboard;

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
import de.ecconia.java.opentung.util.io.ByteWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

public class Exporter
{
	private static final byte REC_HEADER = 0;
	private static final byte REC_REF_CLASS = 1;
	private static final byte REC_DEFs_CLASS = 5;
	private static final byte REC_STRING = 6;
	private static final byte REC_ARRAY = 7;
	private static final byte REC_REFERENCE = 9;
	private static final byte REC_NULL = 10;
	private static final byte REC_DONE = 11;
	private static final byte REC_LIBRARY = 12;
	
	private final ByteWriter w;
	private final Queue<Object> queue = new LinkedList<>();
	
	private Map<Class<?>, Integer> definedClasses = new HashMap<>();
	private int index = 1;
	
	private Integer libID;
	
	public Exporter(Path file, TungBoard rootBoard)
	{
		w = new ByteWriter(file);
		
		//Write Header:
		w.writeByte(REC_HEADER);
		w.writeInt(1);
		w.writeInt(-1);
		w.writeInt(1);
		w.writeInt(0);
		
		queue.add(rootBoard);
		
		System.out.println("Export start.");
		
		//Start writing the main Board.
		while(!queue.isEmpty())
		{
			Object thing = queue.remove();
			
			int id;
			if(thing instanceof ReservedObject)
			{
				ReservedObject res = (ReservedObject) thing;
				id = res.getId();
				thing = res.getObject();
			}
			else
			{
				id = index++;
			}
			
			if(thing instanceof TungBoard)
			{
				storeBoard((TungBoard) thing, id);
			}
			else if(thing instanceof List)
			{
				storeArray((List<?>) thing, id);
			}
			else if(thing instanceof TungPeg)
			{
				storePeg((TungPeg) thing, id);
			}
			else if(thing instanceof TungWire)
			{
				storeWire((TungWire) thing, id);
			}
			else if(thing instanceof TungInverter)
			{
				storeInverter((TungInverter) thing, id);
			}
			else if(thing instanceof TungPanelLabel)
			{
				storePanelLabel((TungPanelLabel) thing, id);
			}
			else if(thing instanceof TungLabel)
			{
				storelLabel((TungLabel) thing, id);
			}
			else if(thing instanceof TungThroughPeg)
			{
				storeThroughPeg((TungThroughPeg) thing, id);
			}
			else if(thing instanceof TungSnappingPeg)
			{
				storeSnappingPeg((TungSnappingPeg) thing, id);
			}
			else if(thing instanceof TungSwitch)
			{
				storeSwitch((TungSwitch) thing, id);
			}
			else if(thing instanceof TungPanelSwitch)
			{
				storePanelSwitch((TungPanelSwitch) thing, id);
			}
			else if(thing instanceof TungDisplay)
			{
				storeDisplay((TungDisplay) thing, id);
			}
			else if(thing instanceof TungPanelDisplay)
			{
				storePanelDisplay((TungPanelDisplay) thing, id);
			}
			else if(thing instanceof TungBlotter)
			{
				storeBlotter((TungBlotter) thing, id);
			}
			else if(thing instanceof TungThroughBlotter)
			{
				storeThroughBlotter((TungThroughBlotter) thing, id);
			}
			else if(thing instanceof TungMount)
			{
				storeMount((TungMount) thing, id);
			}
			else if(thing instanceof TungDelayer)
			{
				storeDelayer((TungDelayer) thing, id);
			}
			else if(thing instanceof TungButton)
			{
				storeButton((TungButton) thing, id);
			}
			else if(thing instanceof TungPanelButton)
			{
				storePanelButton((TungPanelButton) thing, id);
			}
			else if(thing instanceof TungNoisemaker)
			{
				storeNoisemaker((TungNoisemaker) thing, id);
			}
			else if(thing instanceof TungColorDisplay)
			{
				storeColorDisplay((TungColorDisplay) thing, id);
			}
			else if(thing instanceof TungPanelColorDisplay)
			{
				storePanelColorDisplay((TungPanelColorDisplay) thing, id);
			}
			else
			{
				throw new RuntimeException("I don't know how to export: " + thing.getClass().getSimpleName());
			}
		}
		
		w.writeByte(REC_DONE);
		w.close();
		
		System.out.println("Export done.");
	}
	
	//###############################################################
	// STORE PRIMITIVES:
	//###############################################################
	
	private void storeArray(List<?> list, int id)
	{
		w.writeByte(REC_ARRAY);
		w.writeInt(id);
		w.writeByte(0); //Array type
		w.writeInt(1); //One dimensional
		
		w.writeInt(list.size());
		
		w.writeByte(4);
		w.writeString("SavedObjects.SavedObjectV2");
		w.writeInt(libID);
		
		for(Object o : list)
		{
			TungObject component = (TungObject) o;
			int someID = index++;
			writeMemberReference(someID);
			queue.add(new ReservedObject(someID, component));
		}
	}
	
	private void storeAngles(TungAngles angle, int id)
	{
		Integer templateID = definedClasses.get(Vec3.class);
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(Vec3.class, id);
			writeVec3Definition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeFloat(angle.getX());
		w.writeFloat(angle.getY());
		w.writeFloat(angle.getZ());
	}
	
	private void storePosition(TungPosition position, int id)
	{
		Integer templateID = definedClasses.get(Vec3.class);
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(Vec3.class, id);
			writeVec3Definition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeFloat(position.getX());
		w.writeFloat(position.getY());
		w.writeFloat(position.getZ());
	}
	
	private void storeColor(TungColor color, int id)
	{
		Integer templateID = definedClasses.get(color.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(color.getClass(), id);
			writeColorDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeFloat(color.getR());
		w.writeFloat(color.getG());
		w.writeFloat(color.getB());
	}
	
	private void storeBoard(TungBoard board, int id)
	{
		Integer templateID = definedClasses.get(board.getClass());
		getLibID();
		if(templateID == null)
		{
			//New class entry
			definedClasses.put(board.getClass(), id);
			writeBoardDefinition(id);
		}
		else
		{
			//Reference class entry:
			writeTemplateRecord(id, templateID);
		}
		
		w.writeInt(board.getX());
		w.writeInt(board.getZ());
		storeColor(board.getColor(), -index++);
		storePosition(board.getPosition(), -index++);
		storeAngles(board.getAngles(), -index++);
		
		int arrayID = index++;
		writeMemberReference(arrayID);
		queue.add(new ReservedObject(arrayID, board.getChildren()));
	}
	
	private void storePeg(TungPeg peg, int id)
	{
		Integer templateID = definedClasses.get(peg.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(peg.getClass(), id);
			writePegDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(peg.getPosition(), -index++);
		storeAngles(peg.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeButton(TungButton button, int id)
	{
		Integer templateID = definedClasses.get(button.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(button.getClass(), id);
			writeButtonDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(button.getPosition(), -index++);
		storeAngles(button.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storePanelButton(TungPanelButton button, int id)
	{
		Integer templateID = definedClasses.get(button.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(button.getClass(), id);
			writePanelButtonDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(button.getPosition(), -index++);
		storeAngles(button.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeWire(TungWire wire, int id)
	{
		Integer templateID = definedClasses.get(wire.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(wire.getClass(), id);
			writeWireDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(wire.isInputInput());
		w.writeFloat(wire.getLength());
		storePosition(wire.getPosition(), -index++);
		storeAngles(wire.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeNoisemaker(TungNoisemaker noisemaker, int id)
	{
		Integer templateID = definedClasses.get(noisemaker.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(noisemaker.getClass(), id);
			writeNoisemakerDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeFloat(noisemaker.getFrequency());
		storePosition(noisemaker.getPosition(), -index++);
		storeAngles(noisemaker.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeInverter(TungInverter inverter, int id)
	{
		Integer templateID = definedClasses.get(inverter.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(inverter.getClass(), id);
			writeInverterDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(inverter.isOutputOn());
		storePosition(inverter.getPosition(), -index++);
		storeAngles(inverter.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeBlotter(TungBlotter blotter, int id)
	{
		Integer templateID = definedClasses.get(blotter.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(blotter.getClass(), id);
			writeBlotterDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(blotter.isOutputOn());
		storePosition(blotter.getPosition(), -index++);
		storeAngles(blotter.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeThroughBlotter(TungThroughBlotter throughBlotter, int id)
	{
		Integer templateID = definedClasses.get(throughBlotter.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(throughBlotter.getClass(), id);
			writeThroughBlotterDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(throughBlotter.isOutputOn());
		storePosition(throughBlotter.getPosition(), -index++);
		storeAngles(throughBlotter.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storePanelLabel(TungPanelLabel panelLabel, int id)
	{
		Integer templateID = definedClasses.get(panelLabel.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(panelLabel.getClass(), id);
			writePanelLabelDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		writeString(panelLabel.getText(), index++);
		w.writeFloat(panelLabel.getFontSize());
		storePosition(panelLabel.getPosition(), -index++);
		storeAngles(panelLabel.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storelLabel(TungLabel label, int id)
	{
		Integer templateID = definedClasses.get(label.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(label.getClass(), id);
			writeLabelDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		writeString(label.getText(), index++);
		w.writeFloat(label.getFontSize());
		storePosition(label.getPosition(), -index++);
		storeAngles(label.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeThroughPeg(TungThroughPeg throughPeg, int id)
	{
		Integer templateID = definedClasses.get(throughPeg.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(throughPeg.getClass(), id);
			writeThroughPegDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(throughPeg.getPosition(), -index++);
		storeAngles(throughPeg.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeSnappingPeg(TungSnappingPeg snappingPeg, int id)
	{
		Integer templateID = definedClasses.get(snappingPeg.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(snappingPeg.getClass(), id);
			writeSnappingPegDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(snappingPeg.getPosition(), -index++);
		storeAngles(snappingPeg.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeSwitch(TungSwitch tswitch, int id)
	{
		Integer templateID = definedClasses.get(tswitch.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(tswitch.getClass(), id);
			writeSwitchDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(tswitch.isOn());
		storePosition(tswitch.getPosition(), -index++);
		storeAngles(tswitch.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storePanelSwitch(TungPanelSwitch panelSwitch, int id)
	{
		Integer templateID = definedClasses.get(panelSwitch.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(panelSwitch.getClass(), id);
			writePanelSwitchDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(panelSwitch.isOn());
		storePosition(panelSwitch.getPosition(), -index++);
		storeAngles(panelSwitch.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeDisplay(TungDisplay display, int id)
	{
		Integer templateID = definedClasses.get(display.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(display.getClass(), id);
			writeDisplayDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		writeDisplayColor(display.getColor(), -index++);
		storePosition(display.getPosition(), -index++);
		storeAngles(display.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storePanelDisplay(TungPanelDisplay display, int id)
	{
		Integer templateID = definedClasses.get(display.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(display.getClass(), id);
			writePanelDisplayDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		writeDisplayColor(display.getColor(), -index++);
		storePosition(display.getPosition(), -index++);
		storeAngles(display.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeMount(TungMount mount, int id)
	{
		Integer templateID = definedClasses.get(mount.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(mount.getClass(), id);
			writeMountDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(mount.getPosition(), -index++);
		storeAngles(mount.getAngles(), -index++);
		
		int arrayID = index++;
		writeMemberReference(arrayID);
		queue.add(new ReservedObject(arrayID, mount.getChildren()));
	}
	
	private void storeDelayer(TungDelayer delayer, int id)
	{
		Integer templateID = definedClasses.get(delayer.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(delayer.getClass(), id);
			writeDelayerDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeBoolean(delayer.isOutputOn());
		w.writeInt(delayer.getDelayCount());
		storePosition(delayer.getPosition(), -index++);
		storeAngles(delayer.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storeColorDisplay(TungColorDisplay colorDisplay, int id)
	{
		Integer templateID = definedClasses.get(colorDisplay.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(colorDisplay.getClass(), id);
			writeColorDisplayDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(colorDisplay.getPosition(), -index++);
		storeAngles(colorDisplay.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	private void storePanelColorDisplay(TungPanelColorDisplay panelColorDisplay, int id)
	{
		Integer templateID = definedClasses.get(panelColorDisplay.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(panelColorDisplay.getClass(), id);
			writePanelColorDisplayDefinition(id);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		storePosition(panelColorDisplay.getPosition(), -index++);
		storeAngles(panelColorDisplay.getAngles(), -index++);
		w.writeByte(REC_NULL); //Children...
	}
	
	//###############################################################
	// CREATE PRIMITIVES:
	//###############################################################
	
	private void writeVec3Definition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SerializableVector3");
		
		w.writeInt(3);
		w.writeString("x");
		w.writeString("y");
		w.writeString("z");
		
		w.writeByte(0);
		w.writeByte(0);
		w.writeByte(0);
		
		w.writeByte(11);
		w.writeByte(11);
		w.writeByte(11);
		
		w.writeInt(libID);
	}
	
	private void writeColorDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SerializableColor");
		
		w.writeInt(3);
		w.writeString("r");
		w.writeString("g");
		w.writeString("b");
		
		w.writeByte(0);
		w.writeByte(0);
		w.writeByte(0);
		
		w.writeByte(11);
		w.writeByte(11);
		w.writeByte(11);
		
		w.writeInt(libID);
	}
	
	private void writeBoardDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedCircuitBoard");
		
		w.writeInt(6);
		w.writeString("x");
		w.writeString("z");
		w.writeString("color");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(8);
		w.writeByte(8);
		w.writeString("SerializableColor");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writePegDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedPeg");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeButtonDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedButton");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writePanelButtonDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedPanelButton");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeWireDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedWire");
		
		w.writeInt(5);
		w.writeString("InputInput");
		w.writeString("length");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeByte(11);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeNoisemakerDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedNoisemaker");
		
		w.writeInt(4);
		w.writeString("ToneFrequency");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(11);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeInverterDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedInverter");
		
		w.writeInt(4);
		w.writeString("OutputOn");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeBlotterDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedBlotter");
		
		w.writeInt(4);
		w.writeString("OutputOn");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeThroughBlotterDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedThroughBlotter");
		
		w.writeInt(4);
		w.writeString("OutputOn");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writePanelLabelDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedPanelLabel");
		
		w.writeInt(5);
		w.writeString("text");
		w.writeString("FontSize");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(1);
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(11);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeLabelDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedLabel");
		
		w.writeInt(5);
		w.writeString("text");
		w.writeString("FontSize");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(1);
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(11);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeThroughPegDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedThroughPeg");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeSnappingPegDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedSnappingPeg");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeSwitchDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedSwitch");
		
		w.writeInt(4);
		w.writeString("on");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writePanelSwitchDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedPanelSwitch");
		
		w.writeInt(4);
		w.writeString("on");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeDisplayDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedDisplay");
		
		w.writeInt(4);
		w.writeString("Color");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("DisplayColor");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writePanelDisplayDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedPanelDisplay");
		
		w.writeInt(4);
		w.writeString("Color");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("DisplayColor");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeMountDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedMount");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeDelayerDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedDelayer");
		
		w.writeInt(5);
		w.writeString("OutputOn");
		w.writeString("DelayCount");
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(0);
		w.writeByte(0);
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeByte(1);
		w.writeByte(8);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writeColorDisplayDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedColorDisplay");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	private void writePanelColorDisplayDefinition(int id)
	{
		w.writeByte(REC_DEFs_CLASS);
		w.writeInt(id);
		w.writeString("SavedObjects.SavedPanelColorDisplay");
		
		w.writeInt(3);
		w.writeString("LocalPosition");
		w.writeString("LocalEulerAngles");
		w.writeString("Children");
		
		w.writeByte(4);
		w.writeByte(4);
		w.writeByte(4);
		
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SerializableVector3");
		w.writeInt(libID);
		w.writeString("SavedObjects.SavedObjectV2[]");
		w.writeInt(libID);
		
		w.writeInt(libID);
	}
	
	//###############################################################
	// STUFF:
	//###############################################################
	
	private void writeDisplayColor(TungColorEnum color, int id)
	{
		Integer templateID = definedClasses.get(color.getClass());
		getLibID();
		if(templateID == null)
		{
			definedClasses.put(color.getClass(), id);
			w.writeByte(REC_DEFs_CLASS);
			w.writeInt(id);
			w.writeString("DisplayColor");
			
			w.writeInt(1);
			w.writeString("value__");
			
			w.writeByte(0);
			
			w.writeByte(8);
			
			w.writeInt(libID);
		}
		else
		{
			writeTemplateRecord(id, templateID);
		}
		
		w.writeInt(color.getIndex());
	}
	
	private void writeString(String text, int id)
	{
		w.writeByte(REC_STRING);
		w.writeInt(id);
		w.writeString(text);
	}
	
	private void writeMemberReference(int id)
	{
		w.writeByte(REC_REFERENCE);
		w.writeInt(id);
	}
	
	private void getLibID()
	{
		//Write instantly, since it has to be defined "prior" to current element (thats right before).
		if(libID == null)
		{
			libID = index++;
			//Write Library:
			w.writeByte(REC_LIBRARY);
			w.writeInt(libID); //ID, 2 for some reason.
			w.writeString("Assembly-CSharp, Version=0.0.0.0, Culture=neutral, PublicKeyToken=null");
		}
	}
	
	private void writeTemplateRecord(int id, int templateID)
	{
		w.writeByte(REC_REF_CLASS);
		w.writeInt(id);
		w.writeInt(templateID);
	}
	
	private static class ReservedObject
	{
		private int id;
		private Object object;
		
		public ReservedObject(int id, Object object)
		{
			this.id = id;
			this.object = object;
		}
		
		public int getId()
		{
			return id;
		}
		
		public Object getObject()
		{
			return object;
		}
	}
	
	private static class Vec3
	{
	}
}
