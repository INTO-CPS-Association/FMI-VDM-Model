package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class BooleanVariable extends Variable
{
	public BooleanVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		declaredType = stringOf(attributes, "declaredType");
		start = boolOf(attributes, "start");
	}
	
	private String declaredType;
	private Boolean start;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_Boolean(");
		
		printStringAttribute("", declaredType, ", ");
		printRawAttribute("", start, "");
		
		System.out.print(")");
	}
}
