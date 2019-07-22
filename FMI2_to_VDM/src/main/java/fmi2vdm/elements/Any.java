package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Any extends Element
{
	private String text;

	public Any(String qName, Attributes attributes, Locator locator)
	{
		super(locator);
		
		StringBuilder sb = new StringBuilder();
		sb.append(qName);
		
		for (int i=0; i<attributes.getLength(); i++)
		{
			sb.append(",");
			sb.append(attributes.getQName(i));
			sb.append("=");
			sb.append(attributes.getValue(i));
		}
		
		text = sb.toString();
	}
	
	@Override
	public void add(Element element)
	{
		// Fine - we can add anything to an "any".
		text = text + ";" + element.toString();
	}
	
	@Override
	public String toString()
	{
		return text;
	}

	@Override
	void toVDM(String indent)
	{
		// Ignore
	}
}
