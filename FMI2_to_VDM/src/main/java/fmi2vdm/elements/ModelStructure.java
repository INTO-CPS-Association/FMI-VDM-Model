package fmi2vdm.elements;

import org.xml.sax.Locator;


public class ModelStructure extends Element
{
	public ModelStructure(Locator locator)
	{
		super(locator);
	}
	
	private Unknowns outputs;
	private Unknowns derivatives;
	private Unknowns initialUnknowns;

	@Override
	public void add(Element element)
	{
		if (element instanceof Unknowns)
		{
			Unknowns u = (Unknowns) element;
			
			switch (u.kind)
			{
				case "Outputs":			
					outputs = (Unknowns) element;
					break;
					
				case "Derivatives":
					derivatives = (Unknowns) element;
					break;
					
				case "InitialUnknowns":
					initialUnknowns = (Unknowns) element;
					break;
					
				default:
					System.err.println("Unknown ModelStructure type: " + u.kind);
					System.exit(1);
			}
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_ModelStructure");
		System.out.println(indent + "(");
		
		if (outputs != null)
		{
			System.out.println(indent + "\t-- Outputs");
			outputs.toVDM(indent);
			System.out.println(",\n");
		}
		else
		{
			System.out.println(indent + "\tnil,");
		}
		
		if (derivatives != null)
		{
			System.out.println(indent + "\t-- Derivatives");
			derivatives.toVDM(indent);
			System.out.println(",\n");
		}
		else
		{
			System.out.println(indent + "\tnil,");
		}
		
		if (initialUnknowns != null)
		{
			System.out.println(indent + "\t-- InitialUnknowns");
			initialUnknowns.toVDM(indent);
			System.out.println();
		}
		else
		{
			System.out.println(indent + "\tnil");
		}
		
		System.out.println(indent + ")");
	}
}
