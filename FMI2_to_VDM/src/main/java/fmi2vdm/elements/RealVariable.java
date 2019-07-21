package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class RealVariable extends Variable
{
	public RealVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		declaredType = stringOf(attributes, "declaredType");
		min = doubleOf(attributes, "min");
		max = doubleOf(attributes, "max");
		nominal = doubleOf(attributes, "nominal");
		unbounded = boolOf(attributes, "unbounded");
		start = doubleOf(attributes, "start");
		derivative = intOf(attributes, "derivative");
	}
	
	private String declaredType;
	private Double min;
	private Double max;
	private Double nominal;
	private Boolean unbounded;
	private Double start;
	private Integer derivative;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_Real(");
		
		printStringAttribute("", declaredType, ", ");
		printRawAttribute("", min, ", ");
		printRawAttribute("", max, ", ");
		printRawAttribute("", nominal, ", ");
		printRawAttribute("", unbounded, ", ");
		printRawAttribute("", start, ", ");
		printRawAttribute("", derivative, "");
		
		System.out.print(")");
	}
}
