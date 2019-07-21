package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class EnumerationType extends Type
{
	public EnumerationType(Attributes attributes, Locator locator)
	{
		super(locator);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Item)
		{
			if (items == null)
			{
				items = new ElementList<Item>();
			}
			
			items.add(element);
		}
		else
		{
			super.add(element);
		}
	}
	
	private ElementList<Item> items;
	
	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "mk_EnumerationType(");
		printSequence(indent + "\t", items, "\n");
		System.out.println(indent + ")");
	}
}
