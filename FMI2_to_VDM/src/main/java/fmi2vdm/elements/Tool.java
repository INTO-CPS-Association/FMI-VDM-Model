package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Tool extends Element
{
	public Tool(Attributes attributes, Locator locator)
	{
		super(locator);
		name = stringOf(attributes, "name");
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Any)
		{
			if (annotation == null)
			{
				annotation = element.toString();
			}
			else
			{
				annotation = annotation + ";" + element.toString();
			}
		}
		else
		{
			super.add(element);
		}
	}
	
	private String name;
	private String annotation;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_Tool(");
		printStringAttribute("", name, ", ");
		System.out.print("mk_token(\"" + annotation + "\")");
		System.out.print(")");
	}

	public void setAnnotation(String annotation)
	{
		this.annotation = annotation;
	}
}
