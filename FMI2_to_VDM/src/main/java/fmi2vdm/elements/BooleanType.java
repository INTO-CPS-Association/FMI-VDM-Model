package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class BooleanType extends Type
{
	public BooleanType(Attributes attributes, Locator locator)
	{
		super(locator);
	}
	
	@Override
	void toVDM(String indent)
	{
		System.out.print(indent + "mk_BooleanType()");
	}
}
