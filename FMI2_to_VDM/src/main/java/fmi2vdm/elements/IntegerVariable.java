package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class IntegerVariable extends Variable
{
	public IntegerVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		declaredType = stringOf(attributes, "declaredType");
		min = intOf(attributes, "min");
		max = intOf(attributes, "max");
		start = intOf(attributes, "start");
	}
	
	private String declaredType;
	private Integer min;
	private Integer max;
	private Integer start;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_Integer(");
		
		printStringAttribute("", declaredType, ", ");
		printRawAttribute("", min, ", ");
		printRawAttribute("", max, ", ");
		printRawAttribute("", start, "");
		
		System.out.print(")");
	}
}
