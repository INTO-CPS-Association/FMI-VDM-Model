/******************************************************************************
 *
 *	Copyright (c) 2017-2022, INTO-CPS Association,
 *	c/o Professor Peter Gorm Larsen, Department of Engineering
 *	Finlandsgade 22, 8200 Aarhus N.
 *
 *	This file is part of the INTO-CPS toolchain.
 *
 *	xsd2vdm is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	xsd2vdm is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with xsd2vdm. If not, see <http://www.gnu.org/licenses/>.
 *	SPDX-License-Identifier: GPL-3.0-or-later
 *
 ******************************************************************************/

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
