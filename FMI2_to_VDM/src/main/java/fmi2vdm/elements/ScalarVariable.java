package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class ScalarVariable extends Element
{
	public ScalarVariable(Attributes attributes, Locator locator)
	{
		super(locator);
		
		name = stringOf(attributes, "name");
		causality = stringOf(attributes, "causality");
		variability = stringOf(attributes, "variability");
		initial = stringOf(attributes, "initial");
		canHandleMultipleSetPerTimeInstant = boolOf(attributes, "canHandleMultipleSetPerTimeInstant");
	}
	
	@Override
	public void add(Element element)
	{
		if (element instanceof Variable)
		{
			variable = (Variable) element;
		}
		else if (element instanceof VendorAnnotations)
		{
			annotations = (VendorAnnotations) element;
		}
		else
		{
			super.add(element);
		}
	}
	
	private String name;
	private String causality;
	private String variability;
	private String initial;
	private Boolean canHandleMultipleSetPerTimeInstant;
	private Variable variable;
	private VendorAnnotations annotations;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.println(indent + "mk_ScalarVariable");
		System.out.println(indent + "(");
		printStringAttribute(indent + "\t", name, ",\n");
		printQuoteAttribute(indent + "\t", causality, ",\n");
		printQuoteAttribute(indent + "\t", variability, ",\n");
		printQuoteAttribute(indent + "\t", initial, ",\n");
		printRawAttribute(indent + "\t", canHandleMultipleSetPerTimeInstant, ",\n");
		printOptional(indent + "\t", variable, ",\n");
		printOptional(indent + "\t", annotations, "\n");
		System.out.print(indent + ")");
	}
}
