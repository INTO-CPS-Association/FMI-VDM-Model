package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Category extends Element
{
	public Category(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
		description = stringOf(attributes, "description");
	}
	
	private String name;
	private String description;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_Category(");
		printStringAttribute("", name, ", ");
		printStringAttribute("", description, "");
		System.out.print(")");
	}
}
