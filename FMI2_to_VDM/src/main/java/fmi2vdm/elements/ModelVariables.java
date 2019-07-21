package fmi2vdm.elements;

import org.xml.sax.Locator;


public class ModelVariables extends Element
{
	public ModelVariables(Locator locator)
	{
		super(locator);
	}
	
	private ElementList<ScalarVariable> modelVariables = null;
	
	@Override
	public void add(Element element)
	{
		if (element instanceof ScalarVariable)
		{
			if (modelVariables == null)
			{
				modelVariables = new ElementList<ScalarVariable>();
			}
			
			modelVariables.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	void toVDM(String indent)
	{
		printSequence(indent, modelVariables, "");
	}
}
