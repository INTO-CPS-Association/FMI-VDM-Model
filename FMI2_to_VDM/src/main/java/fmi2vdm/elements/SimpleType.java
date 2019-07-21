package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class SimpleType extends Element
{
	public SimpleType(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof Type)
		{
			type = (Type) element;
		}
		else
		{
			super.add(element);
		}
	}
	
	private String name;
	private Type type;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_SimpleType(");
		printStringAttribute(indent + "\t", name, ",\n");
		type.toVDM(indent + "\t");
		System.out.print("\n" + indent + ")");
	}
}
