package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class BaseUnit extends Element
{
	public BaseUnit(Attributes attributes, Locator locator)
	{
		super(locator);
		
		kg = intOf(attributes, "kg");
		m = intOf(attributes, "m");
		s = intOf(attributes, "s");
		A = intOf(attributes, "A");
		K = intOf(attributes, "K");
		mol = intOf(attributes, "mol");
		cs = intOf(attributes, "cs");
		rad = intOf(attributes, "rad");
		factor = doubleOf(attributes, "factor");
		offset = doubleOf(attributes, "offset");
	}

	private Integer kg;
	private Integer m;
	private Integer s;
	private Integer A;
	private Integer K;
	private Integer mol;
	private Integer cs;
	private Integer rad;
	private Double factor;
	private Double offset;

	@Override
	void toVDM(String indent)
	{
		System.out.println(indent + "-- Line " + lineNumber);
		System.out.print(indent + "mk_BaseUnit(");
		printRawAttribute("", kg, ", ");
		printRawAttribute("", m, ", ");
		printRawAttribute("", s, ", ");
		printRawAttribute("", A, ", ");
		printRawAttribute("", K, ", ");
		printRawAttribute("", mol, ", ");
		printRawAttribute("", cs, ", ");
		printRawAttribute("", rad, ", ");
		printRawAttribute("", factor, ", ");
		printRawAttribute("", offset, "");
		System.out.print(")");
	}
}
