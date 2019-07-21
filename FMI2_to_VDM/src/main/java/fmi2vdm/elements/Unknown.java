package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class Unknown extends Element
{
	public Unknown(Attributes attributes, Locator locator)
	{
		super(locator);
		
		index = intOf(attributes, "index");
		String[] deps = arrayOf(stringOf(attributes, "dependencies"));
		
		if (deps == null)
		{
			dependencies = null;
		}
		else
		{
			dependencies = new int[deps.length];
			for (int i=0; i<deps.length; i++)
			{
				dependencies[i] = Integer.parseInt(deps[i]);
			}
		}

		dependenciesKind = arrayOf(stringOf(attributes, "dependenciesKind"));
	}
	
	private String[] arrayOf(String value)
	{
		if (value == null)
		{
			return null;
		}
		else
		{
			return value.split("\\s+");
		}
	}

	private Integer index;
	private int[] dependencies;
	private String[] dependenciesKind;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_Unknown");
		System.out.println(indent + "(");
		printRawAttribute(indent + "\t", index, ",\n");
		
		if (dependencies == null)
		{
			System.out.println(indent + "\tnil,");
		}
		else
		{
			System.out.print(indent + "\t[");
			String sep = "";
			
			for (Integer d: dependencies)
			{
				System.out.print(sep + d);
				sep = ", ";
			}
			
			System.out.println("],");
		}
		
		if (dependenciesKind == null)
		{
			System.out.println(indent + "\tnil");
		}
		else
		{
			System.out.print(indent + "\t[");
			String sep = "";
			
			for (String dk: dependenciesKind)
			{
				System.out.print(sep + "<" + dk + ">");
				sep = ", ";
			}
			
			System.out.println("]");
		}

		System.out.print(indent + ")");
	}
}
