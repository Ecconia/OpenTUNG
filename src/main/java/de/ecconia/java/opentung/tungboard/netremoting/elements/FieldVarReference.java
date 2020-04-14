package de.ecconia.java.opentung.tungboard.netremoting.elements;

import de.ecconia.java.opentung.tungboard.netremoting.ParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.ParsedFile;

public class FieldVarReference extends Object
{
	private final int refId;
	private final ResolveCall callable;
	
	public FieldVarReference(ParseBundle b, ResolveCall callable)
	{
		this.callable = callable;
		
		refId = b.sInt();
		b.addResolver(this);
		
//		System.out.println("Resolver: To: " + refId);
	}
	
	public void resolve(ParsedFile file)
	{
		callable.onCall(file.getObject(refId));
	}
	
	public interface ResolveCall
	{
		void onCall(Object object);
	}
}
