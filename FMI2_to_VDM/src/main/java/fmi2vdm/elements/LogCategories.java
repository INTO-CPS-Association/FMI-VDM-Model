package fmi2vdm.elements;

import org.xml.sax.Locator;

public class LogCategories extends Element
{
	public LogCategories(Locator locator)
	{
		super(locator);
	}
	
	private ElementList<Category> logCategories = null;
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Category)
		{
			if (logCategories == null)
			{
				logCategories = new ElementList<Category>();
			}
			
			logCategories.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	void toVDM(String indent)
	{
		printSequence(indent, logCategories, "");
	}
}
