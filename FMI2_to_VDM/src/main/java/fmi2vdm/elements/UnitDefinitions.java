package fmi2vdm.elements;

import org.xml.sax.Locator;

public class UnitDefinitions extends Element
{
	public UnitDefinitions(Locator locator)
	{
		super(locator);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Unit)
		{
			if (unitDefinitions == null)
			{
				unitDefinitions = new ElementList<Unit>();
			}
			
			unitDefinitions.add(element);
		}
		else
		{
			super.add(element);
		}
	}
	
	private ElementList<Unit> unitDefinitions = null;

	@Override
	void toVDM(String indent)
	{
		printSequence(indent, unitDefinitions, "");
	}
}
