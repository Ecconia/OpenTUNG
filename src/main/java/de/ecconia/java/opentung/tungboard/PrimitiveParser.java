package de.ecconia.java.opentung.tungboard;

import de.ecconia.java.opentung.tungboard.netremoting.NRFile;
import de.ecconia.java.opentung.tungboard.netremoting.NRParser;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRClass;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRObject;
import de.ecconia.java.opentung.tungboard.tungobjects.TungBoard;
import de.ecconia.java.opentung.tungboard.tungobjects.TungPeg;
import de.ecconia.java.opentung.tungboard.tungobjects.TungWire;
import de.ecconia.java.opentung.tungboard.tungobjects.common.TungChildable;
import de.ecconia.java.opentung.tungboard.tungobjects.meta.TungObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;

public class PrimitiveParser
{
	public static void main(String[] args)
	{
//		new Exporter(new File("boards/output.tungboard"), importTungBoard("boards/16Bit-Paralell-CLA-ALU.tungboard"));
		highlightNormals("boards/Highlighted.tungboard", "boards/16Bit-Paralell-CLA-ALU.tungboard");
	}
	
	public static TungBoard importTungBoard(Path file)
	{
		NRFile pf = NRParser.parse(file);
		
		NRObject object = pf.getRootElements().get(0);
		NRClass firstClass;
		if(object instanceof NRClass)
		{
			firstClass = (NRClass) object;
		}
		else
		{
			throw new RuntimeException("Unknown first object: " + object.getClass().getSimpleName());
		}
		
		if(TungBoard.NAME.equals(firstClass.getName()))
		{
			TungBoard board = new TungBoard(firstClass);
			
			//Fixer:
//			fix(board);
			
			return board;
		}
		else
		{
			throw new RuntimeException("First Class has wrong type: " + firstClass.getName());
		}
	}
	
	private static void highlightNormals(String out, String in)
	{
		TungBoard board = importTungBoard(Paths.get(in));
		highlightNormals(board);
		new Exporter(Paths.get(out), board);
	}
	
	private static void highlightNormals(TungChildable childable)
	{
		List<TungObject> children = childable.getChildren();
		Iterator<TungObject> filterIterator = children.iterator();
		while(filterIterator.hasNext())
		{
			if(!(filterIterator.next() instanceof TungChildable))
			{
				filterIterator.remove();
			}
		}
		
		for(TungObject o : children)
		{
			TungChildable child = (TungChildable) o;
			highlightNormals(child);
		}
		
		childable.addChildren(new TungPeg());
	}
	
	private static void fix(TungChildable holder)
	{
		for(TungObject to : holder.getChildren())
		{
			if(!(to instanceof TungWire))
			{
				to.getPosition().fix();
				to.getAngles().fix();
			}
			
			if(to instanceof TungChildable)
			{
				fix((TungChildable) to);
			}
		}
	}
}
