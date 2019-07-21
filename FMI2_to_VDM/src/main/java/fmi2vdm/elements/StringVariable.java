package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class StringVariable extends Variable
{
	public StringVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		declaredType = stringOf(attributes, "declaredType");
		start = stringOf(attributes, "start");
	}
	
	private String declaredType;
	private String start;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_String(");
		
		printStringAttribute("", declaredType, ", ");
		printRawAttribute("", start, "");
		
		System.out.print(")");
	}
}
