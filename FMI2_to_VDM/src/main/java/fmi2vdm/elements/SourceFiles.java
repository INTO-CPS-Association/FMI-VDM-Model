package fmi2vdm.elements;

import org.xml.sax.Locator;

public class SourceFiles extends Element
{
	public SourceFiles(Locator locator)
	{
		super(locator);
	}
	
	private ElementList<File> sourceFiles = null;
	
	@Override
	public void add(Element element)
	{
		if (element instanceof File)
		{
			if (sourceFiles == null)
			{
				sourceFiles = new ElementList<File>();
			}
			
			sourceFiles.add(element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	void toVDM(String indent)
	{
		printSequence(indent, sourceFiles, "\n");
	}

	public boolean isEmpty()
	{
		return sourceFiles == null || sourceFiles.isEmpty();
	}
}
