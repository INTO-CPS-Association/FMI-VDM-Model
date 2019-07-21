package fmi2vdm.elements;

import org.xml.sax.Locator;

abstract class Variable extends Element
{
	protected Variable(Locator locator)
	{
		super(locator);
	}
}
