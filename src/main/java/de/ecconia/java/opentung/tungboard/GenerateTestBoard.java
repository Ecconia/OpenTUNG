package de.ecconia.java.opentung.tungboard;

import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;
import de.ecconia.java.opentung.tungboard.tungobjects.TungInverter;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungAngles;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungColor;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungPosition;
import java.io.File;

public class GenerateTestBoard
{
	public static void main(String[] args)
	{
		TungBoard board = new TungBoard(1, 1);
		board.setPosition(new TungPosition(0, 0, 0));
		board.setAngles(new TungAngles(0, 0, 0));
		
		TungBoard boardX = new TungBoard(1, 1, new TungColor(1, 0, 0));
		boardX.setPosition(new TungPosition(0.3f, 0.0f, 0.0f));
		boardX.setAngles(new TungAngles(0, 0, 0));
		
		TungBoard boardY = new TungBoard(1, 1, new TungColor(0, 1, 0));
		boardY.setPosition(new TungPosition(0.0f, 0.15f, 0.0f));
		boardY.setAngles(new TungAngles(0, 0, 0));
		
		TungBoard boardZ = new TungBoard(1, 1, new TungColor(0, 0, 1));
		boardZ.setPosition(new TungPosition(0.0f, 0.0f, 0.3f));
		boardZ.setAngles(new TungAngles(0, 0, 0));
		
		TungInverter inverter = new TungInverter();
		inverter.setPosition(new TungPosition(1.0f, 0.6f, 0.0f));
		inverter.setAngles(new TungAngles(0, 0, 0));
		board.addChildren(inverter);
		
		inverter = new TungInverter();
		inverter.setPosition(new TungPosition(2.0f, 0.6f, 0.0f));
		inverter.setAngles(new TungAngles(90, 0, 0));
		board.addChildren(inverter);
		
		inverter = new TungInverter();
		inverter.setPosition(new TungPosition(3.0f, 0.6f, 0.0f));
		inverter.setAngles(new TungAngles(0, 90, 0));
		board.addChildren(inverter);
		
		inverter = new TungInverter();
		inverter.setPosition(new TungPosition(4.0f, 0.6f, 0.0f));
		inverter.setAngles(new TungAngles(0, 0, 90));
		board.addChildren(inverter);
		
		inverter = new TungInverter();
		inverter.setPosition(new TungPosition(2.0f, 0.6f, 1.0f));
		inverter.setAngles(new TungAngles(90, 90, 0));
		board.addChildren(inverter);
		
		inverter = new TungInverter();
		inverter.setPosition(new TungPosition(3.0f, 0.6f, 1.0f));
		inverter.setAngles(new TungAngles(90, 0, 90));
		board.addChildren(inverter);
		
		inverter = new TungInverter();
		inverter.setPosition(new TungPosition(4.0f, 0.6f, 1.0f));
		inverter.setAngles(new TungAngles(0, 90, 90));
		board.addChildren(inverter);
		
		board.addChildren(boardX);
		board.addChildren(boardY);
		board.addChildren(boardZ);
		
		new Exporter(new File("boards/0OneOne.tungboard"), board);
	}
}
