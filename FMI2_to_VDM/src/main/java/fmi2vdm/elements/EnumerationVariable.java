package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class EnumerationVariable extends Variable
{
	public EnumerationVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		declaredType = stringOf(attributes, "declaredType");
		min = doubleOf(attributes, "min");
		max = doubleOf(attributes, "max");
		start = doubleOf(attributes, "start");
	}
	
	private String declaredType;
	private Double min;
	private Double max;
	private Double start;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_Enumeration(");
		
		printStringAttribute("", declaredType, ", ");
		printRawAttribute("", min, ", ");
		printRawAttribute("", max, ", ");
		printRawAttribute("", start, "");
		
		System.out.print(")");
	}
}
