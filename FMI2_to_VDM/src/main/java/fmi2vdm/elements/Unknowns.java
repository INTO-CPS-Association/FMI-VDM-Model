package fmi2vdm.elements;

import org.xml.sax.Locator;

public class Unknowns extends Element
{
	public Unknowns(String kind, Locator locator)
	{
		super(locator);
		this.kind = kind;
	}
	
	public final String kind;
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Unknown)
		{
			if (unknowns == null)
			{
				unknowns = new ElementList<Unknown>();
			}
			
			unknowns.add(element);
		}
		else
		{
			super.add(element);
		}
	}
	
	private ElementList<Unknown> unknowns = null;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "\t-- Line " + lineNumber);
		printSequence(indent + "\t", unknowns, "");
	}
}
