package fmi2vdm.elements;

import java.util.Vector;

public class ElementList<T extends Element> extends Vector<T>
{
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean add(Element item)
	{
		return super.add((T) item);
	}
}
