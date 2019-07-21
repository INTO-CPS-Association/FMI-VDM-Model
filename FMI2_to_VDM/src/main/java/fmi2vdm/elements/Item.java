package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Item extends Element
{
	public Item(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
		value = intOf(attributes, "value");
	}
	
	private String name;
	private Integer value;

	@Override
	void toVDM(String indent)
	{
		System.out.print(indent + "mk_Item(\"" + name + "\", " + value + ")");
	}
}
