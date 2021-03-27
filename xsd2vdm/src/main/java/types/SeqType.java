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

import org.xml.sax.Locator;

import values.SeqValue;
import values.VDMValue;

public class SeqType extends Type
{
	private final Type itemtype;
	private final int minOccurs;
	
	public SeqType(Type itemtype, int minOccurs)
	{
		this.itemtype = itemtype;
		this.minOccurs = minOccurs;
	}

	@Override
	protected String signature()
	{
		return (minOccurs == 0 ? "seq of (" : "seq1 of (") + itemtype.signature() + ")";
	}
	
	@Override
	public boolean matches(Type type)
	{
		return itemtype.matches(type);
	}

	@Override
	public VDMValue valueOf(String avalue, Locator locator)
	{
		String[] values = avalue.split("\\s+");
		SeqValue result = new SeqValue(itemtype, locator);
		
		for (String value: values)
		{
			result.add(itemtype.valueOf(value, locator));
		}
		
		return result;
	}
}
