package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class IntegerType extends Type
{
	public IntegerType(Attributes attributes, Locator locator)
	{
		super(locator);
		
		min = intOf(attributes, "min");
		max = intOf(attributes, "max");
	}
	
	private Integer min;
	private Integer max;

	@Override
	void toVDM(String indent)
	{
		System.out.print(indent + "mk_IntegerType(");
		
		printRawAttribute("", min, ", ");
		printRawAttribute("", max, "");
		
		System.out.print(")");
	}
}
