/**
 * This file is part of the INTO-CPS toolchain.
 *
 * Copyright (c) 2017-2021, INTO-CPS Association,
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

package types;

import java.util.List;

public class Field
{
	private String fieldName;		// The VDM field name, typically lower case
	private String elementName;	// the XML element name for the matching value
	private final Type type;
	
	private boolean isAttribute = false;
	private List<Facet> facets = null;
	
	public Field(String fieldName, String elementName, Type type)
	{
		this.fieldName = fieldName;
		this.elementName = elementName;
		this.type = type;
	}
	
	public String getFieldName()
	{
		return fieldName;
	}
	
	public String getElementName()
	{
		return elementName;
	}
	
	public Type getType()
	{
		return type;
	}
	
	/**
	 * Get the final type, depending on whether the field is aggregated and optional.
	 */
	public Type getFieldType()
	{
		Type agg = null;
		
		switch (type.aggregateType())
		{
			case 0:
				agg = type;
				break;
				
			case 1:
				agg = new SeqType(type, 0);
				break;

			case 2:
				agg = new SeqType(type, 1);
				break;
		}
		
		return type.isOptional() ? new OptionalType(agg) : agg;
	}

	public List<Facet> getFacets()
	{
		return facets;
	}

	public boolean isAttribute()
	{
		return isAttribute;
	}

	@Override
	public String toString()
	{
		return fieldName + " : " + getFieldType();
	}

	public void setIsAttribute(boolean isAttribute)
	{
		this.isAttribute = isAttribute;
	}
	
	public void setFacets(List<Facet> facets)
	{
		this.facets = facets;
	}

	public boolean isSequence()
	{
		return type.isSequence();
	}

	public void setNames(String fieldName, String elementName)
	{
		this.fieldName = fieldName;
		this.elementName = elementName;
	}
}
