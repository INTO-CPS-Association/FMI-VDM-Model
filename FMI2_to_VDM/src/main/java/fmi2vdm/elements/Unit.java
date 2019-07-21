package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Unit extends Element
{
	public Unit(Attributes attributes, Locator locator)
	{
		super(locator);
		lineNumber = locator.getLineNumber();
		name = stringOf(attributes, "name");
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof BaseUnit)
		{
			baseUnit = (BaseUnit) element;
		}
		else if (element instanceof DisplayUnit)
		{
			if (displayUnits == null)
			{
				displayUnits = new ElementList<DisplayUnit>();
			}
			
			displayUnits.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	private String name;
	private BaseUnit baseUnit = null;
	private ElementList<DisplayUnit> displayUnits = null;
	
	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_Unit");
		System.out.println(indent + "(");
		printStringAttribute(indent + "\t", name, ",\n");
		printOptional(indent + "\t", baseUnit, ",\n");
		printSequence(indent + "\t", displayUnits, "\n");
		System.out.print(indent + ")");
	}
}
