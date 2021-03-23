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

package xsd2vdm;

import java.util.LinkedHashMap;
import java.util.Map;

import org.xml.sax.Locator;

import types.Field;
import types.Record;

public class VDMValue
{
	private final Record type;
	private final Map<String, String> source = new LinkedHashMap<String, String>();
	
	public VDMValue(Record type, Locator locator)
	{
		this.type = type;
	}

	public boolean setField(String name, String value)
	{
		boolean found = false;
		
		for (Field f: type.getFields())
		{
			if (f.getName().equalsIgnoreCase(name))
			{
				source.put(name, value);
				found = true;
				break;
			}
		}

		return found;
	}

	public boolean setField(String qName, VDMValue value)
	{
		boolean found = false;
		
		for (Field f: type.getFields())
		{
			if (f.getType().matches(value.type))
			{
				source.put(qName, value.toString());
				found = true;
				break;
			}
		}

		return found;
	}

	@Override
	public String toString()
	{
		return type.getName() + " :: " + source.toString();
	}
}
