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
	private final String fieldName;		// The VDM field name, typically lower case
	private final String elementName;	// the XML element name for the matching value
	private final Type type;
	private final boolean optional;
	private final String aggregate;
	
	private boolean isAttribute = false;
	private List<String> comments = null;
	private List<Facet> facets = null;
	
	public Field(String fieldName, String elementName, Type type, boolean optional, String aggregate)
	{
		this.fieldName = fieldName;
		this.elementName = elementName;
		this.type = type;
		this.optional = optional;
		this.aggregate = aggregate;
	}
	
	public Field(String fieldName, String elementName, Type type)
	{
		this.fieldName = fieldName;
		this.elementName = elementName;
		this.type = type;
		this.optional = false;
		this.aggregate = "";
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
	
	public Type getVDMType()
	{
		Type agg = null;
		
		switch (aggregate)
		{
			case "":
				agg = type;
				break;
				
			case "seq of ":
				agg = new SeqType(type, 0);
				break;

			case "seq1 of ":
				agg = new SeqType(type, 1);
				break;
		}
		
		return optional ? new OptionalType(agg) : agg;
	}

	public String getAggregate()
	{
		return aggregate;
	}
	
	public List<String> getComments()
	{
		return comments;
	}
	
	public List<Facet> getFacets()
	{
		return facets;
	}

	public boolean isOptional()
	{
		return optional;
	}

	public boolean isAttribute()
	{
		return isAttribute;
	}

	public boolean isSequence()
	{
		return !aggregate.isEmpty();
	}

	@Override
	public String toString()
	{
		return fieldName + " : " + getVDMType();
	}

	public void setIsAttribute(boolean isAttribute)
	{
		this.isAttribute = isAttribute;
	}
	
	public void setComments(CommentField annotation)
	{
		if (annotation != null)
		{
			this.comments = annotation.getComments();
		}
	}
	
	public void setFacets(List<Facet> facets)
	{
		this.facets = facets;
	}

	public Field renamed(String fieldName, String elementName)
	{
		Field result = new Field(fieldName, elementName, type, optional, aggregate);
		result.isAttribute = isAttribute;
		result.comments = comments;
		result.facets = facets;
		return result;
	}

	public Field reaggregate(String aggregate)
	{
		Field result = new Field(fieldName, elementName, type, optional, aggregate);
		result.isAttribute = isAttribute;
		result.comments = comments;
		result.facets = facets;
		return result;
	}
}
