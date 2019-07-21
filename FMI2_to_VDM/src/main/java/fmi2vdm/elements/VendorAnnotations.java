package fmi2vdm.elements;

import org.xml.sax.Locator;

public class VendorAnnotations extends Element
{
	public VendorAnnotations(Locator locator)
	{
		super(locator);
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Tool)
		{
			if (annotations == null)
			{
				annotations = new ElementList<Tool>();
			}
			
			annotations.add(element);
		}
	}
	
	ElementList<Tool> annotations = null;

	@Override
	void toVDM(String indent)
	{
		printSequence(indent, annotations, "");
	}
}
