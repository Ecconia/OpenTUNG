package de.ecconia.java.opentung.tungboard.netremoting.elements.fields;

import de.ecconia.java.opentung.tungboard.netremoting.NRParseBundle;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRField;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRFieldVarReference;
import de.ecconia.java.opentung.tungboard.netremoting.elements.NRObject;

public class NRObjectReferenceField extends NRField
{
	private NRObject value;
	private final boolean asDataExtra;
	
	public NRObjectReferenceField(boolean asDataExtra)
	{
		this.asDataExtra = asDataExtra;
	}
	
	@Override
	public NRField copy()
	{
		NRField field = new NRObjectReferenceField(asDataExtra);
		field.setName(getName());
		return field;
	}
	
	@Override
	public void parseContent(NRParseBundle b)
	{
		if(asDataExtra)
		{
			int recordTag = b.uByte();
			if(recordTag == 9)
			{
				value = new NRFieldVarReference(b, (NRObject object) -> {
					this.value = object;
				});
			}
			else
			{
				throw new RuntimeException("Expected Object reference entry, but got TAG: " + recordTag);
			}
		}
		else
		{
			value = new NRFieldVarReference(b, (NRObject object) -> {
				this.value = object;
			});
		}
	}
}
