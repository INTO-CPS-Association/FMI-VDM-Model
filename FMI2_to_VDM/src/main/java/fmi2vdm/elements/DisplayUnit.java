package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class DisplayUnit extends Element
{
	public DisplayUnit(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
		factor = doubleOf(attributes, "factor");
		offset = doubleOf(attributes, "offset");
	}

	private String name;
	private Double factor;
	private Double offset;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_DisplayUnit(");
		printStringAttribute("", name, ", ");
		printRawAttribute("", factor, ", ");
		printRawAttribute("", offset, "");
		System.out.print(")");
	}
}
