package fmi2vdm.elements;

import org.xml.sax.Locator;

public class TypeDefinitions extends Element
{
	public TypeDefinitions(Locator locator)
	{
		super(locator);
	}
	
	private ElementList<SimpleType> typeDefinitions = null;
	
	@Override
	public void add(Element element)
	{
		if (element instanceof SimpleType)
		{
			if (typeDefinitions == null)
			{
				typeDefinitions = new ElementList<SimpleType>();
			}
			
			typeDefinitions.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	void toVDM(String indent)
	{
		printSet(indent, typeDefinitions, "");
	}
}
