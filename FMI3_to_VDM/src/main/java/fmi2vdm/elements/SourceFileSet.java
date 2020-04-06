/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2019, INTO-CPS Association,
 * c/o Professor Peter Gorm Larsen, Department of Engineering
 * Finlandsgade 22, 8200 Aarhus N.
 *
 * All rights reserved.
 *
 * THIS PROGRAM IS PROVIDED UNDER THE TERMS OF GPL VERSION 3 LICENSE OR
 * THIS INTO-CPS ASSOCIATION PUBLIC LICENSE VERSION 1.0.
 * ANY USE, REPRODUCTION OR DISTRIBUTION OF THIS PROGRAM CONSTITUTES
 * RECIPIENT'S ACCEPTANCE OF THE OSMC PUBLIC LICENSE OR THE GPL 
 * VERSION 3, ACCORDING TO RECIPIENTS CHOICE.
 *
 * The INTO-CPS toolchain  and the INTO-CPS Association Public License are
 * obtained from the INTO-CPS Association, either from the above address, from
 * the URLs: http://www.into-cps.org, and in the INTO-CPS toolchain distribution.
 * GNU version 3 is obtained from: http://www.gnu.org/copyleft/gpl.html.
 *
 * This program is distributed WITHOUT ANY WARRANTY; without
 * even the implied warranty of  MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE, EXCEPT AS EXPRESSLY SET FORTH IN THE
 * BY RECIPIENT SELECTED SUBSIDIARY LICENSE CONDITIONS OF
 * THE INTO-CPS ASSOCIATION.
 *
 * See the full INTO-CPS Association Public License conditions for more details.
 */

package fmi2vdm.elements;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

public class SourceFileSet extends Element
{
	private String language;
	private String compiler;
	private String compilerOptions;
	private ElementList<SourceFile> sourceFiles;
	private ElementList<PreprocessorDefinition> preprocessorDefinitions;
	private ElementList<IncludeDirectory> includeDirectories;

	public SourceFileSet(Attributes attributes, Locator locator)
	{
		super(locator);
		setAttributes(attributes);
	}

	@Override
	public void add(Element element)
	{
		if (element instanceof SourceFile)
		{
			if (sourceFiles == null)
			{
				sourceFiles = new ElementList<SourceFile>();
			}

			sourceFiles.add((SourceFile) element);
		}
		else if (element instanceof PreprocessorDefinition)
		{
			if (preprocessorDefinitions == null)
			{
				preprocessorDefinitions = new ElementList<PreprocessorDefinition>();
			}

			preprocessorDefinitions.add((PreprocessorDefinition) element);
		}
		else if (element instanceof IncludeDirectory)
		{
			if (includeDirectories == null)
			{
				includeDirectories = new ElementList<IncludeDirectory>();
			}

			includeDirectories.add((IncludeDirectory) element);
		}
		else
		{
			super.add(element);
		}
	}

	@Override
	public void toVDM(String indent)
	{
		System.out.println(indent + "mk_SourceFileSet");
		System.out.println(indent + "(");
		System.out.println(indent + "\t" + lineNumber + ",  -- Line");
		printStringAttribute(indent + "\t", language, ",\n");
		printStringAttribute(indent + "\t", compiler, ",\n");
		printStringAttribute(indent + "\t", compilerOptions, ",\n");

		printOptional(indent + "\t", sourceFiles, ",\n");
		printOptional(indent + "\t", preprocessorDefinitions, ",\n");
		printOptional(indent + "\t", includeDirectories, "\n");
		
		System.out.print(indent + ")");
	}
}
