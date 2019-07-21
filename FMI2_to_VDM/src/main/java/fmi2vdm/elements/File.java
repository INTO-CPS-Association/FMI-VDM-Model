package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class File extends Element
{
	public File(Attributes attributes, Locator locator)
	{
		super(locator);
		name = stringOf(attributes, "name");
	}

	private String name;
	
	@Override
	void toVDM(String indent)
	{
		System.out.print(indent + "mk_SourceFile(\"" + name + "\")");
	}
}
