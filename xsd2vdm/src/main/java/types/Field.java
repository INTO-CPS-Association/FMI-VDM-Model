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

import java.util.Map;

public class Field
{
	private final String name;
	private final Type type;
	private final boolean optional;
	private final String aggregate;
	
	public Field(String name, Type type, Map<String, String> attributes)
	{
		this.name = name;
		this.type = type;
		
		String minOccurs = attributes.get("minOccurs");
		String maxOccurs = attributes.get("maxOccurs");
		String use = attributes.get("use");
		
		int min = minOccurs == null ? 1 : Integer.parseInt(minOccurs);
		int max = maxOccurs == null ? 1 : maxOccurs.equals("unbounded") ? Integer.MAX_VALUE : Integer.parseInt(maxOccurs);
		
		optional = min == 0 && !"required".equals(use);
		aggregate = min > 1 ? "seq1 of " : max > 1 ? (min == 1 ? "seq1 of " : "seq of ") : ""; 
	}
	
	public String getName()
	{
		return name;
	}

	public String getType()
	{
		if (optional)
		{
			return "[" + aggregate + type.signature() + "]";
		}
		else
		{
			return aggregate + type.signature();
		}
	}
}
