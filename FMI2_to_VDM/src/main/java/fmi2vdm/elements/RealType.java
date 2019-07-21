package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class RealType extends Type
{
	public RealType(Attributes attributes, Locator locator)
	{
		super(locator);
		
		min = doubleOf(attributes, "min");
		max = doubleOf(attributes, "max");
		nominal = doubleOf(attributes, "nominal");
		unbounded = boolOf(attributes, "unbounded");
	}
	
	private Double min;
	private Double max;
	private Double nominal;
	private Boolean unbounded;

	@Override
	void toVDM(String indent)
	{
		System.out.print(indent + "mk_RealType(");
		
		printRawAttribute("", min, ", ");
		printRawAttribute("", max, ", ");
		printRawAttribute("", nominal, ", ");
		printRawAttribute("", unbounded, "");
		
		System.out.print(")");
	}
}
