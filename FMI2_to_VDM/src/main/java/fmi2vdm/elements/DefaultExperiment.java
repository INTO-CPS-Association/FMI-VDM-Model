package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class DefaultExperiment extends Element
{
	public DefaultExperiment(Attributes attributes, Locator locator)
	{
		super(locator);
		
		startTime = doubleOf(attributes, "startTime");
		stopTime = doubleOf(attributes, "stopTime");
		tolerance = doubleOf(attributes, "tolerance");
		stepSize = doubleOf(attributes, "stepSize");
	}
	
	private Double startTime;
	private Double stopTime;
	private Double tolerance;
	private Double stepSize;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_DefaultExperiment(");
		printRawAttribute("", startTime, ", ");
		printRawAttribute("", stopTime, ", ");
		printRawAttribute("", tolerance, ", ");
		printRawAttribute("", stepSize, "");
		System.out.print(")");
	}
}
