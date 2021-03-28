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
package values;

import org.xml.sax.Attributes;
import org.xml.sax.Locator;

import types.BasicType;

public class AnyValue extends VDMValue
{
	private String token;
	
	public AnyValue(String qName, Attributes attributes, Locator locator)
	{
		super(new BasicType("token"), locator);
		
		StringBuilder sb = new StringBuilder();
		String sep = "";
		sb.append(qName);
		
		for (int i=0; i<attributes.getLength(); i++)
		{
			sb.append(sep);
			sb.append(attributes.getType(i));
			sb.append("=");
			sb.append(attributes.getValue(i));
			sep = " ";
		}
		
		this.token = sb.toString();
	}
	
	@Override
	public String toString()
	{
		return "mk_token(\"" + token + "\")";
	}

	@Override
	public String toVDM(String indent)
	{
		return indent + toString();
	}

	public void setField(String qName, VDMValue value)
	{
		token = token + "; " + qName;
	}
	
	@Override
	public boolean hasAny()
	{
		return true;
	}
}
